# Controller 与 Service 层说明

> 路径：  
> - 接口层：`src/main/java/com/hospital/drugmanagement/controller/`  
> - 业务层：`src/main/java/com/hospital/drugmanagement/service/`（实现类在 `service/impl/`）

---

## 一、这两层是干什么的？

### 1.1 一句话区别

| 层级 | 职责 | 类比 |
|------|------|------|
| **Controller** | 接收前端 HTTP 请求，做参数接收与校验，调用 Service，封装 JSON 返回 | 餐厅**前台**：接单、传菜、上菜 |
| **Service** | 编写业务规则、事务控制、多表协作 | 餐厅**后厨**：真正做菜 |

### 1.2 标准调用关系

```
前端 Axios 请求
    │
    ▼
Controller（@RestController）
    │  接收参数、权限注解、组装 { code, msg, data }
    ▼
Service 接口（IXxxService）
    │
    ▼
ServiceImpl（service/impl/）
    │  业务逻辑、@Transactional
    ▼
Mapper → MySQL
```

### 1.3 答辩 20 秒怎么说

> Controller 是接口层，负责对接前端的 RESTful API；Service 是业务层，负责入库出库、预警、权限等核心逻辑。Controller 不写复杂业务，只调用 Service，这样分层清晰、便于维护和测试。

---

## 二、目录结构总览

### 2.1 controller 包（15 个文件）

```
controller/
├── PublicController.java          # 公开接口（无需登录）
├── SysUserController.java         # 用户登录、注册、用户管理
├── SysRoleController.java         # 角色与菜单权限
├── DrugInfoController.java        # 药品信息
├── SupplierInfoController.java    # 供应商
├── WarehouseInfoController.java   # 仓库
├── PurchaseOrderController.java   # 采购订单 + 审核
├── DrugInController.java          # 入库
├── DrugOutController.java         # 出库
├── StockController.java           # 库存查询
├── StockCheckController.java      # 库存盘点
├── StockWarningController.java    # 库存预警
├── DrugLockController.java        # 药品锁定
├── DashboardController.java       # 首页统计
└── SystemNoticeController.java    # 系统公告
```

### 2.2 service 包（接口 + impl）

```
service/
├── IXxxService.java / XxxService.java    ← 接口（定义能做什么）
└── impl/
    └── XxxServiceImpl.java               ← 实现类（具体怎么做）
```

**本项目共 21 个 Service 接口 + 21 个实现类。**

---

## 三、Controller 层详解

### 3.1 Controller 的通用写法

每个 Controller 通常包含：

```java
@RestController                              // 标记为 REST 接口，返回 JSON
@RequestMapping("/api/xxx")                  // 统一 URL 前缀
@RequireRole({"ADMIN", "WAREHOUSE"})          // 可选：角色权限
public class XxxController {

    @Autowired
    private IXxxService xxxService;           // 注入 Service

    @GetMapping("/list")                      // GET 查询
    @PostMapping                              // POST 新增
    @PutMapping                               // PUT 修改
    @DeleteMapping("/{id}")                  // DELETE 删除
}
```

**统一响应格式：**

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "total": 100
}
```

### 3.2 各 Controller 文件说明

#### （1）PublicController — 公开接口

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/public` |
| 权限 | 白名单，无需登录 |
| 主要接口 | `GET /login-info` — 登录页展示统计与公告 |
| 调用 Service | `DashboardService`、`SystemNoticeService` |

---

