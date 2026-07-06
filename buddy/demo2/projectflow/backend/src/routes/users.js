const express = require('express');
const bcrypt = require('bcryptjs');
const { authenticate, requireRole } = require('../middleware/auth');
const { logActivity } = require('../middleware/logger');
const { success, created, error, paginated, parsePagination, calcTotalPages } = require('../utils/response');
const { validateSortField, validateSortOrder, isValidEmail, isValidUsername, isValidPassword, isValidId } = require('../utils/validate');

const router = express.Router();

// All user management routes require admin role
router.use(authenticate, requireRole('admin'));

const ALLOWED_SORT_FIELDS = ['id', 'username', 'email', 'display_name', 'role_id', 'status', 'created_at', 'updated_at', 'last_login'];

/**
 * GET /api/v1/users
 * List users with pagination, search, and sort.
 */
router.get('/', (req, res) => {
  try {
    const db = req.app.locals.db;
    const { page, pageSize, offset } = parsePagination(req.query);
    const search = req.query.search || '';
    const sort = validateSortField(req.query.sort, ALLOWED_SORT_FIELDS);
    const sortOrder = validateSortOrder(req.query.order);

    let whereClause = '';
    const params = [];

    if (search) {
      whereClause = 'WHERE (u.username LIKE ? OR u.email LIKE ? OR u.display_name LIKE ?)';
      const like = `%${search}%`;
      params.push(like, like, like);
    }

    const countRow = db.prepare(`SELECT COUNT(*) as total FROM users u ${whereClause}`).get(...params);
    const total = countRow.total;
    const totalPages = calcTotalPages(total, pageSize);

    const list = db.prepare(`
      SELECT u.id, u.username, u.email, u.display_name, u.avatar, u.phone,
             u.role_id, r.name as role_name, u.status, u.last_login, u.created_at, u.updated_at
      FROM users u
      LEFT JOIN roles r ON u.role_id = r.id
      ${whereClause}
      ORDER BY u.${sort} ${sortOrder}
      LIMIT ? OFFSET ?
    `).all(...params, pageSize, offset);

    return paginated(res, list, { page, pageSize, total, totalPages });
  } catch (err) {
    console.error('List users error:', err);
    return error(res, '获取用户列表失败', 500);
  }
});

/**
 * GET /api/v1/users/:id
 */
router.get('/:id', (req, res) => {
  try {
    if (!isValidId(req.params.id)) {
      return error(res, '无效的用户 ID', 400);
    }

    const db = req.app.locals.db;
    const id = parseInt(req.params.id, 10);

    const user = db.prepare(`
      SELECT u.id, u.username, u.email, u.display_name, u.avatar, u.phone,
             u.role_id, r.name as role_name, u.status, u.last_login, u.created_at, u.updated_at
      FROM users u
      LEFT JOIN roles r ON u.role_id = r.id
      WHERE u.id = ?
    `).get(id);

    if (!user) {
      return res.status(404).json({ code: 404, message: '用户不存在', data: null });
    }

    return success(res, 'success', user);
  } catch (err) {
    console.error('Get user error:', err);
    return error(res, '获取用户信息失败', 500);
  }
});

/**
 * POST /api/v1/users
 * Create user (admin only).
 */
