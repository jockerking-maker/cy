# 医院药品管理系统 - 前端

基于 Vue 3 + Element Plus + Vite 的前端项目，与 Spring Boot 后端对接。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Element Plus** - 基于 Vue 3 的组件库
- **Vite** - 下一代前端构建工具
- **Pinia** - Vue 官方状态管理库
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP 客户端
- **ECharts** - 数据可视化图表库

## 功能模块

### 1. 系统管理
- 用户管理：用户 CRUD、重置密码、角色分配
- 角色管理：角色 CRUD、权限分配

### 2. 基础数据管理
- 药品管理：药品信息 CRUD、上下架管理
- 供应商管理：供应商 CRUD、状态管理

### 3. 采购管理
- 采购单管理：创建采购单、多级审核流程、状态跟踪
- 采购明细：药品明细管理、金额统计

### 4. 库存管理
- 库存查询：实时库存查询、多仓库支持
- 库存预警：低库存预警、过期预警
- 库存盘点：盘点单创建、盈亏处理

### 5. 出入库管理
- 入库管理：采购入库、手动入库、批次管理
- 出库管理：门诊/住院领药、调拨、报废

### 6. 报表统计
- 采购报表：月度/季度/年度统计
- 出入库报表：趋势分析
- 库存报表：分类统计
- 财务报表：毛利分析

## 安装步骤

### 1. 安装 Node.js

确保已安装 Node.js (推荐 v18+):
```bash
node -v
```

### 2. 安装依赖

进入前端项目目录:
```bash
cd E:\java\code\drug-management\frontend
```

安装项目依赖:
```bash
npm install
```

或使用国内镜像加速:
```bash
npm install --registry=https://registry.npmmirror.com
```

### 3. 启动开发服务器

确保后端 Spring Boot 应用已在 8080 端口运行，然后启动前端:

```bash
npm run dev
```

访问地址：http://localhost:3000

### 4. 构建生产版本

```bash
npm run build
```

构建后的文件在 `dist` 目录中

### 5. 预览生产构建

```bash
npm run preview
```

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口服务
│   │   ├── drug.js       # 药品管理 API
│   │   ├── user.js       # 用户管理 API
│   │   ├── supplier.js   # 供应商 API
│   │   ├── purchase.js   # 采购管理 API
│   │   ├── stock.js      # 库存管理 API
│   │   ├── drugIn.js     # 入库管理 API
│   │   ├── drugOut.js    # 出库管理 API
│   │   ├── report.js     # 报表统计 API
│   │   └── ...
│   ├── layout/           # 布局组件
│   │   └── Index.vue     # 主布局
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── store/            # 状态管理
│   │   └── user.js       # 用户状态
│   ├── utils/            # 工具函数
│   │   └── request.js    # Axios 封装
│   ├── views/            # 页面组件
│   │   ├── Login.vue     # 登录页
│   │   ├── Dashboard.vue # 首页
│   │   ├── drug/         # 药品管理
│   │   ├── supplier/     # 供应商管理
│   │   ├── purchase/     # 采购管理
│   │   ├── stock/        # 库存管理
│   │   ├── inout/        # 出入库管理
│   │   ├── report/       # 报表统计
│   │   └── system/       # 系统管理
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── package.json
└── vite.config.js
```

## 默认登录账号

```
用户名：admin
密码：123456
```

## 代理配置

开发环境下，API 请求会代理到后端服务器:

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 跨域问题

如果遇到跨域问题，可以在后端添加 CORS 配置:

```java
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class DrugInfoController {
    // ...
}
```

## 环境变量

可以创建 `.env` 文件配置环境变量:

```env
# .env.development
VITE_APP_BASE_API=/api

# .env.production
VITE_APP_BASE_API=https://your-api-domain.com
```

## 常见问题

### 1. 依赖安装失败

尝试清除缓存后重新安装:
```bash
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### 2. 端口被占用

修改 `vite.config.js`:
```javascript
server: {
  port: 3001  // 改为其他端口
}
```

### 3. 后端接口无法访问

确保:
- 后端应用已启动
- 端口号正确 (默认 8080)
- 数据库连接正常

## 开发规范

### 命名规范

- 组件名使用 PascalCase (大驼峰)
- 文件名使用 PascalCase 或 camelCase
- 变量使用 camelCase (小驼峰)
- 常量使用 UPPER_SNAKE_CASE

### 代码风格

- 使用 Composition API (script setup)
- 优先使用 ref 和 reactive
- 异步操作使用 async/await

## 部署说明

### 1. 构建

```bash
npm run build
```

### 2. 上传 dist 目录到服务器

可以使用 FTP、SCP 等方式上传到 Web 服务器

### 3. Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        root /path/to/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 后续优化建议

1. **权限控制**: 实现按钮级别的权限控制
2. **数据导出**: 完善 Excel导入导出功能
3. **移动端适配**: 响应式布局，支持手机端
4. **消息通知**: WebSocket 实时推送预警信息
5. **性能优化**: 路由懒加载、组件懒加载
6. **日志审计**: 记录用户操作日志

## License

MIT