#### （2）SysUserController — 用户与认证

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/user` |
| 权限 | 登录/注册公开；用户 CRUD 仅 ADMIN |
| 主要接口 | |
| | `POST /login` — 登录，返回 JWT |
| | `POST /register` — 注册（采购员/库管员） |
| | `GET /register-roles` — 可注册角色列表 |
| | `GET /current` — 当前用户信息 |
| | `GET /list`、`POST`、`PUT`、`DELETE` — 用户管理 |
| | `POST /changePassword` — 修改密码 |
| 调用 Service | `ISysUserService`（`login` 在 ServiceImpl 中实现） |
| 特点 | 登录逻辑在 **Service**，用户列表查询在 **Controller** 中拼装较多 |

---

#### （3）SysRoleController — 角色权限

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/role` |
| 权限 | 仅 ADMIN |
| 主要接口 | 角色 CRUD、菜单树查询、`POST /assignPerms` 分配菜单 |
| 调用 Service | `ISysRoleService`、`ISysMenuService`、`ISysRoleMenuService` |

---

#### （4）DrugInfoController — 药品信息

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/drug` |
| 权限 | ADMIN、PURCHASER |
| 主要接口 | `/list` 分页查询、`/{id}` 详情、增删改、`/warning` 更新预警值 |
| 调用 Service | `DrugInfoService`（主要用 MyBatis-Plus 自带 `page/save/update`） |
| 特点 | **查询逻辑写在 Controller**，ServiceImpl 几乎为空 |

---

#### （5）SupplierInfoController — 供应商

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/supplier` |
| 权限 | ADMIN、PURCHASER |
| 主要接口 | 标准 CRUD |
| 调用 Service | `ISupplierInfoService` |

---

#### （6）WarehouseInfoController — 仓库

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/warehouse` |
| 权限 | ADMIN、WAREHOUSE |
| 主要接口 | 标准 CRUD |
| 调用 Service | `IWarehouseInfoService` |

---

#### （7）PurchaseOrderController — 采购与审核

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/purchase/order` |
| 权限 | 类级别：ADMIN、PURCHASER、AUDITOR；审核接口额外限制 AUDITOR |
| 主要接口 | |
| | `GET /list`、`GET /{id}` — 订单列表与详情（含明细、审核记录） |
| | `POST` — 创建订单（主表 + 明细） |
| | `POST /audit/{id}` — **审核通过/驳回** |
| | `POST /cancel/{id}` — 作废订单 |
| 调用 Service | `IPurchaseOrderService`、`IPurchaseOrderItemService`、`IAuditRecordService` |
| 特点 | **创建订单、审核流程写在 Controller**，ServiceImpl 仅为空壳继承 `ServiceImpl` |

---

#### （8）DrugInController — 入库

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/drug/in` |
| 权限 | ADMIN、WAREHOUSE |
| 主要接口 | |
| | `GET /next-batch-no` — 生成批次号 |
| | `GET /list` — 入库单列表 |
| | `POST` — **入库登记** |
| 调用 Service | `IDrugInService` |
| 特点 | 核心业务在 **`DrugInServiceImpl.saveDrugIn()`**（事务 + 更新库存 + 预警） |

---

#### （9）DrugOutController — 出库

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/drug/out` |
| 权限 | ADMIN、WAREHOUSE |
| 主要接口 | 列表、详情、`POST` 出库 |
| 调用 Service | `IDrugOutService` |
| 特点 | 核心业务在 **`DrugOutServiceImpl.saveDrugOut()`**（库存校验 + FIFO 扣减 + 预警） |

---

#### （10）StockController — 库存查询

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/stock` |
| 权限 | ADMIN、WAREHOUSE |
| 主要接口 | `/list` 分组库存、`/warning/list` 近效期列表 |
| 调用 Service | `StockService` |
| 特点 | 分组展示、效期标签逻辑在 **`StockServiceImpl`** |

---

#### （11）StockCheckController — 库存盘点

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/stock-check` |
| 权限 | ADMIN、WAREHOUSE |
| 主要接口 | 创建盘点单、`PUT /complete` 完成盘点、`PUT /cancel` 取消 |
| 调用 Service | `IStockCheckService`、`IStockCheckItemService`（多为 CRUD） |
| 特点 | **盘点完成、库存同步逻辑写在 Controller** |

---

