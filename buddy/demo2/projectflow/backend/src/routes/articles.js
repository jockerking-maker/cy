const express = require('express');
const { authenticate } = require('../middleware/auth');
const { logActivity } = require('../middleware/logger');
const { success, created, error, paginated, parsePagination, calcTotalPages } = require('../utils/response');
const { validateSortField, validateSortOrder, isValidId, sanitizeString } = require('../utils/validate');

const router = express.Router();

router.use(authenticate);

const ALLOWED_SORT_FIELDS = ['created_at', 'updated_at', 'title', 'view_count', 'published_at', 'status'];

/**
 * Build WHERE clause from query filters.
 */
function buildFilters(query) {
  const conditions = [];
  const params = [];

  if (query.search) {
    conditions.push('(a.title LIKE ?)');
    params.push(`%${query.search}%`);
  }
  if (query.category) {
    conditions.push('a.category = ?');
    params.push(query.category);
  }
  if (query.status) {
    conditions.push('a.status = ?');
    params.push(query.status);
  }

  const whereClause = conditions.length > 0 ? 'WHERE ' + conditions.join(' AND ') : '';
  return { whereClause, params };
}

/**
 * GET /api/v1/articles
 */
router.get('/', (req, res) => {
  try {
    const db = req.app.locals.db;
    const { page, pageSize, offset } = parsePagination(req.query);
    const { whereClause, params } = buildFilters(req.query);
    const sort = validateSortField(req.query.sort, ALLOWED_SORT_FIELDS);
    const sortOrder = validateSortOrder(req.query.order);

    const countRow = db.prepare(`SELECT COUNT(*) as total FROM articles a ${whereClause}`).get(...params);
    const total = countRow.total;
    const totalPages = calcTotalPages(total, pageSize);

    const list = db.prepare(`
      SELECT a.id, a.title, a.summary, a.cover_image, a.category, a.tags,
             a.status, a.author_id, u.username as author_name, a.view_count,
             a.published_at, a.created_at, a.updated_at
      FROM articles a
      LEFT JOIN users u ON a.author_id = u.id
      ${whereClause}
      ORDER BY a.${sort} ${sortOrder}
      LIMIT ? OFFSET ?
    `).all(...params, pageSize, offset);

    return paginated(res, list, { page, pageSize, total, totalPages });
  } catch (err) {
    console.error('List articles error:', err);
    return error(res, '获取文章列表失败', 500);
  }
});

/**
 * GET /api/v1/articles/:id
 * Increments view_count on each fetch.
 */
router.get('/:id', (req, res) => {
  try {
    if (!isValidId(req.params.id)) {
      return error(res, '无效的文章 ID', 400);
    }

    const db = req.app.locals.db;
    const id = parseInt(req.params.id, 10);

    const article = db.prepare(`
      SELECT a.*, u.username as author_name
      FROM articles a
      LEFT JOIN users u ON a.author_id = u.id
      WHERE a.id = ?
    `).get(id);

    if (!article) {
      return res.status(404).json({ code: 404, message: '文章不存在', data: null });
    }

    // Increment view_count
    db.prepare('UPDATE articles SET view_count = view_count + 1 WHERE id = ?').run(id);
    article.view_count += 1;

    return success(res, 'success', article);
  } catch (err) {
    console.error('Get article error:', err);
    return error(res, '获取文章信息失败', 500);
  }
});

/**
 * POST /api/v1/articles
 */
router.post('/', (req, res) => {
  try {
    const { title, content, summary, cover_image, category, tags, status } = req.body;

    if (!title || !title.trim()) {
      return error(res, '标题为必填项', 400);
    }

    const db = req.app.locals.db;
    const articleStatus = status || 'draft';
    const publishedAt = articleStatus === 'published' ? new Date().toISOString() : null;

    const result = db.prepare(`
      INSERT INTO articles (title, content, summary, cover_image, category, tags, status, author_id, published_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      sanitizeString(title, 200),
      content || '',
      sanitizeString(summary, 500),
      cover_image || '',
      category || '',
      tags || '',
      articleStatus,
      req.user.id,
      publishedAt
    );

    logActivity(req, 'create', 'article', result.lastInsertRowid, '创建文章 ' + title);

    const article = db.prepare(`
      SELECT a.*, u.username as author_name
      FROM articles a
      LEFT JOIN users u ON a.author_id = u.id
      WHERE a.id = ?
    `).get(result.lastInsertRowid);

    return created(res, '创建成功', article);
  } catch (err) {
    console.error('Create article error:', err);
    return error(res, '创建文章失败', 500);
  }
});

/**
 * PUT /api/v1/articles/:id
 */
router.put('/:id', (req, res) => {
  try {
    if (!isValidId(req.params.id)) {
      return error(res, '无效的文章 ID', 400);
    }

    const id = parseInt(req.params.id, 10);
    const { title, content, summary, cover_image, category, tags, status } = req.body;
    const db = req.app.locals.db;

    const existing = db.prepare('SELECT id, title, status FROM articles WHERE id = ?').get(id);
    if (!existing) {
      return res.status(404).json({ code: 404, message: '文章不存在', data: null });
    }

    // If publishing for the first time, set published_at
    let publishedAt = null;
    if (status === 'published' && existing.status !== 'published') {
      publishedAt = new Date().toISOString();
    }

    db.prepare(`
      UPDATE articles SET
        title = COALESCE(?, title),
        content = COALESCE(?, content),
        summary = COALESCE(?, summary),
        cover_image = COALESCE(?, cover_image),
        category = COALESCE(?, category),
        tags = COALESCE(?, tags),
        status = COALESCE(?, status),
        published_at = COALESCE(?, published_at),
        updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).run(
      title !== undefined ? sanitizeString(title, 200) : null,
      content !== undefined ? content : null,
      summary !== undefined ? sanitizeString(summary, 500) : null,
      cover_image !== undefined ? cover_image : null,
      category !== undefined ? category : null,
      tags !== undefined ? tags : null,
      status !== undefined ? status : null,
      publishedAt,
      id
    );

    logActivity(req, 'update', 'article', id, '更新文章 ' + (title || existing.title));

    const article = db.prepare(`
      SELECT a.*, u.username as author_name
      FROM articles a
      LEFT JOIN users u ON a.author_id = u.id
      WHERE a.id = ?
    `).get(id);

    return success(res, '更新成功', article);
  } catch (err) {
    console.error('Update article error:', err);
    return error(res, '更新文章失败', 500);
  }
});

/**
 * DELETE /api/v1/articles/:id
 */
router.delete('/:id', (req, res) => {
  try {
    if (!isValidId(req.params.id)) {
      return error(res, '无效的文章 ID', 400);
    }

    const id = parseInt(req.params.id, 10);
    const db = req.app.locals.db;

    const article = db.prepare('SELECT id, title FROM articles WHERE id = ?').get(id);
    if (!article) {
      return res.status(404).json({ code: 404, message: '文章不存在', data: null });
    }

    db.prepare('DELETE FROM articles WHERE id = ?').run(id);

    logActivity(req, 'delete', 'article', id, '删除文章 ' + article.title);

    return success(res, '删除成功');
  } catch (err) {
    console.error('Delete article error:', err);
    return error(res, '删除文章失败', 500);
  }
});

module.exports = router;
