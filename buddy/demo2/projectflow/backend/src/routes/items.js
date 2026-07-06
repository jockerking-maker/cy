const express = require('express');
const { authenticate } = require('../middleware/auth');
const { logActivity } = require('../middleware/logger');
const { success, created, error, paginated, parsePagination, calcTotalPages } = require('../utils/response');
const { validateSortField, validateSortOrder } = require('../utils/validate');

const router = express.Router();

router.use(authenticate);

// Whitelist of allowed sort fields to prevent SQL injection
const ALLOWED_SORT_FIELDS = ['created_at', 'updated_at', 'title', 'priority', 'due_date', 'status', 'category'];

/**
 * Build WHERE clause and params from query filters.
 * @param {object} query - req.query
 * @returns {{ whereClause: string, params: any[] }}
 */
function buildFilters(query) {
  const conditions = [];
  const params = [];

  if (query.search) {
    conditions.push('(i.title LIKE ? OR i.description LIKE ?)');
    const like = `%${query.search}%`;
    params.push(like, like);
  }
  if (query.category) {
    conditions.push('i.category = ?');
    params.push(query.category);
  }
  if (query.status) {
    conditions.push('i.status = ?');
    params.push(query.status);
  }
  if (query.priority) {
    conditions.push('i.priority = ?');
    params.push(query.priority);
  }

  const whereClause = conditions.length > 0 ? 'WHERE ' + conditions.join(' AND ') : '';
  return { whereClause, params };
}

/**
 * GET /api/v1/items
 * List items with pagination, search, filter, and sort.
 */
router.get('/', (req, res) => {
  try {
    const db = req.app.locals.db;
    const { page, pageSize, offset } = parsePagination(req.query);
    const { whereClause, params } = buildFilters(req.query);

    const sort = validateSortField(req.query.sort, ALLOWED_SORT_FIELDS);
    const sortOrder = validateSortOrder(req.query.order);

    const countRow = db.prepare(`SELECT COUNT(*) as total FROM items i ${whereClause}`).get(...params);
    const total = countRow.total;
    const totalPages = calcTotalPages(total, pageSize);

    const list = db.prepare(`
      SELECT i.*,
        creator.username as creator_name,
        assignee.username as assignee_name
      FROM items i
      LEFT JOIN users creator ON i.creator_id = creator.id
      LEFT JOIN users assignee ON i.assignee_id = assignee.id
      ${whereClause}
      ORDER BY i.${sort} ${sortOrder}
      LIMIT ? OFFSET ?
    `).all(...params, pageSize, offset);

    return paginated(res, list, { page, pageSize, total, totalPages });
  } catch (err) {
    console.error('List items error:', err);
    return error(res, '获取项目列表失败', 500);
  }
});

/**
 * GET /api/v1/items/:id
 */
router.get('/:id', (req, res) => {
  try {
    const db = req.app.locals.db;
    const id = parseInt(req.params.id, 10);

    if (isNaN(id) || id <= 0) {
      return error(res, '无效的 ID', 400);
    }

    const item = db.prepare(`
      SELECT i.*,
        creator.username as creator_name,
        assignee.username as assignee_name
      FROM items i
      LEFT JOIN users creator ON i.creator_id = creator.id
      LEFT JOIN users assignee ON i.assignee_id = assignee.id
      WHERE i.id = ?
    `).get(id);

    if (!item) {
      return res.status(404).json({ code: 404, message: '项目不存在', data: null });
    }

    return success(res, 'success', item);
  } catch (err) {
    console.error('Get item error:', err);
    return error(res, '获取项目信息失败', 500);
  }
});

/**
 * POST /api/v1/items
 */
