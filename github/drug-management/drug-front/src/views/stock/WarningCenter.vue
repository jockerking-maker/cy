<template>
  <div class="warning-center page-shell">
    <div class="page-header">
      <h2>预警中心</h2>
      <p>库存预警、临期与滞销监控</p>
    </div>

    <el-row :gutter="16" class="stat-grid">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card-wrapper" @click="filterByStatus(0)">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--red">
              <el-icon><Warning /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.unhandled }}</div>
              <div class="stat-item__label">未处理预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--amber">
              <el-icon><Bottom /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.lowStock }}</div>
              <div class="stat-item__label">低库存预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--red">
              <el-icon><Timer /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.nearExpiry }}</div>
              <div class="stat-item__label">临期预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-item">
            <div class="stat-item__icon stat-item__icon--gray">
              <el-icon><Box /></el-icon>
            </div>
            <div>
              <div class="stat-item__value">{{ stats.overstock }}</div>
              <div class="stat-item__label">库存积压预警</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header><span>预警趋势与分布</span></template>
      <el-row :gutter="20">
        <el-col :span="16">
          <div class="chart-sub-title">近6个月预警趋势</div>
          <div ref="trendChartRef" style="height: 320px"></div>
          <div class="chart-note">临期/过期趋势结合库存有效期统计，其余类型按预警记录生成时间统计</div>
        </el-col>
        <el-col :span="8">
          <div class="chart-sub-title">预警类型分布</div>
          <div ref="pieChartRef" style="height: 320px"></div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-form :model="searchForm" inline>
        <el-form-item label="预警类型">
          <el-select v-model="searchForm.warningType" placeholder="全部" clearable style="width: 140px">
            <el-option label="低库存" :value="0" />
            <el-option label="库存积压" :value="1" />
            <el-option label="临期" :value="2" />
            <el-option label="过期" :value="3" />
            <el-option label="滞销" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警级别">
          <el-select v-model="searchForm.warningLevel" placeholder="全部" clearable style="width: 120px">
            <el-option label="一般" :value="0" />
            <el-option label="重要" :value="1" />
            <el-option label="紧急" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.handleStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchForm.warehouseId" placeholder="请选择仓库" clearable style="width: 150px">
            <el-option
              v-for="item in warehouseList"
              :key="item.warehouseId"
              :label="item.warehouseName"
              :value="item.warehouseId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button
          type="success"
          :disabled="unhandledSelectedCount === 0"
          @click="openBatchHandleDialog"
        >
          <el-icon><Check /></el-icon>
          批量处理{{ unhandledSelectedCount > 0 ? ` (${unhandledSelectedCount})` : '' }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="handledSelectedCount === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除{{ handledSelectedCount > 0 ? ` (${handledSelectedCount})` : '' }}
        </el-button>
        <el-button type="warning" @click="handleCheckNearExpiry">
          <el-icon><Timer /></el-icon>
          检查临期药品
        </el-button>
        <el-button type="info" @click="handleCheckSlowMoving">
          <el-icon><TrendCharts /></el-icon>
          检查滞销药品
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="warningNo" label="预警单号" width="160" />
        <el-table-column prop="drugCode" label="药品编码" width="120" />
        <el-table-column prop="drugName" label="药品名称" width="150" />
        <el-table-column prop="batchNo" label="批号" width="110" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column label="预警类型" width="100">
          <template #default="{ row }">
            <el-tag :type="warningTypeTag[row.warningType]">{{ warningTypeName[row.warningType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预警级别" width="90">
          <template #default="{ row }">
            <el-tag :type="warningLevelTag[row.warningLevel]">{{ warningLevelName[row.warningLevel] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stockNum" label="当前库存" width="90" />
        <el-table-column label="预警值" width="120">
          <template #default="{ row }">
            <span v-if="row.warningType === 0 || row.warningType === 2 || row.warningType === 3 || row.warningType === 4">
              最低 {{ row.minWarningNum ?? '-' }}
            </span>
            <span v-else-if="row.warningType === 1">
              最高 {{ row.maxWarningNum ?? '-' }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.handleStatus === 1 ? 'success' : 'info'">
              {{ row.handleStatus === 1 ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="处理建议" min-width="150" show-overflow-tooltip />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ').substring(0, 16) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.handleStatus === 0" type="primary" size="small" @click="openHandleDialog(row)">处理</el-button>
            <el-button v-if="row.handleStatus === 1" type="danger" size="small" @click="handleDeleteWarning(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        :hide-on-single-page="false"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination-wrap"
      />
    </el-card>

    <el-dialog v-model="handleDialogVisible" title="处理预警" width="520px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
        title="处理预警 = 确认已跟进"
        description="此操作仅将预警标记为「已处理」，不会自动下架、报损或扣减库存。过期/临期药品请至「出库管理」办理实际处置。"
      />
      <el-descriptions :column="1" border style="margin-bottom: 16px">
        <el-descriptions-item label="药品名称">{{ currentWarning.drugName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentWarning.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="预警类型">
          <el-tag :type="warningTypeTag[currentWarning.warningType]">{{ warningTypeName[currentWarning.warningType] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警级别">
          <el-tag :type="warningLevelTag[currentWarning.warningLevel]">{{ warningLevelName[currentWarning.warningLevel] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理建议">{{ currentWarning.suggestion }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="处置说明" required>
          <el-input
            v-model="handleRemark"
            type="textarea"
            :rows="3"
            :placeholder="handleRemarkPlaceholder"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchHandleDialogVisible" title="批量处理预警" width="500px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
        :title="`将处理 ${unhandledSelectedCount} 条未处理预警`"
        description="已处理的记录不可再次批量处理。临期/过期/滞销类预警需填写处置说明；若低库存/库存积压问题仍未解决，也需填写处理说明。"
      />
      <el-form label-width="80px">
        <el-form-item label="处置说明">
          <el-input v-model="batchHandleRemark" type="textarea" :rows="3" placeholder="临期/过期/滞销预警必填，如：已下架、已报损出库、已优先出库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchHandleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchHandle">确认处理</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  getStockWarningList,
  handleWarning,
  deleteWarning,
  getWarningStats,
  getWarningTrend,
  batchHandleWarnings,
  batchDeleteWarnings,
  checkNearExpiry,
  checkSlowMoving
} from '@/api/stockWarning'
import { getWarehouseList } from '@/api/warehouse'

const loading = ref(false)
const handleDialogVisible = ref(false)
const batchHandleDialogVisible = ref(false)
const handleRemark = ref('')
const batchHandleRemark = ref('')
const currentWarning = ref({})
const selectedRows = ref([])
const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null

const warningTypeName = { 0: '低库存', 1: '库存积压', 2: '临期', 3: '过期', 4: '滞销' }
const warningTypeTag = { 0: 'danger', 1: 'warning', 2: 'warning', 3: 'danger', 4: 'info' }
const warningLevelName = { 0: '一般', 1: '重要', 2: '紧急' }
const warningLevelTag = { 0: 'info', 1: 'warning', 2: 'danger' }

const stats = reactive({
  unhandled: 0,
  lowStock: 0,
  nearExpiry: 0,
  overstock: 0
})

const searchForm = reactive({
  warningType: null,
  warningLevel: null,
  handleStatus: null,
  warehouseId: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 50,
  total: 0
})

const tableData = ref([])
const warehouseList = ref([])

const unhandledSelectedCount = computed(() =>
  selectedRows.value.filter(row => row.handleStatus === 0).length
)

const handledSelectedCount = computed(() =>
  selectedRows.value.filter(row => row.handleStatus === 1).length
)

const handleRemarkPlaceholder = computed(() => {
  const type = currentWarning.value?.warningType
  if (type === 3) return '必填，如：已下架、已报损出库'
  if (type === 2) return '必填，如：已优先出库、已调拨'
  if (type === 4) return '必填，如：已促销出库、暂停采购'
  return '请填写处理说明（部分预警类型必填）'
})

const requiresHandleRemark = (warningType) => [2, 3, 4].includes(warningType)

const loadStats = async () => {
  try {
    const res = await getWarningStats()
    if (res.data) {
      stats.unhandled = res.data.unhandled || 0
      stats.lowStock = res.data.lowStock || 0
      stats.nearExpiry = res.data.nearExpiry || 0
      stats.overstock = res.data.overstock || 0
    }
  } catch {
    ElMessage.error('加载预警统计失败')
  }
}

const initTrendChart = async () => {
  try {
    const [trendRes, statsRes] = await Promise.all([getWarningTrend(6), getWarningStats()])
    const data = trendRes.data || {}
    const statsData = statsRes.data || {}

    if (trendChartRef.value) {
      trendChart?.dispose()
      trendChart = echarts.init(trendChartRef.value)
      const months = data.months || []
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } }
        },
        legend: {
          data: ['低库存', '库存积压', '临期', '过期', '滞销'],
          top: 0
        },
        grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true },
        xAxis: {
          type: 'category',
          data: months,
          boundaryGap: false,
          axisLine: { lineStyle: { color: '#D8DEE6' } },
          axisLabel: { color: '#4A5568' }
        },
        yAxis: {
          type: 'value',
          allowDecimals: false,
          splitLine: { lineStyle: { type: 'dashed', color: '#E8ECF1' } },
          axisLabel: { color: '#8A94A6' }
        },
        series: [
          {
            name: '低库存', type: 'line', data: data.lowStock || [], smooth: false,
            symbol: 'circle', symbolSize: 6,
            itemStyle: { color: '#C0392B' },
            lineStyle: { width: 2 }
          },
          {
            name: '库存积压', type: 'line', data: data.overstock || [], smooth: false,
            symbol: 'circle', symbolSize: 6,
            itemStyle: { color: '#B8860B' },
            lineStyle: { width: 2 }
          },
          {
            name: '临期', type: 'line', data: data.nearExpiry || [], smooth: false,
            symbol: 'circle', symbolSize: 6,
            itemStyle: { color: '#C45C26' },
            lineStyle: { width: 2, type: 'dashed' }
          },
          {
            name: '过期', type: 'line', data: data.expired || [], smooth: false,
            symbol: 'circle', symbolSize: 6,
            itemStyle: { color: '#8A2942' },
            lineStyle: { width: 2, type: 'dashed' }
          },
          {
            name: '滞销', type: 'line', data: data.slowMoving || [], smooth: false,
            symbol: 'circle', symbolSize: 6,
            itemStyle: { color: '#4A6FA5' },
            lineStyle: { width: 2 }
          }
        ]
      }
      trendChart.setOption(option)
    }

    if (pieChartRef.value) {
      pieChart?.dispose()
      pieChart = echarts.init(pieChartRef.value)
      const lowStock = Number(statsData.lowStock || 0)
      const overstock = Number(statsData.overstock || 0)
      const nearExpiry = Number(statsData.nearExpiry || 0)
      const slowMoving = Number(statsData.slowMoving || 0)
      const pieData = [
        { value: lowStock, name: '低库存', itemStyle: { color: '#C0392B' } },
        { value: overstock, name: '库存积压', itemStyle: { color: '#B8860B' } },
        { value: nearExpiry, name: '临期/过期', itemStyle: { color: '#C45C26' } },
        { value: slowMoving, name: '滞销', itemStyle: { color: '#4A6FA5' } }
      ].filter(d => d.value > 0)

      if (pieData.length === 0) {
        pieData.push({ value: 1, name: '暂无预警', itemStyle: { color: '#D8DEE6' } })
      }

      pieChart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}条 ({d}%)'
        },
        legend: {
          orient: 'horizontal',
          bottom: 0,
          itemWidth: 10,
          itemHeight: 10,
          textStyle: { fontSize: 12, color: '#4A5568' }
        },
        series: [{
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 4,
            borderColor: '#fff',
            borderWidth: 1
          },
          label: {
            show: true,
            formatter: '{b}\n{c}条',
            fontSize: 12,
            color: '#4A5568'
          },
          emphasis: {
            scale: true,
            scaleSize: 4
          },
          data: pieData
        }]
      })
    }
  } catch {
    ElMessage.error('加载预警图表失败')
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStockWarningList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...searchForm
    })
    tableData.value = res.data || []
    pagination.total = Number(res.total) || 0
  } catch {
    ElMessage.error('加载预警列表失败')
  } finally {
    loading.value = false
  }
}

const loadWarehouseList = async () => {
  try {
    const res = await getWarehouseList()
    warehouseList.value = res.data || []
  } catch {
    ElMessage.error('加载仓库列表失败')
  }
}

const filterByStatus = (status) => {
  searchForm.handleStatus = status
  pagination.currentPage = 1
  loadData()
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  searchForm.warningType = null
  searchForm.warningLevel = null
  searchForm.handleStatus = null
  searchForm.warehouseId = null
  handleSearch()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const openHandleDialog = (row) => {
  currentWarning.value = row
  handleRemark.value = ''
  handleDialogVisible.value = true
}

const submitHandle = async () => {
  const warningType = currentWarning.value?.warningType
  if (requiresHandleRemark(warningType) && !handleRemark.value.trim()) {
    ElMessage.warning('请填写处置说明')
    return
  }
  try {
    await handleWarning(currentWarning.value.warningId, {
      handleRemark: handleRemark.value
    })
    ElMessage.success('预警已标记为已处理；请在出库管理中完成实际下架/报损')
    handleDialogVisible.value = false
    loadData()
    loadStats()
    initTrendChart()
  } catch {
    // 错误信息由 request 拦截器统一提示
  }
}

const handleDeleteWarning = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条预警记录吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWarning(row.warningId)
    ElMessage.success('删除成功')
    loadData()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const openBatchHandleDialog = () => {
  if (unhandledSelectedCount.value === 0) {
    ElMessage.warning('请勾选未处理的预警记录')
    return
  }
  batchHandleRemark.value = ''
  batchHandleDialogVisible.value = true
}

const submitBatchHandle = async () => {
  const unhandledRows = selectedRows.value.filter(row => row.handleStatus === 0)
  if (unhandledRows.length === 0) {
    ElMessage.warning('请勾选未处理的预警记录')
    return
  }
  const needRemark = unhandledRows.some(row => requiresHandleRemark(row.warningType))
  if (needRemark && !batchHandleRemark.value.trim()) {
    ElMessage.warning('所选预警包含临期/过期/滞销类型，请填写处置说明')
    return
  }
  try {
    const res = await batchHandleWarnings({
      warningIds: unhandledRows.map(row => row.warningId),
      handleRemark: batchHandleRemark.value
    })
    ElMessage.success(res.msg || '批量处理成功')
    batchHandleDialogVisible.value = false
    selectedRows.value = []
    loadData()
    loadStats()
    initTrendChart()
  } catch {
    // 错误信息由 request 拦截器统一提示
  }
}

const handleBatchDelete = async () => {
  const handledRows = selectedRows.value.filter(row => row.handleStatus === 1)
  if (handledRows.length === 0) {
    ElMessage.warning('请勾选已处理的预警记录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${handledRows.length} 条已处理预警吗？删除后不可恢复。`,
      '批量删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const res = await batchDeleteWarnings({
      warningIds: handledRows.map(row => row.warningId)
    })
    ElMessage.success(res.msg || '批量删除成功')
    selectedRows.value = []
    loadData()
    loadStats()
    initTrendChart()
  } catch (error) {
    if (error !== 'cancel') {
      // 错误信息由 request 拦截器统一提示
    }
  }
}

const handleCheckNearExpiry = async () => {
  try {
    const res = await checkNearExpiry()
    ElMessage.success(res.msg || '临期药品检查完成')
    loadData()
    loadStats()
    initTrendChart()
  } catch (error) {
    ElMessage.error('检查失败')
  }
}

const handleCheckSlowMoving = async () => {
  try {
    const res = await checkSlowMoving()
    ElMessage.success(res.msg || '滞销药品检查完成')
    loadData()
    loadStats()
    initTrendChart()
  } catch (error) {
    ElMessage.error('检查失败')
  }
}

const handleSizeChange = () => {
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  loadStats()
  loadWarehouseList()
  loadData()
  await nextTick()
  initTrendChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.stat-card-wrapper {
  cursor: pointer;
}

.chart-card {
  margin-top: 16px;
}

.chart-sub-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.chart-note {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>
