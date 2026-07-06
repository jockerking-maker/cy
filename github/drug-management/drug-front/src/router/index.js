/**
 * 前端路由与权限守卫。
 * <p>
 * meta.requiredRoles 与后端 RBAC 对应；未登录跳转 /login，无权限跳转首页。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'drug',
        name: 'Drug',
        component: () => import('@/views/drug/DrugList.vue'),
        meta: { title: '药品管理', icon: 'Aim', requiredRoles: ['ADMIN', 'PURCHASER'] }
      },
      {
        path: 'supplier',
        name: 'Supplier',
        component: () => import('@/views/supplier/SupplierList.vue'),
        meta: { title: '供应商管理', icon: 'OfficeBuilding', requiredRoles: ['ADMIN', 'PURCHASER'] }
      },
      {
        path: 'purchase',
        name: 'Purchase',
        component: () => import('@/views/purchase/PurchaseOrderList.vue'),
        meta: { title: '采购管理', icon: 'ShoppingCart', requiredRoles: ['ADMIN', 'PURCHASER'] }
      },
      {
        path: 'purchase-audit',
        name: 'PurchaseAudit',
        component: () => import('@/views/purchase/PurchaseAuditList.vue'),
        meta: { title: '采购审核', icon: 'Check', requiredRoles: ['ADMIN', 'AUDITOR'] }
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('@/views/stock/StockList.vue'),
        meta: { title: '库存管理', icon: 'Box', requiredRoles: ['ADMIN', 'WAREHOUSE'] }
      },
      {
        path: 'drug-in',
        name: 'DrugIn',
        component: () => import('@/views/inout/DrugInList.vue'),
        meta: { title: '入库管理', icon: 'Bottom', requiredRoles: ['ADMIN', 'WAREHOUSE'] }
      },
      {
        path: 'drug-out',
        name: 'DrugOut',
        component: () => import('@/views/inout/DrugOutList.vue'),
        meta: { title: '出库管理', icon: 'Top', requiredRoles: ['ADMIN', 'WAREHOUSE'] }
      },
      {
        path: 'warehouse',
        name: 'Warehouse',
        component: () => import('@/views/warehouse/WarehouseList.vue'),
        meta: { title: '仓库管理', icon: 'House', requiredRoles: ['ADMIN', 'WAREHOUSE'] }
      },
      {
        path: 'stock-check',
        name: 'StockCheck',
        component: () => import('@/views/stock/StockCheckList.vue'),
        meta: { title: '库存盘点', icon: 'List', requiredRoles: ['ADMIN', 'WAREHOUSE'] }
      },
      {
        path: 'warning-center',
        name: 'WarningCenter',
        component: () => import('@/views/stock/WarningCenter.vue'),
        meta: { title: '预警中心', icon: 'Bell', requiredRoles: ['ADMIN', 'WAREHOUSE'] }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'User', requiredRoles: ['ADMIN'] }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理', icon: 'Setting', requiredRoles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 无需登录即可访问的路由 */
const whiteList = ['/login']

/** 全局前置守卫：登录校验 + 角色路由权限 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  document.title = to.meta.title ? `${to.meta.title} - 医院药品管理系统` : '医院药品管理系统'

  if (userStore.isLoggedIn) {
    if (to.path === '/login') {
      next('/')
    } else {
      const requiredRoles = to.meta.requiredRoles
      if (requiredRoles && requiredRoles.length > 0) {
        const userRoles = userStore.roles || []
        const hasPermission = userRoles.some(role => requiredRoles.includes(role))
        if (!hasPermission) {
          ElMessage.warning('您没有权限访问该页面')
          next('/dashboard')
        } else {
          next()
        }
      } else {
        next()
      }
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