router.post('/', (req, res) => {
  try {
    const { username, email, password, display_name, phone, role_id, status } = req.body;

    if (!username || !email || !password) {
      return error(res, '用户名、邮箱和密码为必填项', 400);
    }
    if (!isValidUsername(username)) {
      return error(res, '用户名格式无效', 400);
    }
    if (!isValidEmail(email)) {
      return error(res, '邮箱格式无效', 400);
    }
    if (!isValidPassword(password)) {
      return error(res, '密码长度不能少于6位', 400);
    }

    const db = req.app.locals.db;

    const existing = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username.trim(), email.trim());
    if (existing) {
      return error(res, '用户名或邮箱已被注册', 400);
    }

    const hashedPassword = bcrypt.hashSync(password, 10);
    const result = db.prepare(`
      INSERT INTO users (username, email, password, display_name, phone, role_id, status)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(
      username.trim(),
      email.trim(),
      hashedPassword,
      display_name || username,
      phone || '',
      role_id || 2,
      status || 'active'
    );

    logActivity(req, 'create', 'user', result.lastInsertRowid, '创建用户 ' + username);

    return created(res, '创建成功', { id: result.lastInsertRowid, username, email });
  } catch (err) {
    console.error('Create user error:', err);
    return error(res, '创建用户失败', 500);
  }
});

/**
 * PUT /api/v1/users/:id
 * Update user (admin only).
 */
router.put('/:id', (req, res) => {
  try {
    if (!isValidId(req.params.id)) {
      return error(res, '无效的用户 ID', 400);
    }

    const id = parseInt(req.params.id, 10);
    const { username, email, password, display_name, phone, role_id, status } = req.body;
    const db = req.app.locals.db;

    const user = db.prepare('SELECT id FROM users WHERE id = ?').get(id);
    if (!user) {
      return res.status(404).json({ code: 404, message: '用户不存在', data: null });
    }

    // Validate email format if provided
    if (email && !isValidEmail(email)) {
      return error(res, '邮箱格式无效', 400);
    }

    // Check email uniqueness
    if (email) {
      const existing = db.prepare('SELECT id FROM users WHERE email = ? AND id != ?').get(email.trim(), id);
      if (existing) {
        return error(res, '邮箱已被其他用户使用', 400);
      }
    }

    // Check username uniqueness
    if (username) {
      const existing = db.prepare('SELECT id FROM users WHERE username = ? AND id != ?').get(username.trim(), id);
      if (existing) {
        return error(res, '用户名已被其他用户使用', 400);
      }
    }

    // Build update query dynamically based on provided fields
    const updates = [];
    const params = [];

    if (username !== undefined) { updates.push('username = ?'); params.push(username.trim()); }
    if (email !== undefined) { updates.push('email = ?'); params.push(email.trim()); }
    if (display_name !== undefined) { updates.push('display_name = ?'); params.push(display_name); }
    if (phone !== undefined) { updates.push('phone = ?'); params.push(phone); }
    if (role_id !== undefined) { updates.push('role_id = ?'); params.push(role_id); }
    if (status !== undefined) { updates.push('status = ?'); params.push(status); }
    if (password) {
      if (!isValidPassword(password)) {
        return error(res, '密码长度不能少于6位', 400);
      }
      updates.push('password = ?');
      params.push(bcrypt.hashSync(password, 10));
    }

    if (updates.length === 0) {
      return error(res, '没有可更新的字段', 400);
    }

    updates.push('updated_at = CURRENT_TIMESTAMP');
    params.push(id);

    db.prepare(`UPDATE users SET ${updates.join(', ')} WHERE id = ?`).run(...params);

    logActivity(req, 'update', 'user', id, '更新用户信息');

    const updated = db.prepare(`
      SELECT u.id, u.username, u.email, u.display_name, u.phone, u.role_id,
             r.name as role_name, u.status, u.updated_at
      FROM users u
      LEFT JOIN roles r ON u.role_id = r.id
      WHERE u.id = ?
    `).get(id);

    return success(res, '更新成功', updated);
  } catch (err) {
    console.error('Update user error:', err);
    return error(res, '更新用户失败', 500);
  }
});

/**
 * DELETE /api/v1/users/:id
 * Delete user (admin only). Prevents self-deletion.
 */
router.delete('/:id', (req, res) => {
  try {
    if (!isValidId(req.params.id)) {
      return error(res, '无效的用户 ID', 400);
    }

    const id = parseInt(req.params.id, 10);

    // Prevent self-deletion
    if (id === req.user.id) {
      return error(res, '不能删除当前登录的管理员账号', 400);
    }

    const db = req.app.locals.db;
    const user = db.prepare('SELECT id, username FROM users WHERE id = ?').get(id);
    if (!user) {
      return res.status(404).json({ code: 404, message: '用户不存在', data: null });
    }

    db.prepare('DELETE FROM users WHERE id = ?').run(id);

    logActivity(req, 'delete', 'user', id, '删除用户 ' + user.username);

    return success(res, '删除成功');
  } catch (err) {
    console.error('Delete user error:', err);
    return error(res, '删除用户失败', 500);
  }
});

/**
 * PUT /api/v1/users/:id/role
 * Change user role (admin only).
 */
router.put('/:id/role', (req, res) => {
  try {
    if (!isValidId(req.params.id)) {
      return error(res, '无效的用户 ID', 400);
    }

    const id = parseInt(req.params.id, 10);
    const { role_id } = req.body;

    if (!role_id || !isValidId(role_id)) {
      return error(res, '有效的角色ID为必填项', 400);
    }

    const db = req.app.locals.db;

    const user = db.prepare('SELECT id, username FROM users WHERE id = ?').get(id);
    if (!user) {
      return res.status(404).json({ code: 404, message: '用户不存在', data: null });
    }

    const role = db.prepare('SELECT id, name FROM roles WHERE id = ?').get(role_id);
    if (!role) {
      return error(res, '角色不存在', 400);
    }

    db.prepare('UPDATE users SET role_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(role_id, id);

    logActivity(req, 'update', 'user', id, '修改用户角色为 ' + role.name);

    return success(res, '角色修改成功', { role_name: role.name });
  } catch (err) {
    console.error('Change role error:', err);
    return error(res, '修改角色失败', 500);
  }
});

module.exports = router;
