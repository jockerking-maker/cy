# ProjectFlow

现代化项目管理工具 - 完整前后端分离架构

## 项目结构

```
projectflow/
├── frontend/                 # React 前端应用
│   ├── src/
│   │   ├── components/       # 通用组件
│   │   ├── pages/            # 页面组件
│   │   ├── contexts/         # React 上下文
│   │   ├── services/         # API 服务
│   │   ├── App.jsx           # 应用入口
│   │   └── index.css         # 全局样式
│   ├── package.json
│   └── vite.config.js
├── backend/                  # Express 后端服务
│   ├── src/
│   │   ├── routes/           # 路由
│   │   ├── controllers/      # 控制器
│   │   ├── models/           # 数据模型
│   │   ├── middleware/       # 中间件
│   │   ├── config/           # 配置
│   │   ├── utils/            # 工具函数
│   │   └── app.js            # 应用入口
│   ├── package.json
│   └── Dockerfile
├── docker-compose.yml        # Docker 编排
├── .env.example              # 环境变量模板
├── API_CONTRACT.md           # API 接口文档
└── README.md                 # 本文件
```

## 快速开始

### 前置要求
- Node.js >= 18
- npm / pnpm

### 后端启动

```bash
cd backend
cp ../.env.example ../.env  # 复制环境变量
npm install
npm start
```

服务启动在 http://localhost:3001

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

服务启动在 http://localhost:5173

### Docker 部署

```bash
docker-compose up -d
```

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户管理 | 注册、登录、权限管理、个人资料、密码修改 |
| 数据管理 | 增删改查、分页、搜索、筛选、排序 |
| 仪表盘 | 统计图表、近期活动、系统概览 |
| 内容管理 | 富文本编辑、文章管理、多媒体上传 |
| 系统设置 | 站点配置、邮件配置、缓存管理、日志查看 |

## 技术栈

- **前端**: React 18 + Vite + React Router + Recharts
- **后端**: Node.js + Express + JWT + SQLite
- **部署**: Docker + Docker Compose

## 默认账号

- 管理员: admin / admin123
