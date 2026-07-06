<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="sidebar-header">
        <div class="brand-mark">药</div>
        <div class="sidebar-brand">
          <span class="brand-name">药品管理系统</span>
          <span class="brand-tag">Hospital Drug Management</span>
        </div>
      </div>

      <div class="sidebar-menu">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item
            v-for="menu in menuRoutes"
            :key="menu.path"
            :index="menu.path"
          >
            <el-icon><component :is="menu.meta.icon" /></el-icon>
            <span>{{ menu.meta.title }}</span>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="sidebar-footer">
        <span>{{ userStore.realName || '用户' }}</span>
        <span class="dot">·</span>
        <span>{{ currentRole }}</span>
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="top-header">
        <div class="header-left">
          <nav class="breadcrumb">
            <span class="breadcrumb-root">工作台</span>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-current">{{ currentTitle }}</span>
          </nav>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="header-user">
              <div class="user-avatar">{{ userStore.realName?.charAt(0) || 'U' }}</div>
              <span class="user-name">{{ userStore.realName }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog
      v-model="profileDialogVisible"
      title="个人信息"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-width="80px"
        v-loading="loading"
      >
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="profileForm.realName" placeholder="请输入姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag v-if="profileForm.roles?.length" size="small">{{ profileForm.roles[0] }}</el-tag>
          <span v-else>-</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = true">修改密码</el-button>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="changePasswordHandler" :loading="changingPassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import appRouter from '@/router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getCurrentUser, changePassword, updateUserInfo } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '首页')

const currentRole = computed(() => {
  const roles = userStore.userRoles || []
  if (roles.includes('ADMIN')) return '管理员'
  if (roles.includes('AUDITOR')) return '审核员'
  if (roles.includes('PURCHASER')) return '采购员'
  if (roles.includes('WAREHOUSE')) return '库管员'
  return '普通用户'
})

const menuRoutes = computed(() => {
  const userMenus = userStore.menus || []
  if (userMenus.length > 0) {
    return userMenus.map(menu => {
      let path = menu.path || '/'
      if (!path.startsWith('/')) path = '/' + path
      return {
        path,
        meta: {
          title: menu.menuName || '未命名',
          icon: menu.icon || 'Document'
        }
      }
    })
  }

  const roles = userStore.userRoles || []
  const layoutRoute = appRouter.options.routes.find(r => r.path === '/')
  if (!layoutRoute?.children) return []

  return layoutRoute.children
    .filter(child => child.meta?.title)
    .filter(child => {
      const required = child.meta.requiredRoles
      if (!required || required.length === 0) return true
      return roles.some(role => required.includes(role))
    })
    .map(child => ({
      path: child.path.startsWith('/') ? child.path : `/${child.path}`,
      meta: {
        title: child.meta.title,
        icon: child.meta.icon || 'Document'
      }
    }))
})

const profileDialogVisible = ref(false)
const profileFormRef = ref(null)
const loading = ref(false)
const savingProfile = ref(false)
const profileForm = reactive({
  userId: null,
  username: '',
  realName: '',
  phone: '',
  email: '',
  roles: []
})

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{
    validator: (_rule, value, callback) => {
      if (!value || !String(value).trim()) {
        callback()
        return
      }
      if (!/^1[3-9]\d{9}$/.test(String(value).trim())) {
        callback(new Error('请输入正确的手机号'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }],
  email: [{
    validator: (_rule, value, callback) => {
      if (!value || !String(value).trim()) {
        callback()
        return
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value).trim())) {
        callback(new Error('请输入正确的邮箱地址'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }]
}

const showPasswordDialog = ref(false)
const changingPassword = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const loadUserInfo = async () => {
  loading.value = true
  try {
    const res = await getCurrentUser()
    if (res.code === 200 && res.data) {
      const userInfo = res.data.userInfo
      profileForm.userId = userInfo?.userId || null
      profileForm.username = userInfo?.username || ''
      profileForm.realName = userInfo?.realName || ''
      profileForm.phone = userInfo?.phone || ''
      profileForm.email = userInfo?.email || ''
      profileForm.roles = res.data?.roles || []
    }
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

const saveProfile = async () => {
  if (!profileFormRef.value) return
  await profileFormRef.value.validate(async (valid) => {
    if (!valid) return
    savingProfile.value = true
    try {
      await updateUserInfo({
        realName: profileForm.realName.trim(),
        phone: profileForm.phone?.trim() || '',
        email: profileForm.email?.trim() || ''
      })
      ElMessage.success('个人信息已保存')
      await userStore.getCurrentUser()
      profileDialogVisible.value = false
    } catch {
      // 错误由拦截器提示
    } finally {
      savingProfile.value = false
    }
  })
}

const changePasswordHandler = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('密码长度不能少于 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  changingPassword.value = true
  try {
    const res = await changePassword({
      userId: profileForm.userId,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      showPasswordDialog.value = false
      userStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(res.msg || '密码修改失败')
    }
  } catch (error) {
    ElMessage.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      userStore.logout()
      router.push('/login')
    } catch {}
  } else if (command === 'profile') {
    await loadUserInfo()
    profileDialogVisible.value = true
  } else if (command === 'password') {
    showPasswordDialog.value = true
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
  background: var(--bg-base);
}

.sidebar {
  background: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(0, 0, 0, 0.15);
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sidebar-brand {
  min-width: 0;
}

.brand-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-tag {
  display: block;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 1px;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

:deep(.el-menu) {
  border-right: none !important;
  background: transparent !important;
  padding: 0 8px;
}

:deep(.el-menu-item) {
  border-radius: var(--radius-md) !important;
  margin: 1px 0 !important;
  height: 40px !important;
  line-height: 40px !important;
  color: var(--text-sidebar) !important;
  font-size: 13px !important;
  position: relative;
}

:deep(.el-menu-item:hover) {
  background: var(--bg-sidebar-hover) !important;
  color: #fff !important;
}

:deep(.el-menu-item.is-active) {
  background: var(--bg-sidebar-active) !important;
  color: var(--text-sidebar-active) !important;
  font-weight: 500;
}

:deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: var(--primary-light);
}

:deep(.el-menu-item .el-icon) {
  font-size: 16px !important;
  margin-right: 8px !important;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-footer .dot {
  margin: 0 4px;
}

.main-container {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 52px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.breadcrumb {
  font-size: 13px;
  color: var(--text-tertiary);
}

.breadcrumb-current {
  color: var(--text-primary);
  font-weight: 500;
}

.breadcrumb-sep {
  margin: 0 6px;
  color: var(--border);
}

.header-right {
  display: flex;
  align-items: center;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.header-user:hover {
  background: var(--bg-base);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.user-arrow {
  font-size: 12px;
  color: var(--text-tertiary);
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: var(--bg-base);
  padding: 0;
}
</style>
