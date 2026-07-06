const express = require('express');
const path = require('path');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const config = require('./config/env');
const { initDatabase } = require('./config/database');
const { errorHandler } = require('./middleware/auth');

// Initialize database
const db = initDatabase();

const app = express();

// Store db instance in app locals for routes
app.locals.db = db;

// --- Middleware stack ---
app.use(cors());
app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));

// Logging: 'combined' in production, 'dev' in development
app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Static file serving for uploads
app.use('/uploads', express.static(path.resolve(__dirname, '..', 'uploads'), {
  maxAge: '1d',
  etag: true,
}));

// --- API routes ---
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const itemRoutes = require('./routes/items');
const articleRoutes = require('./routes/articles');
const dashboardRoutes = require('./routes/dashboard');
const uploadRoutes = require('./routes/upload');
const settingsRoutes = require('./routes/settings');

app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/users', userRoutes);
app.use('/api/v1/items', itemRoutes);
app.use('/api/v1/articles', articleRoutes);
app.use('/api/v1/dashboard', dashboardRoutes);
app.use('/api/v1/upload', uploadRoutes);
app.use('/api/v1/settings', settingsRoutes);

// Health check endpoint
app.get('/api/health', (_req, res) => {
  res.json({
    code: 200,
    message: 'OK',
    data: {
      status: 'running',
      version: '1.0.0',
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
    },
  });
});

// 404 handler - must be before error handler
app.use((_req, res) => {
  res.status(404).json({ code: 404, message: '接口不存在', data: null });
});

// Global error handler - must be last middleware
app.use(errorHandler);

// --- Start server ---
const server = app.listen(config.PORT, () => {
  console.log('========================================');
  console.log('  ProjectFlow API Server');
  console.log('  Version: 1.0.0');
  console.log(`  Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`  Listening on: http://localhost:${config.PORT}`);
  console.log(`  API Base: http://localhost:${config.PORT}/api/v1`);
  console.log(`  Database: ${config.DB_PATH}`);
  console.log('========================================');
});

// --- Graceful shutdown ---
function gracefulShutdown(signal) {
  console.log(`\n[${signal}] Shutting down gracefully...`);

  server.close(() => {
    console.log('HTTP server closed.');

    try {
      db.close();
      console.log('Database connection closed.');
    } catch (err) {
      console.error('Error closing database:', err.message);
    }

    console.log('Shutdown complete.');
    process.exit(0);
  });

  // Force close after 10 seconds
  setTimeout(() => {
    console.error('Forced shutdown after timeout.');
    process.exit(1);
  }, 10000);
}

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

// Handle unhandled promise rejections
process.on('unhandledRejection', (err) => {
  console.error('Unhandled Promise Rejection:', err);
});

// Handle uncaught exceptions
process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err);
  gracefulShutdown('uncaughtException');
});

module.exports = app;
