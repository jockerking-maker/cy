/**
 * Activity logging middleware.
 * Records user actions across the application.
 *
 * Usage in route files:
 *   const { logActivity } = require('../middleware/logger');
 *   logActivity(req, 'create', 'item', itemId, 'Created new item');
 */
function logActivity(req, action, resourceType, resourceId, description) {
  const db = req.app.locals.db;
  if (!db) return;

  const userId = req.user ? req.user.id : null;
  const ipAddress = req.ip || req.connection?.remoteAddress || '';

  try {
    db.prepare(`
      INSERT INTO activities (user_id, action, resource_type, resource_id, description, ip_address)
      VALUES (?, ?, ?, ?, ?, ?)
    `).run(userId, action, resourceType, resourceId, description, ipAddress);
  } catch (err) {
    console.error('Failed to log activity:', err.message);
  }
}

module.exports = { logActivity };
