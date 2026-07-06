<template>
  <div class="dashboard page-shell">
    <div class="page-header">
      <h2>工作台</h2>
      <p>{{ greeting }}，{{ userStore.realName || '用户' }}</p>
    </div>
    
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-grid">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--blue">
              <el-icon><Aim /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.drugCount }}</div>
              <div class="stat-item__label">药品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--red">
              <el-icon><Warning /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.warningCount }}</div>
              <div class="stat-item__label">库存预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--amber">
              <el-icon><ShoppingCart /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.purchaseCount }}</div>
              <div class="stat-item__label">待审核采购单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--green">
              <el-icon><Box /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.stockValue }}</div>
              <div class="stat-item__label">库存总金额（元）</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ECharts 图表区域 -->
    <el-row :gutter="16" class="chart-cards">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>药品分类统计</span></template>
          <div ref="drugTypeChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>库存预警概览</span></template>
          <div ref="stockWarningChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近效期药品预警 -->
    <el-card class="near-expiry-card" shadow="never" v-if="nearExpiryDrugs.length > 0">
      <template #header>
        <div class="card-header">
          <span>近效期药品预警</span>
          <el-tag type="danger" size="small">{{ nearExpiryDrugs.length }} 种药品即将过期</el-tag>
        </div>
      </template>
      <el-table :data="nearExpiryDrugs" size="small" stripe>
        <el-table-column prop="drugName" label="药品名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column prop="batchNo" label="批号" />
        <el-table-column prop="stockNum" label="库存数量" width="100" />
        <el-table-column prop="expiryDate" label="有效期至" width="120" />
        <el-table-column prop="remainingDays" label="剩余天数" width="100">
          <template #default="{ row }">
            <el-tag :type="row.remainingDays <= 30 ? 'danger' : 'warning'" size="small">
              {{ row.remainingDays }} 天
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 公告信息 -->
    <el-card class="notice-card" shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>系统公告</span>
          <el-button v-if="isAdmin" type="primary" size="small" @click="handleAddNotice">
            <el-icon><Plus /></el-icon>
            新建公告
          </el-button>
        </div>
      </template>
      <el-timeline v-if="noticeList.length > 0">
        <el-timeline-item 
          v-for="notice in noticeList" 
          :key="notice.noticeId" 
          :timestamp="formatDate(notice.createTime)" 
          placement="top"
        >
          <div class="notice-item">
            <div class="notice-content">
              <h4>{{ notice.title }}</h4>
              <p>{{ notice.content }}</p>
            </div>
            <div v-if="isAdmin" class="notice-actions">
              <el-button type="primary" size="small" link @click="handleEditNotice(notice)">编辑</el-button>
              <el-button type="danger" size="small" link @click="handleDeleteNotice(notice)">删除</el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无公告" />
    </el-card>

    <!-- 公告表单对话框 -->
    <el-dialog v-model="noticeDialogVisible" :title="noticeDialogTitle" width="600px">
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="noticeRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="noticeForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="noticeForm.content" type="textarea" :rows="5" placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNotice" :loading="noticeSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardStats, getDrugTypeStats, getStockWarningStats } from '@/api/dashboard'
import { useUserStore } from '@/store/user'
import { getNoticeList, saveNotice, updateNotice, deleteNotice } from '@/api/notice'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const isAdmin = computed(() => {
  const roles = userStore.userRoles || []
  return roles.includes('ADMIN') || roles.includes('系统管理员')
})

const stats = reactive({
  drugCount: 0,
  warningCount: 0,
  purchaseCount: 0,
  stockValue: 0
})

const noticeList = ref([])

const nearExpiryDrugs = ref([])

const drugTypeChartRef = ref(null)
const stockWarningChartRef = ref(null)
let drugTypeChart = null
let stockWarningChart = null