router.post('/', (req, res) => {
  try {
    const { title, description, category, status, priority, assignee_id, due_date, tags } = req.body;

    if (!title || !title.trim()) {
      return error(res, '标题为必填项', 400);
    }

    const db = req.app.locals.db;

    const result = db.prepare(`
      INSERT INTO items (title, description, category, status, priority, creator_id, assignee_id, due_date, tags)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      title.trim(),
      description || '',
      category || '',
      status || 'active',
      priority || 'normal',
      req.user.id,
      assignee_id || null,
      due_date || null,
      tags || ''
    );

    logActivity(req, 'create', 'item', result.lastInsertRowid, '创建项目 ' + title);

    const item = db.prepare(`
      SELECT i.*, creator.username as creator_name, assignee.username as assignee_name
      FROM items i
      LEFT JOIN users creator ON i.creator_id = creator.id
      LEFT JOIN users assignee ON i.assignee_id = assignee.id
      WHERE i.id = ?
    `).get(result.lastInsertRowid);

    return created(res, '创建成功', item);
  } catch (err) {
    console.error('Create item error:', err);
    return error(res, '创建项目失败', 500);
  }
});

/**
 * PUT /api/v1/items/:id
 */
router.put('/:id', (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    if (isNaN(id) || id <= 0) {
      return error(res, '无效的 ID', 400);
    }

    const { title, description, category, status, priority, assignee_id, due_date, tags } = req.body;
    const db = req.app.locals.db;

    const existing = db.prepare('SELECT id, title FROM items WHERE id = ?').get(id);
    if (!existing) {
      return res.status(404).json({ code: 404, message: '项目不存在', data: null });
    }

    db.prepare(`
      UPDATE items SET
        title = COALESCE(?, title),
        description = COALESCE(?, description),
        category = COALESCE(?, category),
        status = COALESCE(?, status),
        priority = COALESCE(?, priority),
        assignee_id = COALESCE(?, assignee_id),
        due_date = COALESCE(?, due_date),
        tags = COALESCE(?, tags),
        updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).run(
      title !== undefined ? title : null,
      description !== undefined ? description : null,
      category !== undefined ? category : null,
      status !== undefined ? status : null,
      priority !== undefined ? priority : null,
      assignee_id !== undefined ? assignee_id : null,
      due_date !== undefined ? due_date : null,
      tags !== undefined ? tags : null,
      id
    );

    logActivity(req, 'update', 'item', id, '更新项目 ' + (title || existing.title));

    const item = db.prepare(`
      SELECT i.*, creator.username as creator_name, assignee.username as assignee_name
      FROM items i
      LEFT JOIN users creator ON i.creator_id = creator.id
      LEFT JOIN users assignee ON i.assignee_id = assignee.id
      WHERE i.id = ?
    `).get(id);

    return success(res, '更新成功', item);
  } catch (err) {
    console.error('Update item error:', err);
    return error(res, '更新项目失败', 500);
  }
});

/**
 * DELETE /api/v1/items/:id
 */
router.delete('/:id', (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    if (isNaN(id) || id <= 0) {
      return error(res, '无效的 ID', 400);
    }

    const db = req.app.locals.db;
    const item = db.prepare('SELECT id, title FROM items WHERE id = ?').get(id);
    if (!item) {
      return res.status(404).json({ code: 404, message: '项目不存在', data: null });
    }

    db.prepare('DELETE FROM items WHERE id = ?').run(id);

    logActivity(req, 'delete', 'item', id, '删除项目 ' + item.title);

    return success(res, '删除成功');
  } catch (err) {
    console.error('Delete item error:', err);
    return error(res, '删除项目失败', 500);
  }
});

/**
 * POST /api/v1/items/batch-delete
 */
router.post('/batch-delete', (req, res) => {
  try {
    const { ids } = req.body;

    if (!Array.isArray(ids) || ids.length === 0) {
      return error(res, '请提供要删除的ID数组', 400);
    }

    // Validate all IDs are positive integers
    const validIds = ids
      .map((id) => parseInt(id, 10))
      .filter((id) => !isNaN(id) && id > 0);
    if (validIds.length === 0) {
      return error(res, 'ID数组包含无效值', 400);
    }

    const db = req.app.locals.db;
    const placeholders = validIds.map(() => '?').join(',');
    const deleted = db.prepare(`DELETE FROM items WHERE id IN (${placeholders})`).run(...validIds);

    logActivity(req, 'delete', 'item', null, '批量删除 ' + deleted.changes + ' 个项目');

    return success(res, '批量删除成功', { deleted: deleted.changes });
  } catch (err) {
    console.error('Batch delete error:', err);
    return error(res, '批量删除失败', 500);
  }
});

module.exports = router;
