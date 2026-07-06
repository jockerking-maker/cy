const express = require('express');
const { authenticate, requireRole } = require('../middleware/auth');
const { logActivity } = require('../middleware/logger');
const { success, error, paginated, parsePagination, calcTotalPages } = require('../utils/response');

const router = express.Router();

router.use(authenticate, requireRole('admin'));

// Keys that are allowed to be stored in each settings table
const SITE_SETTING_KEYS = ['site_name', 'site_description', 'site_logo', 'site_footer', 'maintenance_mode'];
const MAIL_SETTING_KEYS = ['mail_host', 'mail_port', 'mail_user', 'mail_pass', 'mail_from', 'mail_encryption'];

/**
 * Convert rows to a key-value object.
 * @param {Array} rows - Array of { setting_key, setting_value }
 * @param {boolean} [maskPassword=false]
 * @returns {object}
 */
function rowsToObject(rows, maskPassword = false) {
  const obj = {};
  for (const row of rows) {
    if (maskPassword && row.setting_key === 'mail_pass' && row.setting_value) {
      obj[row.setting_key] = '******';
    } else {
      obj[row.setting_key] = row.setting_value;
    }
  }
  return obj;
}

/**
 * GET /api/v1/settings/site
 */
router.get('/site', (req, res) => {
  try {
    const db = req.app.locals.db;
    const rows = db.prepare('SELECT setting_key, setting_value FROM site_settings').all();
    return success(res, 'success', rowsToObject(rows));
  } catch (err) {
    console.error('Get site settings error:', err);
    return error(res, '获取站点设置失败', 500);
  }
});

/**
 * PUT /api/v1/settings/site
 */
router.put('/site', (req, res) => {
  try {
    const settings = req.body;
    const db = req.app.locals.db;

    // Filter to only allowed keys
    const filtered = {};
    for (const key of SITE_SETTING_KEYS) {
      if (settings[key] !== undefined) {
        filtered[key] = String(settings[key]);
      }
    }

    if (Object.keys(filtered).length === 0) {
      return error(res, '没有可更新的设置项', 400);
    }

    const upsert = db.prepare(`
      INSERT INTO site_settings (setting_key, setting_value, updated_at)
      VALUES (?, ?, CURRENT_TIMESTAMP)
      ON CONFLICT(setting_key) DO UPDATE SET
        setting_value = excluded.setting_value,
        updated_at = CURRENT_TIMESTAMP
    `);

    const transaction = db.transaction((entries) => {
      for (const [key, value] of Object.entries(entries)) {
        upsert.run(key, value);
      }
    });

    transaction(filtered);

    logActivity(req, 'update', 'settings', null, '更新站点设置');

    const rows = db.prepare('SELECT setting_key, setting_value FROM site_settings').all();
    return success(res, '更新成功', rowsToObject(rows));
  } catch (err) {
    console.error('Update site settings error:', err);
    return error(res, '更新站点设置失败', 500);
  }
});

/**
 * GET /api/v1/settings/mail
 * Returns mail settings with password masked.
 */
router.get('/mail', (req, res) => {
  try {
    const db = req.app.locals.db;
    const rows = db.prepare('SELECT setting_key, setting_value FROM mail_settings').all();
    return success(res, 'success', rowsToObject(rows, true));
  } catch (err) {
    console.error('Get mail settings error:', err);
    return error(res, '获取邮件设置失败', 500);
  }
});

/**
 * PUT /api/v1/settings/mail
 * Updates mail settings. Skips password if it's the masked placeholder '******'.
 */
router.put('/mail', (req, res) => {
  try {
    const settings = req.body;
    const db = req.app.locals.db;

    // Filter to allowed keys, skip masked password
    const filtered = {};
    for (const key of MAIL_SETTING_KEYS) {
      if (settings[key] !== undefined) {
        // Don't save the masked password placeholder
        if (key === 'mail_pass' && settings[key] === '******') {
          continue;
        }
        filtered[key] = String(settings[key]);
      }
    }

    if (Object.keys(filtered).length === 0) {
      return error(res, '没有可更新的设置项', 400);
    }

    const upsert = db.prepare(`
      INSERT INTO mail_settings (setting_key, setting_value, updated_at)
      VALUES (?, ?, CURRENT_TIMESTAMP)
      ON CONFLICT(setting_key) DO UPDATE SET
        setting_value = excluded.setting_value,
        updated_at = CURRENT_TIMESTAMP
    `);

    const transaction = db.transaction((entries) => {
      for (const [key, value] of Object.entries(entries)) {
        upsert.run(key, value);
      }
    });

    transaction(filtered);

    logActivity(req, 'update', 'settings', null, '更新邮件设置');

    const rows = db.prepare('SELECT setting_key, setting_value FROM mail_settings').all();
    return success(res, '更新成功', rowsToObject(rows, true));
  } catch (err) {
    console.error('Update mail settings error:', err);
    return error(res, '更新邮件设置失败', 500);
  }
});

/**
 * POST /api/v1/settings/mail/test
 */
router.post('/mail/test', (req, res) => {
  try {
    const db = req.app.locals.db;
    const rows = db.prepare('SELECT setting_key, setting_value FROM mail_settings').all();
    const config = rowsToObject(rows);

    console.log('[Mail Test] Would send test email with config:', {
      host: config.mail_host,
      port: config.mail_port,
      user: config.mail_user,
      from: config.mail_from,
      encryption: config.mail_encryption,
    });

    logActivity(req, 'test', 'mail', null, '测试邮件发送');

    return success(res, '测试邮件已发送（模拟）', { to: config.mail_from || 'unknown' });
  } catch (err) {
    console.error('Test mail error:', err);
    return error(res, '测试邮件发送失败', 500);
  }
});

/**
 * POST /api/v1/settings/cache/clear
 */
router.post('/cache/clear', (req, res) => {
  try {
    console.log('[Cache Clear] Cache cleared (mock)');
    logActivity(req, 'clear', 'cache', null, '清除系统缓存');
    return success(res, '缓存已清除（模拟）');
  } catch (err) {
    console.error('Clear cache error:', err);
    return error(res, '清除缓存失败', 500);
  }
});

/**
 * GET /api/v1/settings/logs
 * Paginated activity logs.
 */
router.get('/logs', (req, res) => {
  try {
    const db = req.app.locals.db;
    const { page, pageSize, offset } = parsePagination(req.query);

    const total = db.prepare('SELECT COUNT(*) as total FROM activities').get().total;
    const totalPages = calcTotalPages(total, pageSize);

    const list = db.prepare(`
      SELECT a.*, u.username, u.display_name
      FROM activities a
      LEFT JOIN users u ON a.user_id = u.id
      ORDER BY a.created_at DESC
      LIMIT ? OFFSET ?
    `).all(pageSize, offset);

    return paginated(res, list, { page, pageSize, total, totalPages });
  } catch (err) {
    console.error('Get logs error:', err);
    return error(res, '获取日志失败', 500);
  }
});

module.exports = router;
