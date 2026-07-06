<template>
  <div class="login-page">
    <div class="login-bg" aria-hidden="true">
      <img class="login-bg__img" :src="loginBgUrl" alt="" />
    </div>
    <div class="login-bg-overlay" aria-hidden="true"></div>

    <div class="login-shell" :class="{ 'login-shell--register': isRegister }">
      <aside class="shell-brand">
        <div class="brand-content">
          <div class="brand-logo">
            <span class="brand-logo__icon">药</span>
            <span class="brand-logo__text">Hospital Drug</span>
          </div>

          <div class="brand-hero">
            <h1>医院药品管理系统</h1>
            <p>库存 · 采购 · 出入库 · 预警</p>
          </div>

          <ul class="brand-features">
            <li><el-icon><CircleCheck /></el-icon>批次追溯与效期管理</li>
            <li><el-icon><CircleCheck /></el-icon>采购审核与库存预警</li>
            <li><el-icon><CircleCheck /></el-icon>出入库与盘点一体化</li>
          </ul>

          <div class="brand-clock">
            <span class="brand-clock__date">{{ clockDate }}</span>
            <span class="brand-clock__time">{{ clockTime }}</span>
          </div>
        </div>
      </aside>

      <main class="shell-form">
        <div class="form-wrap">
          <div class="form-tabs">
            <button
              type="button"
              class="form-tab"
              :class="{ active: !isRegister }"
              @click="switchToLogin"
            >
              登录
            </button>
            <button
              type="button"
              class="form-tab"
              :class="{ active: isRegister }"
              @click="switchToRegister"
            >
              注册
            </button>
          </div>

          <el-form
            v-if="!isRegister"
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="auth-form"
            @submit.prevent="handleLogin"
          >
            <el-form-item prop="username">
              <el-autocomplete
                v-model="loginForm.username"
                :fetch-suggestions="queryRecentUsers"
                placeholder="用户名"
                size="large"
                clearable
                :prefix-icon="User"
                :trigger-on-focus="true"
                @select="handleSelectRecentUser"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input
                  v-model="loginForm.captcha"
                  placeholder="验证码"
                  size="large"
                  maxlength="3"
                  @keyup.enter="handleLogin"
                />
                <button type="button" class="captcha-btn" @click="refreshCaptcha" title="点击刷新">
                  {{ captchaQuestion }}
                </button>
              </div>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="rememberUsername">记住用户名</el-checkbox>
              <a class="text-link" @click="forgotVisible = true">忘记密码？</a>
            </div>

            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="submit-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form>

          <el-form
            v-else
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            class="auth-form auth-form--register"
          >
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名（3-20 字符）" size="large" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少 6 位）" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="真实姓名" size="large" :prefix-icon="UserFilled" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号（选填）" size="large" :prefix-icon="Phone" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱（选填）" size="large" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="roleId">
              <el-select v-model="registerForm.roleId" placeholder="选择角色" size="large" style="width: 100%">
                <el-option v-for="role in registerRoles" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
              </el-select>
            </el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="handleRegister">
              注 册
            </el-button>
            <p class="register-note">仅开放采购员、库管员注册，管理员请联系系统管理员</p>
          </el-form>
        </div>
      </main>
    </div>

    <el-dialog v-model="forgotVisible" title="忘记密码" width="420px" align-center>
      <div class="forgot-body">
        <p>本系统暂不支持在线找回密码，请联系系统管理员重置：</p>
        <ul>
          <li>管理员账号：<strong>admin</strong></li>
          <li>联系邮箱：<strong>admin@hospital.com</strong></li>
          <li>联系电话：<strong>138-0013-8000</strong></li>
        </ul>
        <p class="forgot-tip">重置后请使用新密码登录，并建议立即在个人中心修改密码。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="forgotVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Lock, UserFilled, Phone, Message, CircleCheck
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import loginBgUrl from '@/assets/login-bg.jpg'

const REMEMBER_KEY = 'login_remember_username'
const USERNAME_KEY = 'login_saved_username'
const RECENT_KEY = 'login_recent_usernames'
const MAX_RECENT = 5

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const isRegister = ref(false)
const registerRoles = ref([])
const rememberUsername = ref(false)
const forgotVisible = ref(false)

const loginForm = reactive({ username: '', password: '', captcha: '' })
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  roleId: null
})

const clockDate = ref('')
const clockTime = ref('')

const captchaAnswer = ref(0)
const captchaQuestion = ref('')

let clockTimer = null

