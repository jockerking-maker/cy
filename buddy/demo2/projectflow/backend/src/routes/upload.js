const express = require('express');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const { authenticate } = require('../middleware/auth');
const { logActivity } = require('../middleware/logger');

const router = express.Router();

router.use(authenticate);

// Ensure upload directories exist
function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

const uploadsBase = path.resolve(__dirname, '..', '..', 'uploads');
ensureDir(path.join(uploadsBase, 'images'));
ensureDir(path.join(uploadsBase, 'files'));

// Image storage
const imageStorage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, path.join(uploadsBase, 'images'));
  },
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname);
    const name = Date.now() + '-' + Math.round(Math.random() * 1e9) + ext;
    cb(null, name);
  },
});

// File storage
const fileStorage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, path.join(uploadsBase, 'files'));
  },
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname);
    const name = Date.now() + '-' + Math.round(Math.random() * 1e9) + ext;
    cb(null, name);
  },
});

const imageFilter = (_req, file, cb) => {
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml'];
  if (allowed.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error('只允许上传图片文件 (jpeg, png, gif, webp, svg)'), false);
  }
};

const uploadImage = multer({
  storage: imageStorage,
  fileFilter: imageFilter,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
});

const uploadFile = multer({
  storage: fileStorage,
  limits: { fileSize: 50 * 1024 * 1024 }, // 50MB
});

/**
 * POST /api/v1/upload/image
 * Upload image.
 */
router.post('/image', (req, res) => {
  uploadImage.single('file')(req, res, (err) => {
    if (err) {
      if (err instanceof multer.MulterError) {
        return res.status(400).json({ code: 400, message: '文件上传错误: ' + err.message, data: null });
      }
      return res.status(400).json({ code: 400, message: err.message, data: null });
    }

    if (!req.file) {
      return res.status(400).json({ code: 400, message: '请选择要上传的文件', data: null });
    }

    const fileUrl = '/uploads/images/' + req.file.filename;
    const db = req.app.locals.db;

    try {
      const result = db.prepare(`
        INSERT INTO media (filename, original_name, mime_type, size, url, type, uploader_id)
        VALUES (?, ?, ?, ?, ?, 'image', ?)
      `).run(req.file.filename, req.file.originalname, req.file.mimetype, req.file.size, fileUrl, req.user.id);

      logActivity(req, 'upload', 'media', result.lastInsertRowid, '上传图片 ' + req.file.originalname);

      return res.status(201).json({
        code: 201,
        message: '上传成功',
        data: {
          id: result.lastInsertRowid,
          url: fileUrl,
          filename: req.file.filename,
          original_name: req.file.originalname,
          size: req.file.size,
          mime_type: req.file.mimetype,
        },
      });
    } catch (dbErr) {
      console.error('Upload image db error:', dbErr);
      return res.status(500).json({ code: 500, message: '上传成功但记录失败', data: { url: fileUrl } });
    }
  });
});

/**
 * POST /api/v1/upload/file
 * Upload file.
 */
router.post('/file', (req, res) => {
  uploadFile.single('file')(req, res, (err) => {
    if (err) {
      if (err instanceof multer.MulterError) {
        return res.status(400).json({ code: 400, message: '文件上传错误: ' + err.message, data: null });
      }
      return res.status(400).json({ code: 400, message: err.message, data: null });
    }

    if (!req.file) {
      return res.status(400).json({ code: 400, message: '请选择要上传的文件', data: null });
    }

    const fileUrl = '/uploads/files/' + req.file.filename;
    const db = req.app.locals.db;

    try {
      const result = db.prepare(`
        INSERT INTO media (filename, original_name, mime_type, size, url, type, uploader_id)
        VALUES (?, ?, ?, ?, ?, 'document', ?)
      `).run(req.file.filename, req.file.originalname, req.file.mimetype, req.file.size, fileUrl, req.user.id);

      logActivity(req, 'upload', 'media', result.lastInsertRowid, '上传文件 ' + req.file.originalname);

      return res.status(201).json({
        code: 201,
        message: '上传成功',
        data: {
          id: result.lastInsertRowid,
          url: fileUrl,
          filename: req.file.filename,
          original_name: req.file.originalname,
          size: req.file.size,
          mime_type: req.file.mimetype,
        },
      });
    } catch (dbErr) {
      console.error('Upload file db error:', dbErr);
      return res.status(500).json({ code: 500, message: '上传成功但记录失败', data: { url: fileUrl } });
    }
  });
});

module.exports = router;