const noticeDialogVisible = ref(false)
const noticeDialogTitle = ref('新建公告')
const noticeSubmitting = ref(false)
const noticeFormRef = ref(null)
const noticeForm = reactive({
  noticeId: null,
  title: '',
  content: ''
})
const noticeRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const loadStats = async () => {
  try {
    const res = await getDashboardStats()
    if (res.data) {
      stats.drugCount = res.data.drugCount || 0
      stats.warningCount = res.data.warningCount || 0
      stats.purchaseCount = res.data.purchaseCount || 0
      stats.stockValue = res.data.stockValue || 0
      if (res.data.nearExpiryDrugs) {
        nearExpiryDrugs.value = res.data.nearExpiryDrugs
      }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const loadNoticeList = async () => {
  loading.value = true
  try {
    const res = await getNoticeList()
    if (res.data) {
      noticeList.value = res.data
    }
  } catch (error) {
    console.error('加载公告列表失败:', error)
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const handleAddNotice = () => {
  noticeDialogTitle.value = '新建公告'
  noticeForm.noticeId = null
  noticeForm.title = ''
  noticeForm.content = ''
  noticeDialogVisible.value = true
  nextTick(() => {
    noticeFormRef.value?.clearValidate()
  })
}

const handleEditNotice = (notice) => {
  noticeDialogTitle.value = '编辑公告'
  noticeForm.noticeId = notice.noticeId
  noticeForm.title = notice.title
  noticeForm.content = notice.content
  noticeDialogVisible.value = true
  nextTick(() => {
    noticeFormRef.value?.clearValidate()
  })
}

const submitNotice = async () => {
  if (!noticeFormRef.value) return
  await noticeFormRef.value.validate()
  noticeSubmitting.value = true
  try {
    if (noticeForm.noticeId) {
      await updateNotice({
        noticeId: noticeForm.noticeId,
        title: noticeForm.title,
        content: noticeForm.content
      })
      ElMessage.success('公告编辑成功')
    } else {
      await saveNotice({
        title: noticeForm.title,
        content: noticeForm.content,
        createUserId: userStore.userInfo.userId,
        createUserName: userStore.userInfo.realName || userStore.userInfo.username
      })
      ElMessage.success('公告创建成功')
    }
    noticeDialogVisible.value = false
    loadNoticeList()
  } catch (error) {
    console.error('操作公告失败:', error)
    ElMessage.error(error.response?.data?.msg || '操作失败')
  } finally {
    noticeSubmitting.value = false
  }
}

const handleDeleteNotice = (notice) => {
  ElMessageBox.confirm('确定要删除这条公告吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteNotice(notice.noticeId)
      ElMessage.success('删除成功')
      loadNoticeList()
    } catch (error) {
      console.error('删除公告失败:', error)
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  }).catch(() => {})
}

const CHART_COLORS = ['#2563A8', '#C0392B', '#B8860B', '#2E7D5A', '#4A6FA5', '#C45C26']

const initDrugTypeChart = async () => {
  try {
    const res = await getDrugTypeStats()
    const data = (res.data && res.data.typeStats) || []
    if (drugTypeChartRef.value) {
      drugTypeChart = echarts.init(drugTypeChartRef.value)
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center'
        },
        series: [
          {
            name: '药品分类',
            type: 'pie',
            radius: ['42%', '68%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderRadius: 4,
              borderColor: '#fff',
              borderWidth: 1
            },
            label: {
              show: true,
              formatter: '{b}\n{d}%',
              fontSize: 12
            },
            emphasis: {
              scale: true,
              scaleSize: 4
            },
            data: data.map((item, i) => ({
              name: item.name,
              value: item.value,
              itemStyle: { color: CHART_COLORS[i % CHART_COLORS.length] }
            }))
          }
        ]
      }
      drugTypeChart.setOption(option)
    }
  } catch (error) {
    if (drugTypeChartRef.value) {
      drugTypeChart = echarts.init(drugTypeChartRef.value)
      drugTypeChart.setOption({
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'center',
          textStyle: { color: '#aaa', fontSize: 14 }
        }
      })
    }
  }
}

const initStockWarningChart = async () => {
  try {
    const res = await getStockWarningStats()
    const data = res.data || {}
    if (stockWarningChartRef.value) {
      stockWarningChart = echarts.init(stockWarningChartRef.value)
      const categories = data.categories || ['库存不足', '库存过剩', '近效期', '已过期']
      const values = data.values || [0, 0, 0, 0]
      const barColors = ['#C0392B', '#C0392B', '#B8860B', '#4A6FA5']
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: categories,
          axisLabel: { fontSize: 12, color: '#4A5568' },
          axisLine: { lineStyle: { color: '#D8DEE6' } }
        },
        yAxis: {
          type: 'value',
          allowDecimals: false,
          splitLine: { lineStyle: { type: 'dashed', color: '#E8ECF1' } },
          axisLabel: { color: '#8A94A6' }
        },
        series: [
          {
            name: '数量',
            type: 'bar',
            barWidth: '48%',
            data: values.map((v, i) => ({
              value: v,
              itemStyle: {
                color: barColors[i] || '#2563A8',
                borderRadius: [3, 3, 0, 0]
              }
            }))
          }
        ]
      }
      stockWarningChart.setOption(option)
    }
  } catch (error) {
    console.error('初始化库存预警图表失败:', error)
    if (stockWarningChartRef.value) {
      stockWarningChart = echarts.init(stockWarningChartRef.value)
      stockWarningChart.setOption({
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'center',
          textStyle: { color: '#aaa', fontSize: 14 }
        }
      })
    }
  }
}

const handleResize = () => {
  drugTypeChart?.resize()
  stockWarningChart?.resize()
}

onMounted(async () => {
  loadStats()
  loadNoticeList()
  await nextTick()
  initDrugTypeChart()
  initStockWarningChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  drugTypeChart?.dispose()
  stockWarningChart?.dispose()
})
</script>

<style scoped>
.chart-cards {
  margin-bottom: 20px;
}

.near-expiry-card,
.notice-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-item {
  padding: 12px 0 4px;
  border-bottom: 1px solid var(--border-light);
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-content h4 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
}

.notice-content p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.notice-actions {
  margin-top: 8px;
  display: flex;
  gap: 4px;
}
</style>
