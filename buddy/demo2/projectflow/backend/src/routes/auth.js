const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const config = require('../config/env');
const { authenticate } = require('../middleware/auth');
const { logActivity } = require('../middleware/logger');
const { success, created, error } = require('../utils/response');
const { isValidEmail, isValidUsername, isValidPassword, sanitizeString } = require('../utils/validate');

const router = express.Router();

/**
 * POST /api/v1/auth/register
 * Register a new user.
 */
router.post('/register', (req, res) => {
  try {
    const { username, email, password, display_name } = req.body;

    // Validate required fields
    if (!username || !email || !password) {
      return error(res, '用户名、邮箱和密码为必填项', 400);
    }

    // Validate field formats
    if (!isValidUsername(username)) {
      return error(res, '用户名格式无效（2-30个字符，支持字母、数字、下划线和中文）', 400);
    }
    if (!isValidEmail(email)) {
      return error(res, '邮箱格式无效', 400);
    }
    if (!isValidPassword(password)) {
      return error(res, '密码长度不能少于6位', 400);
    }

    const db = req.app.locals.db;

    // Check existing user
    const existing = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username.trim(), email.trim());
    if (existing) {
      return error(res, '用户名或邮箱已被注册', 400);
    }

    const hashedPassword = bcrypt.hashSync(password, 10);
    const result = db.prepare(
      'INSERT INTO users (username, email, password, display_name, role_id) VALUES (?, ?, ?, ?, 2)'
    ).run(username.trim(), email.trim(), hashedPassword, sanitizeString(display_name || username, 50));

    const userId = result.lastInsertRowid;

    logActivity(req, 'create', 'user', userId, '注册新用户 ' + username);

    return created(res, '注册成功', { id: userId, username: username.trim(), email: email.trim(), display_name: display_name || username });
  } catch (err) {
    console.error('Register error:', err);
    return error(res, '注册失败', 500);
  }
});

/**
 * POST /api/v1/auth/login
 * Authenticate user and return JWT token.
 */
router.post('/login', (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return error(res, '用户名和密码为必填项', 400);
    }

    const db = req.app.locals.db;

    const user = db.prepare(`
      SELECT u.*, r.name as role_name
      FROM users u
      LEFT JOIN roles r ON u.role_id = r.id
      WHERE u.username = ? OR u.email = ?
    `).get(username.trim(), username.trim());

    if (!user) {
      return res.status(401).json({ code: 401, message: '用户名或密码错误', data: null });
    }

    if (user.status !== 'active') {
      return res.status(403).json({ code: 403, message: '账号已被禁用', data: null });
    }

    const valid = bcrypt.compareSync(password, user.password);
    if (!valid) {
      return res.status(401).json({ code: 401, message: '用户名或密码错误', data: null });
    }

    // Update last_login
    db.prepare('UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?').run(user.id);

    logActivity(req, 'login', 'auth', user.id, '用户登录系统');

    const tokenPayload = {
      id: user.id,
      username: user.username,
      role_id: user.role_id,
      role_name: user.role_name,
    };

    const token = jwt.sign(tokenPayload, config.JWT_SECRET, {
      expiresIn: config.JWT_EXPIRES_IN,
    });

    return success(res, '登录成功', {
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        display_name: user.display_name,
        avatar: user.avatar,
        phone: user.phone,
        role_id: user.role_id,
        role_name: user.role_name,
        status: user.status,
      },
    });
  } catch (err) {
    console.error('Login error:', err);
    return error(res, '登录失败', 500);
  }
});

/**
 * GET /api/v1/auth/profile
 * Get current user profile.
 */
router.get('/profile', authenticate, (req, res) => {
  try {
    const db = req.app.locals.db;
    const user = db.prepare(`
      SELECT u.id, u.username, u.email, u.display_name, u.avatar, u.phone,
             u.role_id, r.name as role_name, u.status, u.last_login, u.created_at, u.updated_at
      FROM users u
      LEFT JOIN roles r ON u.role_id = r.id
      WHERE u.id = ?
    `).get(req.user.id);

    if (!user) {
      return res.status(404).json({ code: 404, message: '用户不存在', data: null });
    }

    return success(res, 'success', user);
  } catch (err) {
    console.error('Get profile error:', err);
    return error(res, '获取用户信息失败', 500);
  }
});

/**
 * PUT /api/v1/auth/profile
 * Update current user profile (display_name, email, phone, avatar).
 */
router.put('/profile', authenticate, (req, res) => {
  try {
    const { display_name, email, phone, avatar } = req.body;
    const db = req.app.locals.db;

    // Validate email format if provided
    if (email && !isValidEmail(email)) {
      return error(res, '邮箱格式无效', 400);
    }

    // Check email uniqueness if changing
    if (email) {
      const existing = db.prepare('SELECT id FROM users WHERE email = ? AND id != ?').get(email.trim(), req.user.id);
      if (existing) {
        return error(res, '邮箱已被其他用户使用', 400);
      }
    }

    db.prepare(`
      UPDATE users SET
        display_name = COALESCE(?, display_name),
        email = COALESCE(?, email),
        phone = COALESCE(?, phone),
        avatar = COALESCE(?, avatar),
        updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).run(
      display_name !== undefined ? sanitizeString(display_name, 50) : null,
      email ? email.trim() : null,
      phone !== undefined ? sanitizeString(phone, 20) : null,
      avatar !== undefined ? sanitizeString(avatar, 500) : null,
      req.user.id
    );

    logActivity(req, 'update', 'user', req.user.id, '更新个人资料');

    const updated = db.prepare('SELECT id, username, email, display_name, avatar, phone FROM users WHERE id = ?').get(req.user.id);

    return success(res, '更新成功', updated);
  } catch (err) {
    console.error('Update profile error:', err);
    return error(res, '更新资料失败', 500);
  }
});

/**
 * PUT /api/v1/auth/password
 * Change password (require old_password + new_password).
 */
router.put('/password', authenticate, (req, res) => {
  try {
    const { old_password, new_password } = req.body;

    if (!old_password || !new_password) {
      return error(res, '旧密码和新密码为必填项', 400);
    }

    if (!isValidPassword(new_password)) {
      return error(res, '新密码长度不能少于6位', 400);
    }

    const db = req.app.locals.db;
    const user = db.prepare('SELECT password FROM users WHERE id = ?').get(req.user.id);

    const valid = bcrypt.compareSync(old_password, user.password);
    if (!valid) {
      return error(res, '旧密码不正确', 400);
    }

    const hashedPassword = bcrypt.hashSync(new_password, 10);
    db.prepare('UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?').run(hashedPassword, req.user.id);

    logActivity(req, 'update', 'user', req.user.id, '修改密码');

    return success(res, '密码修改成功');
  } catch (err) {
    console.error('Change password error:', err);
    return error(res, '修改密码失败', 500);
  }
});

module.exports = router;