#### （12）StockWarningController — 库存预警

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/stock-warning` |
| 权限 | ADMIN、WAREHOUSE |
| 主要接口 | 预警列表、处理、统计、趋势图、临期/滞销扫描 |
| 调用 Service | `StockWarningService` |
| 特点 | 业务最复杂的 Service 之一，**`StockWarningServiceImpl`** |

---

#### （13）DrugLockController — 药品锁定

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/drug-lock` |
| 权限 | ADMIN、WAREHOUSE |
| 主要接口 | `POST /lock`、`POST /unlock/{lockId}` |
| 调用 Service | `DrugLockService` |
| 特点 | 锁定/解锁事务在 **`DrugLockServiceImpl`** |

---

#### （14）DashboardController — 首页统计

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/dashboard` |
| 主要接口 | `/stats` 总览、`/drugTypeStats` 分类图、`/stockWarningStats` 预警图 |
| 调用 Service | `DashboardService` |
| 特点 | 聚合统计在 **`DashboardServiceImpl`** |

---

#### （15）SystemNoticeController — 系统公告

| 项目 | 内容 |
|------|------|
| 路径前缀 | `/api/notice` |
| 权限 | 查询公开；增删改仅 ADMIN |
| 主要接口 | 公告 CRUD |
| 调用 Service | `SystemNoticeService` |

---

### 3.3 Controller 与前端 API 对应表

| Controller | 前端 api 文件 | 前端页面 |
|------------|---------------|----------|
| SysUserController | `api/user.js` | `Login.vue`、`UserList.vue` |
| SysRoleController | `api/role.js` | `RoleList.vue` |
| DrugInfoController | `api/drug.js` | `DrugList.vue` |
| SupplierInfoController | `api/supplier.js` | `SupplierList.vue` |
| WarehouseInfoController | `api/warehouse.js` | `WarehouseList.vue` |
| PurchaseOrderController | `api/purchase.js` | `PurchaseOrderList.vue`、`PurchaseAuditList.vue` |
| DrugInController | `api/drugIn.js` | `DrugInList.vue` |
| DrugOutController | `api/drugOut.js` | `DrugOutList.vue` |
| StockController | `api/stock.js` | `StockList.vue` |
| StockCheckController | `api/stockCheck.js` | `StockCheckList.vue` |
| StockWarningController | `api/stockWarning.js` | `WarningCenter.vue` |
| DrugLockController | `api/drugLock.js` | `StockList.vue`（锁定功能） |
| DashboardController | `api/dashboard.js` | `Dashboard.vue` |
| SystemNoticeController | `api/notice.js` | `Dashboard.vue` |
| PublicController | `api/public.js` | `Login.vue` |

---

## 四、Service 层详解

### 4.1 接口与实现类的关系

```
service/IDrugInService.java          ← 接口：声明方法
service/impl/DrugInServiceImpl.java ← 实现：写具体逻辑
```

**为什么分接口和实现？**

- 符合面向接口编程，Controller 只依赖接口
- 方便以后换实现或做单元测试（Mock）
- 与 Spring 依赖注入配合：`@Autowired IDrugInService drugInService`

### 4.2 MyBatis-Plus 的 IService

大部分 Service 接口继承了：

```java
public interface IDrugInService extends IService<DrugIn> {
    // 自定义方法...
}
```

`IService<T>` 已自带常用方法（无需自己写）：

| 方法 | 作用 |
|------|------|
| `save(entity)` | 新增 |
| `updateById(entity)` | 按 ID 更新 |
| `removeById(id)` | 按 ID 删除 |
| `getById(id)` | 按 ID 查询 |
| `list(wrapper)` | 条件列表 |
| `page(page, wrapper)` | 分页查询 |
| `count(wrapper)` | 计数 |

实现类继承：

```java
public class DrugInServiceImpl
    extends ServiceImpl<DrugInMapper, DrugIn>   // 注入 Mapper
    implements IDrugInService {
}
```

这样 `drugInService.save()` 等方法可以直接用。

---

### 4.3 各 Service 分类说明

#### A. 核心业务 Service（逻辑重，答辩重点）

| 接口 | 实现类 | 自定义核心方法 | 说明 |
|------|--------|----------------|------|
| `IDrugInService` | `DrugInServiceImpl` | `saveDrugIn`、`generateBatchNo`、`getDrugInList` | 入库事务、更新库存、触发预警 |
| `IDrugOutService` | `DrugOutServiceImpl` | `saveDrugOut`、`getDrugOutList` | 库存校验、FIFO 出库、触发预警 |
| `StockWarningService` | `StockWarningServiceImpl` | `checkAndCreateWarning`、`handleWarning`、临期/滞销扫描、统计趋势 | 预警全流程 |
| `DrugLockService` | `DrugLockServiceImpl` | `lockDrug`、`unlockDrug` | 锁定/解锁库存 |
| `StockService` | `StockServiceImpl` | `getGroupedStockList`、`checkStock` | 库存分组、效期展示 |
| `ISysUserService` | `SysUserServiceImpl` | `login`、`getCurrentUserInfo` | JWT 登录、角色菜单 |
| `DashboardService` | `DashboardServiceImpl` | `getStats`、`getDrugTypeStats` | 首页聚合统计 |

#### B. 辅助业务 Service（有少量自定义方法）

| 接口 | 实现类 | 自定义方法 |
|------|--------|------------|
| `IAuditRecordService` | `AuditRecordServiceImpl` | `getAuditRecordsByOrderId`、`saveAuditRecord` |
| `ISysMenuService` | `SysMenuServiceImpl` | `getAllMenus`、`getMenusByRoleId` |
| `ISysRoleMenuService` | `SysRoleMenuServiceImpl` | `assignMenusToRole`、`getMenuIdsByRoleId` |
| `SystemNoticeService` | `SystemNoticeServiceImpl` | `getNoticeList` |

#### C. 薄 Service（仅继承 IService，无自定义业务）

以下实现类**只有空壳**，实际 CRUD 在 Controller 里直接调用 `service.save()` / `service.page()`：

| 接口 | 实现类 | 对应实体 |
|------|--------|----------|
| `DrugInfoService` | `DrugInfoServiceImpl` | DrugInfo |
| `IPurchaseOrderService` | `PurchaseOrderServiceImpl` | PurchaseOrder |
| `IPurchaseOrderItemService` | `PurchaseOrderItemServiceImpl` | PurchaseOrderItem |
| `ISupplierInfoService` | `SupplierInfoServiceImpl` | SupplierInfo |
| `IWarehouseInfoService` | `WarehouseInfoServiceImpl` | WarehouseInfo |
| `IDrugStockService` | `DrugStockServiceImpl` | DrugStock |
| `IStockCheckService` | `StockCheckServiceImpl` | StockCheck |
| `IStockCheckItemService` | `StockCheckItemServiceImpl` | StockCheckItem |
| `ISysRoleService` | `SysRoleServiceImpl` | SysRole |
| `SysOperationLogService` | `SysOperationLogServiceImpl` | SysOperationLog |

> **说明**：这是毕业设计中常见的写法——简单模块逻辑写在 Controller，复杂模块写在 ServiceImpl。答辩时可以说「入库出库等核心流程放在 Service 层并用事务保证一致性」。

---

### 4.4 核心 Service 方法详解

#### DrugInServiceImpl — 入库

```java
@Transactional
public boolean saveDrugIn(DrugIn drugIn) {
    // 1. 校验数量、效期、批次
    // 2. 生成入库单号 RK+时间戳
    // 3. 保存 drug_in
    // 4. updateStock() — 同批次累加或新建 drug_stock
    // 5. stockWarningService.checkAndCreateWarning()
}
```

#### DrugOutServiceImpl — 出库

```java
@Transactional
public boolean saveDrugOut(DrugOut drugOut) {
    // 1. checkStock() — 可用库存 = stock_num - lock_num
    // 2. 保存 drug_out
    // 3. updateStock() — 按 expiry_date 升序 FIFO 扣减
    // 4. checkAndCreateWarning()
}
```

#### StockWarningServiceImpl — 预警

```java
void checkAndCreateWarning(Long drugId, Long warehouseId);
// 入库/出库后调用：对比 warning_num / max_warning_num，自动生成预警记录

