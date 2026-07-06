const jwt = require('jsonwebtoken');
const config = require('../config/env');

/**
 * JWT authentication middleware.
 * Extracts user from token and attaches to req.user.
 */
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      code: 401,
      message: '未提供认证令牌',
      data: null,
    });
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, config.JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({
      code: 401,
      message: '认证令牌无效或已过期',
      data: null,
    });
  }
}

/**
 * Role-based authorization middleware.
 * @param {string} role - Required role name (e.g. 'admin')
 */
function requireRole(role) {
  return function (req, res, next) {
    if (!req.user) {
      return res.status(401).json({
        code: 401,
        message: '未认证',
        data: null,
      });
    }

    if (req.user.role_name !== role) {
      return res.status(403).json({
        code: 403,
        message: '权限不足，需要 ' + role + ' 角色',
        data: null,
      });
    }

    next();
  };
}

/**
 * Global error handler middleware.
 */
function errorHandler(err, req, res, _next) {
  console.error('Unhandled error:', err);

  const statusCode = err.statusCode || 500;
  const message = err.message || '服务器内部错误';

  res.status(statusCode).json({
    code: statusCode,
    message: message,
    data: null,
  });
}

module.exports = { authenticate, requireRole, errorHandler };
