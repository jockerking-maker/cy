const express = require('express');
const path = require('path');
const fs = require('fs');
const config = require('../config/env');
const { authenticate } = require('../middleware/auth');
const { success, error } = require('../utils/response');

const router = express.Router();

router.use(authenticate);

/**
 * Recursively calculate total size of a directory.
 * @param {string} dir - Directory path
 * @returns {number} Total size in bytes
 */
function getDirSize(dir) {
  let total = 0;
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        total += getDirSize(fullPath);
      } else {
        total += fs.statSync(fullPath).size;
      }
    }
  } catch (_) {
    // ignore unreadable directories
  }
  return total;
}

/**
 * Format bytes into human-readable string.
 * @param {number} bytes
 * @returns {string}
 */
function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Format uptime seconds into human-readable string.
 * @param {number} seconds
 * @returns {string}
 */
function formatUptime(seconds) {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const parts = [];
  if (days > 0) parts.push(days + '天');
  if (hours > 0) parts.push(hours + '小时');
  parts.push(mins + '分钟');
  return parts.join(' ');
}

/**
 * GET /api/v1/dashboard/stats
 * Returns aggregated statistics for the dashboard.
 */
router.get('/stats', (req, res) => {
  try {
    const db = req.app.locals.db;

    const totalUsers = db.prepare('SELECT COUNT(*) as count FROM users').get().count;
    const totalItems = db.prepare('SELECT COUNT(*) as count FROM items').get().count;
    const totalArticles = db.prepare('SELECT COUNT(*) as count FROM articles').get().count;
    const totalViews = db.prepare('SELECT COALESCE(SUM(view_count), 0) as count FROM articles').get().count;

    const itemsByCategory = db.prepare(`
      SELECT category, COUNT(*) as count FROM items GROUP BY category ORDER BY count DESC
    `).all();

    const usersByRole = db.prepare(`
      SELECT r.name as role, COUNT(*) as count
      FROM users u
      LEFT JOIN roles r ON u.role_id = r.id
      GROUP BY u.role_id
    `).all();

    return success(res, 'success', {
      total_users: totalUsers,
      total_items: totalItems,
      total_articles: totalArticles,
      total_views: totalViews,
      items_by_category: itemsByCategory,
      users_by_role: usersByRole,
    });
  } catch (err) {
    console.error('Stats error:', err);
    return error(res, '获取统计数据失败', 500);
  }
});

/**
 * GET /api/v1/dashboard/recent-activities
 * Returns the 10 most recent activities with user info.
 */
router.get('/recent-activities', (req, res) => {
  try {
    const db = req.app.locals.db;

    const activities = db.prepare(`
      SELECT a.id, a.user_id, a.action, a.resource_type, a.resource_id,
             a.description, a.ip_address, a.created_at,
             u.username, u.display_name, u.avatar
      FROM activities a
      LEFT JOIN users u ON a.user_id = u.id
      ORDER BY a.created_at DESC
      LIMIT 10
    `).all();

    return success(res, 'success', activities);
  } catch (err) {
    console.error('Recent activities error:', err);
    return error(res, '获取最近活动失败', 500);
  }
});

/**
 * GET /api/v1/dashboard/overview
 * Returns system overview information.
 */
router.get('/overview', (req, res) => {
  try {
    const dbPath = path.resolve(config.DB_PATH);
    let dbSize = 0;
    try {
      dbSize = fs.statSync(dbPath).size;
    } catch (_) {
      // ignore
    }

    const uploadDir = path.resolve(__dirname, '..', '..', 'uploads');
    const storageUsed = fs.existsSync(uploadDir) ? getDirSize(uploadDir) : 0;

    const db = req.app.locals.db;
    const itemStatusCounts = db.prepare(`
      SELECT status, COUNT(*) as count FROM items GROUP BY status
    `).all();
    const sqliteVersion = db.prepare('SELECT sqlite_version() as ver').get().ver;

    return success(res, 'success', {
      db_size: dbSize,
      db_size_formatted: formatBytes(dbSize),
      storage_used: storageUsed,
      storage_used_formatted: formatBytes(storageUsed),
      version: '1.0.0',
      environment: process.env.NODE_ENV || 'development',
      node_version: process.version,
      sqlite_version: sqliteVersion,
      uptime: process.uptime(),
      uptime_formatted: formatUptime(process.uptime()),
      item_status_counts: itemStatusCounts,
    });
  } catch (err) {
    console.error('Overview error:', err);
    return error(res, '获取系统概览失败', 500);
  }
});

module.exports = router;