boolean handleWarning(Long warningId, Long handleUserId, String remark);
// 库管员处理预警

Map<String, Object> checkAndCreateNearExpiryWarning();
// 扫描临期、过期批次
```

#### SysUserServiceImpl — 登录

```java
LoginResponse login(String username, String password) {
    // 1. 查用户、校验密码（BCrypt）
    // 2. jwtUtil.generateToken()
    // 3. 查角色、查菜单列表
    // 4. 返回 LoginResponse
}
```

---

## 五、Controller 与 Service 对照关系

```
┌─────────────────────────┬──────────────────────────────────────┐
│ Controller              │ 主要调用的 Service                    │
├─────────────────────────┼──────────────────────────────────────┤
│ PublicController        │ DashboardService, SystemNoticeService │
│ SysUserController       │ ISysUserService                       │
│ SysRoleController       │ ISysRoleService, ISysMenuService,     │
│                         │ ISysRoleMenuService                   │
│ DrugInfoController      │ DrugInfoService, StockService,        │
│                         │ StockWarningService                   │
│ SupplierInfoController  │ ISupplierInfoService                  │
│ WarehouseInfoController │ IWarehouseInfoService                 │
│ PurchaseOrderController │ IPurchaseOrderService,                │
│                         │ IPurchaseOrderItemService,            │
│                         │ IAuditRecordService, ISysUserService  │
│ DrugInController        │ IDrugInService                        │
│ DrugOutController       │ IDrugOutService                       │
│ StockController         │ StockService                          │
│ StockCheckController    │ IStockCheckService,                   │
│                         │ IStockCheckItemService, DrugStockMapper│
│ StockWarningController  │ StockWarningService                   │
│ DrugLockController      │ DrugLockService                       │
│ DashboardController     │ DashboardService                      │
│ SystemNoticeController  │ SystemNoticeService                   │
└─────────────────────────┴──────────────────────────────────────┘
```

---

## 六、代码写在哪里的规律（本项目）

| 业务类型 | 通常写在哪里 | 举例 |
|----------|--------------|------|
| 接收参数、返回 JSON | Controller | 所有 `@GetMapping` |
| 权限控制 | Controller 类/方法上的 `@RequireRole` | 采购审核仅 AUDITOR |
| 简单 CRUD | Controller 直接调 `service.page()` | 药品列表、供应商 |
| 多表事务、复杂规则 | ServiceImpl | 入库、出库、锁定 |
| 数据库访问 | Mapper（ServiceImpl 注入） | `DrugStockMapper` |
| 跨模块调用 | Service 注入另一个 Service | 入库后调 `StockWarningService` |

---

## 七、举例：一次「入库」请求经过哪些类

```
1. 前端 POST /api/drug/in
       │