const validateCaptcha = (rule, value, callback) => {
  if (!value && value !== 0) {
    callback(new Error('请输入验证码'))
    return
  }
  if (Number(value) !== captchaAnswer.value) {
    callback(new Error('验证码错误'))
    return
  }
  callback()
}

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  captcha: [{ validator: validateCaptcha, trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const refreshCaptcha = () => {
  const a = Math.floor(Math.random() * 9) + 1
  const b = Math.floor(Math.random() * 9) + 1
  captchaAnswer.value = a + b
  captchaQuestion.value = `${a} + ${b} = ?`
  loginForm.captcha = ''
}

const updateClock = () => {
  const now = new Date()
  clockDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
  clockTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
}

const getRecentUsernames = () => {
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

const saveRecentUsername = (username) => {
  const name = username?.trim()
  if (!name) return
  const list = getRecentUsernames().filter(item => item !== name)
  list.unshift(name)
  localStorage.setItem(RECENT_KEY, JSON.stringify(list.slice(0, MAX_RECENT)))
}

const queryRecentUsers = (queryString, cb) => {
  const recent = getRecentUsernames()
  const results = recent
    .filter(name => !queryString || name.toLowerCase().includes(queryString.toLowerCase()))
    .map(name => ({ value: name }))
  cb(results)
}

const handleSelectRecentUser = (item) => {
  loginForm.username = item.value
}

const loadSavedUsername = () => {
  rememberUsername.value = localStorage.getItem(REMEMBER_KEY) === 'true'
  if (rememberUsername.value) {
    loginForm.username = localStorage.getItem(USERNAME_KEY) || ''
  }
}

const persistUsernamePreference = () => {
  localStorage.setItem(REMEMBER_KEY, String(rememberUsername.value))
  if (rememberUsername.value) {
    localStorage.setItem(USERNAME_KEY, loginForm.username.trim())
  } else {
    localStorage.removeItem(USERNAME_KEY)
  }
}

const loadRegisterRoles = async () => {
  try {
    const res = await request({ url: '/user/register-roles', method: 'get' })
    registerRoles.value = res.data || []
  } catch {}
}

const switchToRegister = () => {
  isRegister.value = true
  if (!registerRoles.value.length) loadRegisterRoles()
}

const switchToLogin = () => {
  isRegister.value = false
  refreshCaptcha()
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({
        username: loginForm.username,
        password: loginForm.password
      })
      persistUsernamePreference()
      saveRecentUsername(loginForm.username)
      ElMessage.success('登录成功')
      router.push('/')
    } catch {
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await request({
        url: '/user/register',
        method: 'post',
        data: {
          username: registerForm.username,
          password: registerForm.password,
          realName: registerForm.realName,
          phone: registerForm.phone || undefined,
          email: registerForm.email || undefined,
          roleId: registerForm.roleId
        }
      })
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      loginForm.username = registerForm.username
      loginForm.password = ''
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  loadSavedUsername()
  refreshCaptcha()
  loadRegisterRoles()
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: -24px;
  z-index: 0;
  overflow: hidden;
}

.login-bg__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  filter: blur(14px);
  transform: scale(1.06);
}

.login-bg-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(
    135deg,
    rgba(15, 28, 46, 0.72) 0%,
    rgba(26, 51, 82, 0.55) 45%,
    rgba(37, 99, 168, 0.35) 100%
  );
}

.login-shell {
  position: relative;
  z-index: 2;
  display: flex;
  width: 100%;
  max-width: 920px;
  min-height: 520px;
  border-radius: 20px;
  overflow: hidden;
  background: #fff;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.12),
    0 24px 60px -12px rgba(0, 0, 0, 0.35),
    0 12px 32px -8px rgba(37, 99, 168, 0.2);
}

.login-shell--register {
  max-width: 960px;
  min-height: 600px;
}

/* 左侧品牌 */
.shell-brand {
  flex: 0 0 42%;
  background: linear-gradient(168deg, #1a3352 0%, #234a72 55%, #2a5a8a 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
}

.shell-brand::before {
  content: '';
  position: absolute;
  width: 280px;
  height: 280px;
  top: -100px;
  right: -80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}

.shell-brand::after {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  bottom: -60px;
  left: -40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.08);
}

.brand-content {
  position: relative;
  z-index: 1;
  height: 100%;
  padding: 44px 40px;
  display: flex;
  flex-direction: column;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.brand-logo__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #fff;
  color: #2563a8;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-logo__text {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.55);
}

.brand-hero h1 {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  margin: 0 0 10px;
  letter-spacing: -0.02em;
}

.brand-hero p {
  margin: 0;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.65);
  letter-spacing: 0.02em;
}

.brand-features {
  list-style: none;
  margin: 36px 0 0;
  padding: 0;
}

.brand-features li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-features li:last-child {
  border-bottom: none;
}

.brand-features .el-icon {
  font-size: 16px;
  color: #7ec8a0;
}

.brand-clock {
  margin-top: auto;
  padding-top: 28px;
}

.brand-clock__date {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.brand-clock__time {
  font-size: 32px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
  line-height: 1;
}

/* 右侧表单 */
.shell-form {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 44px;
  background: #fff;
}

.form-wrap {
  width: 100%;
  max-width: 360px;
}

.form-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 32px;
  border-bottom: 1px solid var(--border-light);
}

.form-tab {
  flex: 1;
  padding: 0 0 14px;
  margin-bottom: -1px;
  border: none;
  background: none;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-tertiary);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.form-tab:hover {
  color: var(--text-secondary);
}

.form-tab.active {
  color: var(--primary);
  font-weight: 600;
}

.form-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background: var(--primary);
  border-radius: 2px 2px 0 0;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.auth-form :deep(.el-autocomplete) {
  width: 100%;
}

.auth-form--register {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-btn {
  flex-shrink: 0;
  width: 108px;
  height: 40px;
  border: none;
  border-radius: var(--radius-md);
  background: #f0f4f8;
  color: var(--primary-dark);
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  transition: background 0.15s;
}

.captcha-btn:hover {
  background: var(--primary-lighter);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.text-link {
  font-size: 13px;
  color: var(--primary);
  cursor: pointer;
}

.text-link:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.12em;
  border-radius: var(--radius-md);
}

.register-note {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.6;
  text-align: center;
}

.forgot-body p {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.forgot-body ul {
  margin: 0 0 12px;
  padding-left: 20px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.forgot-tip {
  font-size: 12px !important;
  color: var(--text-tertiary) !important;
}

@media (max-width: 768px) {
  .login-page {
    padding: 16px;
    align-items: flex-start;
    padding-top: 24px;
  }

  .login-shell {
    flex-direction: column;
    max-width: 440px;
    min-height: auto;
  }

  .shell-brand {
    flex: none;
  }

  .brand-content {
    padding: 28px 24px;
  }

  .brand-features {
    display: none;
  }

  .brand-clock {
    padding-top: 16px;
  }

  .brand-clock__time {
    font-size: 24px;
  }

  .shell-form {
    padding: 28px 24px 32px;
  }
}
</style>
