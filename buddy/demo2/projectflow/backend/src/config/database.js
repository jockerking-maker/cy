const path = require('path');
const fs = require('fs');
const bcrypt = require('bcryptjs');
const Database = require('better-sqlite3');
const config = require('./env');

/**
 * Initialize database: create connection, run schema, fix admin password, seed demo data.
 */
function initDatabase() {
  // Ensure data directory exists
  const dbDir = path.dirname(path.resolve(config.DB_PATH));
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  const db = new Database(path.resolve(config.DB_PATH));

  // Enable WAL mode for better performance
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  // Execute schema
  const schemaPath = path.resolve(__dirname, 'schema.sql');
  const schema = fs.readFileSync(schemaPath, 'utf-8');
  db.exec(schema);

  // Fix admin password with proper bcrypt hash
  const hashedPassword = bcrypt.hashSync('admin123', 10);
  db.prepare('UPDATE users SET password = ? WHERE id = 1').run(hashedPassword);

  // Seed demo data
  seedDemoData(db);

  return db;
}

function seedDemoData(db) {
  // --- Seed 5 users (skip if already exists) ---
  const users = [
    { username: 'alice', email: 'alice@projectflow.com', display_name: 'Alice Wang', role_id: 2 },
    { username: 'bob', email: 'bob@projectflow.com', display_name: 'Bob Li', role_id: 2 },
    { username: 'charlie', email: 'charlie@projectflow.com', display_name: 'Charlie Zhang', role_id: 2 },
    { username: 'diana', email: 'diana@projectflow.com', display_name: 'Diana Chen', role_id: 2 },
    { username: 'editor', email: 'editor@projectflow.com', display_name: 'Editor Liu', role_id: 2 },
  ];

  const insertUser = db.prepare(`
    INSERT OR IGNORE INTO users (username, email, password, display_name, role_id)
    VALUES (?, ?, ?, ?, ?)
  `);

  const defaultPw = bcrypt.hashSync('password123', 10);
  for (const u of users) {
    insertUser.run(u.username, u.email, defaultPw, u.display_name, u.role_id);
  }

  // --- Seed 20 items ---
  const categories = ['development', 'design', 'marketing', 'research', 'operation'];
  const statuses = ['active', 'pending', 'completed', 'archived'];
  const priorities = ['low', 'normal', 'high', 'urgent'];

  const count = db.prepare('SELECT COUNT(*) as cnt FROM items').get();
  if (count.cnt === 0) {
    const insertItem = db.prepare(`
      INSERT INTO items (title, description, category, status, priority, creator_id, assignee_id, due_date, tags)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    const itemData = [
      ['Setup CI/CD Pipeline', 'Configure GitHub Actions for automated builds and deployment', 'development', 'active', 'high', 1, 2, '2026-07-15', 'devops,ci/cd'],
      ['Design Landing Page', 'Create mockups for the new landing page redesign', 'design', 'active', 'normal', 2, 1, '2026-07-10', 'design,ui'],
      ['Q2 Marketing Campaign', 'Plan and execute Q2 digital marketing strategy', 'marketing', 'pending', 'high', 3, 4, '2026-07-20', 'marketing'],
      ['User Research Report', 'Compile findings from recent user interviews', 'research', 'completed', 'normal', 4, 3, '2026-06-30', 'research'],
      ['Server Optimization', 'Optimize database queries and server response times', 'development', 'active', 'urgent', 1, 2, '2026-07-08', 'backend,performance'],
      ['Mobile App Prototype', 'Build interactive prototype for mobile app', 'design', 'active', 'normal', 2, 1, '2026-07-25', 'mobile,prototype'],
      ['Email Newsletter Setup', 'Set up automated email newsletter system', 'marketing', 'pending', 'low', 3, 4, '2026-07-30', 'email,marketing'],
      ['Competitor Analysis', 'Analyze top 10 competitors in the market', 'research', 'active', 'normal', 4, 1, '2026-07-12', 'research,competitor'],
      ['API Documentation', 'Write comprehensive API documentation', 'operation', 'pending', 'normal', 1, 1, '2026-07-18', 'docs,api'],
      ['Database Migration', 'Migrate database from MySQL to PostgreSQL', 'development', 'pending', 'high', 1, 2, '2026-08-01', 'database,migration'],
      ['Brand Guidelines', 'Create brand style guide and design system', 'design', 'completed', 'normal', 2, 3, '2026-06-25', 'brand,design'],
      ['Social Media Calendar', 'Plan social media content for next month', 'marketing', 'active', 'normal', 3, 4, '2026-07-14', 'social,content'],
      ['Performance Benchmarking', 'Benchmark system performance under various loads', 'research', 'active', 'low', 4, 2, '2026-07-22', 'benchmark'],
      ['Employee Training', 'Prepare training materials for new team members', 'operation', 'active', 'normal', 1, 3, '2026-07-28', 'training'],
      ['Security Audit', 'Conduct comprehensive security audit', 'development', 'pending', 'urgent', 1, 2, '2026-07-05', 'security'],
      ['Dashboard Redesign', 'Redesign analytics dashboard UI', 'design', 'active', 'high', 2, 1, '2026-07-16', 'dashboard,ui'],
      ['SEO Optimization', 'Improve search engine rankings for main site', 'marketing', 'active', 'normal', 3, 2, '2026-07-20', 'seo'],
      ['Market Size Analysis', 'Calculate total addressable market size', 'research', 'completed', 'low', 4, 1, '2026-06-20', 'market,analysis'],
      ['Office Relocation', 'Coordinate office move to new location', 'operation', 'pending', 'normal', 1, 3, '2026-08-15', 'office'],
      ['Feature: Dark Mode', 'Implement dark mode support across the app', 'development', 'active', 'normal', 2, 2, '2026-07-19', 'frontend,darkmode'],
    ];

    for (const item of itemData) {
      insertItem.run(...item);
    }
  }

  // --- Seed 5 articles ---
  const articleCount = db.prepare('SELECT COUNT(*) as cnt FROM articles').get();
  if (articleCount.cnt === 0) {
    const insertArticle = db.prepare(`
      INSERT INTO articles (title, content, summary, cover_image, category, tags, status, author_id, view_count, published_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    const articles = [
      ['Getting Started with ProjectFlow', '<h2>Welcome to ProjectFlow</h2><p>ProjectFlow is a modern project management tool designed to help teams collaborate effectively.</p><p>This guide will walk you through the basic features.</p>', 'A comprehensive guide to getting started with ProjectFlow project management tool.', '', 'guide', 'getting-started,guide', 'published', 1, 128, '2026-06-01'],
      ['Best Practices for Agile Development', '<h2>Agile Development Best Practices</h2><p>Learn how to implement agile methodologies effectively in your team.</p><p>Key practices include daily standups, sprint planning, and retrospectives.</p>', 'Learn how to implement agile methodologies effectively in your team.', '', 'development', 'agile,development', 'published', 1, 256, '2026-06-05'],
      ['Design System Overview', '<h2>Our Design System</h2><p>A comprehensive look at our design system components and guidelines.</p><p>Consistent design helps create better user experiences.</p>', 'A comprehensive look at our design system components and guidelines.', '', 'design', 'design,ui', 'published', 2, 89, '2026-06-10'],
      ['Q3 Roadmap Announcement', '<h2>Q3 2026 Roadmap</h2><p>Exciting features coming in Q3! We are working on AI-powered task suggestions, advanced analytics, and team collaboration improvements.</p>', 'Exciting features coming in Q3! See what we have planned.', '', 'announcement', 'roadmap,announcement', 'published', 1, 312, '2026-06-15'],
      ['Tips for Remote Teams', '<h2>Remote Work Tips</h2><p>Working remotely comes with unique challenges. Here are our top tips for staying productive and connected.</p><p>Communication is key for remote teams.</p>', 'Tips for staying productive and connected while working remotely.', '', 'guide', 'remote,productivity', 'draft', 2, 45, null],
    ];

    for (const article of articles) {
      insertArticle.run(...article);
    }
  }

  // --- Seed 20 activities ---
  const activityCount = db.prepare('SELECT COUNT(*) as cnt FROM activities').get();
  if (activityCount.cnt === 0) {
    const insertActivity = db.prepare(`
      INSERT INTO activities (user_id, action, resource_type, resource_id, description, ip_address)
      VALUES (?, ?, ?, ?, ?, ?)
    `);

    const activities = [
      [1, 'login', 'auth', null, '用户登录系统', '192.168.1.100'],
      [1, 'create', 'user', 2, '创建用户 alice', '192.168.1.100'],
      [2, 'create', 'item', 1, '创建项目 CI/CD Pipeline', '192.168.1.101'],
      [1, 'update', 'item', 1, '更新项目状态', '192.168.1.100'],
      [3, 'create', 'article', 1, '发布文章 Getting Started', '192.168.1.102'],
      [2, 'update', 'article', 1, '编辑文章内容', '192.168.1.101'],
      [1, 'delete', 'media', 3, '删除过期文件', '192.168.1.100'],
      [4, 'login', 'auth', null, '用户登录系统', '192.168.1.103'],
      [1, 'create', 'item', 2, '创建项目 Design Landing Page', '192.168.1.100'],
      [3, 'create', 'item', 3, '创建项目 Marketing Campaign', '192.168.1.102'],
      [2, 'update', 'settings', null, '更新站点设置', '192.168.1.101'],
      [1, 'create', 'user', 5, '创建用户 editor', '192.168.1.100'],
      [4, 'create', 'item', 4, '创建项目 User Research Report', '192.168.1.103'],
      [1, 'upload', 'media', 1, '上传图片 logo.png', '192.168.1.100'],
      [2, 'login', 'auth', null, '用户登录系统', '192.168.1.101'],
      [3, 'update', 'item', 3, '更新项目优先级', '192.168.1.102'],
      [1, 'create', 'article', 4, '发布 Q3 Roadmap 公告', '192.168.1.100'],
      [4, 'create', 'item', 8, '创建项目 Competitor Analysis', '192.168.1.103'],
      [2, 'update', 'article', 3, '更新设计系统文档', '192.168.1.101'],
      [1, 'login', 'auth', null, '用户登录系统', '192.168.1.100'],
    ];

    for (const a of activities) {
      insertActivity.run(...a);
    }
  }
}

module.exports = { initDatabase };