2. AuthInterceptor          校验 JWT
       │
3. DrugInController.save()  接收 JSON，转成 DrugIn 对象
       │
4. DrugInServiceImpl.saveDrugIn()   【@Transactional】
       ├── drugInMapper.insert()     写 drug_in
       ├── drugStockMapper 更新       写 drug_stock
       └── stockWarningService.checkAndCreateWarning()
       │
5. Controller 封装 { code:200, msg:"保存成功" }
       │
6. 前端 request.js 解析，刷新列表
```

---

## 八、常见问题（答辩用）

**Q：Controller 和 Service 为什么要分开？**

> 分层解耦。Controller 只管 HTTP，Service 管业务。改业务规则不用动接口 URL，改接口格式不用动事务逻辑。

**Q：为什么有些 ServiceImpl 是空的？**

> 简单模块只用 MyBatis-Plus 自带的 CRUD，在 Controller 里直接 `drugInfoService.page()` 就够了。复杂模块才在 ServiceImpl 里写 `saveDrugIn` 这类方法。

**Q：@Transactional 写在哪？**

> 写在 ServiceImpl 的方法上，如 `DrugInServiceImpl.saveDrugIn()`，保证入库和改库存在同一事务。

**Q：@RequireRole 写在哪？**

> 写在 Controller 类或方法上，由 `AuthInterceptor` 配合注解做接口级权限控制。

**Q：接口名为什么有的叫 IxxxService，有的叫 XxxService？**

> 都是 Service 接口，命名风格不统一，功能一样。`I` 前缀表示 Interface，如 `IDrugInService`。

---

## 九、文件清单速查

### controller（15 个）

| 文件名 | URL 前缀 |
|--------|----------|
| PublicController | `/api/public` |
| SysUserController | `/api/user` |
| SysRoleController | `/api/role` |
| DrugInfoController | `/api/drug` |
| SupplierInfoController | `/api/supplier` |
| WarehouseInfoController | `/api/warehouse` |
| PurchaseOrderController | `/api/purchase/order` |
| DrugInController | `/api/drug/in` |
| DrugOutController | `/api/drug/out` |
| StockController | `/api/stock` |
| StockCheckController | `/api/stock-check` |
| StockWarningController | `/api/stock-warning` |
| DrugLockController | `/api/drug-lock` |
| DashboardController | `/api/dashboard` |
| SystemNoticeController | `/api/notice` |

### service 接口（21 个）+ impl（21 个）

| 接口 | 实现类 | 业务重量 |
|------|--------|----------|
| ISysUserService | SysUserServiceImpl | ★★★ |
| IDrugInService | DrugInServiceImpl | ★★★ |
| IDrugOutService | DrugOutServiceImpl | ★★★ |
| StockWarningService | StockWarningServiceImpl | ★★★ |
| DrugLockService | DrugLockServiceImpl | ★★ |
| StockService | StockServiceImpl | ★★ |
| DashboardService | DashboardServiceImpl | ★★ |
| IAuditRecordService | AuditRecordServiceImpl | ★ |
| ISysMenuService | SysMenuServiceImpl | ★ |
| ISysRoleMenuService | SysRoleMenuServiceImpl | ★ |
| SystemNoticeService | SystemNoticeServiceImpl | ★ |
| DrugInfoService | DrugInfoServiceImpl | ☆（空） |
| IPurchaseOrderService | PurchaseOrderServiceImpl | ☆ |
| IPurchaseOrderItemService | PurchaseOrderItemServiceImpl | ☆ |
| ISupplierInfoService | SupplierInfoServiceImpl | ☆ |
| IWarehouseInfoService | WarehouseInfoServiceImpl | ☆ |
| IDrugStockService | DrugStockServiceImpl | ☆ |
| IStockCheckService | StockCheckServiceImpl | ☆ |
| IStockCheckItemService | StockCheckItemServiceImpl | ☆ |
| ISysRoleService | SysRoleServiceImpl | ☆ |
| SysOperationLogService | SysOperationLogServiceImpl | ☆ |

★★★ = 答辩建议重点阅读

---

## 十、推荐阅读顺序（熟悉代码）

1. `SysUserController` + `SysUserServiceImpl` — 理解登录与 JWT  
2. `DrugInfoController` — 理解最简单的 CRUD 接口  
3. `PurchaseOrderController` — 理解主从表与审核  
4. `DrugInController` + `DrugInServiceImpl` — 理解事务与库存  
5. `DrugOutController` + `DrugOutServiceImpl` — 理解 FIFO  
6. `StockWarningServiceImpl` — 理解预警机制  
