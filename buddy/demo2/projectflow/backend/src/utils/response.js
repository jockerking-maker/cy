/**
 * Unified API response helpers.
 * Ensures all endpoints return a consistent { code, message, data } structure.
 */

/**
 * Send a success response.
 * @param {object} res - Express response object
 * @param {string} [message='success'] - Response message
 * @param {*} [data=null] - Response data
 * @param {number} [status=200] - HTTP status code
 */
function success(res, message = 'success', data = null, status = 200) {
  return res.status(status).json({ code: status, message, data });
}

/**
 * Send a created response (201).
 * @param {object} res - Express response object
 * @param {string} [message='创建成功'] - Response message
 * @param {*} [data=null] - Response data
 */
function created(res, message = '创建成功', data = null) {
  return res.status(201).json({ code: 201, message, data });
}

/**
 * Send an error response.
 * @param {object} res - Express response object
 * @param {string} [message='操作失败'] - Error message
 * @param {number} [status=400] - HTTP status code
 * @param {*} [data=null] - Optional error data
 */
function error(res, message = '操作失败', status = 400, data = null) {
  return res.status(status).json({ code: status, message, data });
}

/**
 * Send a paginated list response.
 * @param {object} res - Express response object
 * @param {Array} list - Data list
 * @param {object} pagination - { page, pageSize, total, totalPages }
 * @param {string} [message='success'] - Response message
 */
function paginated(res, list, pagination, message = 'success') {
  return res.json({ code: 200, message, data: { list, pagination } });
}

/**
 * Parse and validate pagination parameters from query.
 * @param {object} query - Express req.query
 * @returns {{page: number, pageSize: number, offset: number}}
 */
function parsePagination(query) {
  const page = Math.max(parseInt(query.page, 10) || 1, 1);
  const pageSize = Math.min(Math.max(parseInt(query.pageSize, 10) || 20, 1), 100);
  const offset = (page - 1) * pageSize;
  return { page, pageSize, offset };
}

/**
 * Calculate total pages from total count and page size.
 * @param {number} total - Total record count
 * @param {number} pageSize - Items per page
 * @returns {number}
 */
function calcTotalPages(total, pageSize) {
  return Math.ceil(total / pageSize);
}

module.exports = {
  success,
  created,
  error,
  paginated,
  parsePagination,
  calcTotalPages,
};
