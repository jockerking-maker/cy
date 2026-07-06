/**
 * Input validation utilities.
 * Provides reusable validators for common field types.
 */

/**
 * Validate email format.
 * @param {string} email
 * @returns {boolean}
 */
function isValidEmail(email) {
  if (!email || typeof email !== 'string') return false;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

/**
 * Validate username (3-30 chars, alphanumeric + underscore).
 * @param {string} username
 * @returns {boolean}
 */
function isValidUsername(username) {
  if (!username || typeof username !== 'string') return false;
  return /^[a-zA-Z0-9_\u4e00-\u9fa5]{2,30}$/.test(username.trim());
}

/**
 * Validate password strength (min 6 chars).
 * @param {string} password
 * @returns {boolean}
 */
function isValidPassword(password) {
  if (!password || typeof password !== 'string') return false;
  return password.length >= 6;
}

/**
 * Validate a positive integer ID.
 * @param {*} id
 * @returns {boolean}
 */
function isValidId(id) {
  const num = parseInt(id, 10);
  return !isNaN(num) && num > 0;
}

/**
 * Sanitize a string by trimming and limiting length.
 * @param {string} str
 * @param {number} [maxLen=1000]
 * @returns {string}
 */
function sanitizeString(str, maxLen = 1000) {
  if (!str || typeof str !== 'string') return '';
  return str.trim().slice(0, maxLen);
}

/**
 * Validate sort field against a whitelist.
 * @param {string} field - Requested sort field
 * @param {string[]} allowed - Allowed sort fields
 * @param {string} [defaultField='created_at'] - Fallback field
 * @returns {string}
 */
function validateSortField(field, allowed, defaultField = 'created_at') {
  return allowed.includes(field) ? field : defaultField;
}

/**
 * Validate and normalize sort order.
 * @param {string} order - 'asc' or 'desc'
 * @returns {string} 'ASC' or 'DESC'
 */
function validateSortOrder(order) {
  return order === 'asc' ? 'ASC' : 'DESC';
}

module.exports = {
  isValidEmail,
  isValidUsername,
  isValidPassword,
  isValidId,
  sanitizeString,
  validateSortField,
  validateSortOrder,
};
