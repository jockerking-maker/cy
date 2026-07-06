const path = require('path');
const dotenv = require('dotenv');

// Load .env from projectflow root
dotenv.config({ path: path.resolve(__dirname, '..', '..', '..', '.env') });

const config = {
  PORT: parseInt(process.env.PORT, 10) || 3001,
  JWT_SECRET: process.env.JWT_SECRET || 'projectflow_jwt_secret_change_in_production',
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '7d',
  UPLOAD_DIR: process.env.UPLOAD_DIR || './uploads',
  DB_PATH: process.env.DB_PATH || './data/projectflow.db',
};

module.exports = config;
