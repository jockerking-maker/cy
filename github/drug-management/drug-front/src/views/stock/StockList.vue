<template>
  <div class="stock-list page-shell">
    <div class="page-header">
      <h2>库存管理</h2>
      <p>按「药品 + 仓库」展示库存；库存不足可在操作菜单或批次明细中「补货」，追加至现有批次</p>
    </div>
    <el-card shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="药品名称">
          <el-input v-model="searchForm.drugName" placeholder="请输入药品名称" clearable />
        </el-form-item>
        <el-form-item label="药品编码">
          <el-input v-model="searchForm.drugCode" placeholder="请输入药品编码" clearable />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchForm.warehouseId" placeholder="请选择仓库" clearable style="width: 150px;">
            <el-option
              v-for="item in warehouseList"
              :key="item.warehouseId"
              :label="item.status === 0 ? `${item.warehouseName}（已禁用）` : item.warehouseName"
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
          <el-button type="warning" @click="toggleWarningOnly">
            <el-icon><Warning /></el-icon>
            {{ showWarningOnly ? '显示全部' : '只看预警' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="danger" @click="router.push('/warning-center')">
          <el-icon><Warning /></el-icon>
          预警处理
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        class="stock-table"
        style="width: 100%"
        :row-class-name="tableRowClassName"
        row-key="rowKey"
        :default-sort="{ prop: 'drugCode', order: 'ascending' }"
        :span-method="spanMethod"
        @sort-change="handleSortChange"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <div class="expand-title">批次明细（共 {{ row.batchCount }} 个批次）</div>
              <el-table :data="row.batchDetails" border size="small" style="width: 90%; margin: 0 auto;">
                <el-table-column prop="batchNo" label="批次号" width="150" />
                <el-table-column prop="productionDate" label="生产日期" width="160">
                  <template #default="{ row: batch }">
                    {{ batch.productionDate ? batch.productionDate.split('T')[0] : '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="expiryDate" label="有效期至" width="160">
                  <template #default="{ row: batch }">
                    {{ batch.expiryDate ? batch.expiryDate.split('T')[0] : '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="stockNum" label="库存数量" width="100" align="right" />
                <el-table-column prop="lockNum" label="锁定数量" width="100" align="right" />
                <el-table-column prop="availableNum" label="可用数量" width="100" align="right">
                  <template #default="{ row: batch }">
                    <span :class="{ 'text-danger': batch.availableNum <= 0 }">{{ batch.availableNum }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="效期状态" width="140">
                  <template #default="{ row: batch }">
                    <el-tag v-if="isExpired(batch.expiryDate)" type="danger" size="small">已过期</el-tag>
                    <el-tag v-else-if="isNearExpiry(batch.expiryDate)" type="warning" size="small">近效期</el-tag>
                    <el-tag v-else type="success" size="small">正常</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140">
                  <template #default="{ row: batch }">
                    <el-button
                      v-if="isLowStock(row) && !isExpired(batch.expiryDate)"
                      link
                      type="primary"
                      size="small"
                      @click="openReplenish(row, batch)"
                    >
                      补货
                    </el-button>
                    <el-button link type="primary" size="small" @click="handleLockBatch(row, batch)">锁定</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="drugCode" label="药品编码" width="120" sortable="custom" />
        <el-table-column prop="drugName" label="药品名称" width="200" sortable="custom" />
        <el-table-column prop="spec" label="规格" width="150" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="warehouseName" label="仓库" width="180" sortable="custom">
          <template #default="{ row }">
            <span>{{ row.warehouseName }}</span>
            <el-tag v-if="row.warehouseStatus === 0" type="danger" size="small" style="margin-left: 6px">已禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchCount" label="批次数" width="90" align="center" sortable="custom">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.batchCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalStockNum" label="总库存" width="100" align="right" sortable="custom" />
        <el-table-column prop="totalLockNum" label="总锁定" width="100" align="right" />
        <el-table-column prop="availableNum" label="可用库存" width="100" align="right" sortable="custom">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.availableNum <= 0 }">{{ row.availableNum }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warningNum" label="最低预警" width="90" align="right" />
        <el-table-column prop="maxWarningNum" label="最高预警" width="90" align="right" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.availableNum <= row.warningNum" type="danger">库存不足</el-tag>
            <el-tag v-else-if="row.maxWarningNum && row.totalStockNum >= row.maxWarningNum" type="warning">库存积压</el-tag>
            <el-tag v-else type="success">充足</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="88" fixed="right" align="center">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="(cmd) => handleRowCommand(cmd, row)">
              <el-button link type="primary">
                操作
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="isLowStock(row)" command="replenish">批次补货</el-dropdown-item>
                  <el-dropdown-item command="warning">预警设置</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <el-dialog
      v-model="replenishVisible"
      title="批次补货"
      width="520px"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
        title="追加至现有批次"
        description="沿用所选批次的批号、生产日期和效期，只需填写补货数量和单价。"
      />
      <el-form label-width="100px">
        <el-form-item label="药品">
          <span>{{ replenishForm.drugName }}</span>
        </el-form-item>
        <el-form-item label="仓库">
          <span>{{ replenishForm.warehouseName }}</span>
        </el-form-item>
        <el-form-item label="选择批次" required>
          <el-select
            v-model="replenishForm.stockId"
            placeholder="请选择批次"
            style="width: 100%"
            @change="onReplenishBatchChange"
          >
            <el-option
              v-for="item in replenishForm.batchOptions"
              :key="item.stockId"
              :label="`${item.batchNo}（库存 ${item.stockNum}，效期 ${formatStockDate(item.expiryDate)}）`"
              :value="item.stockId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生产日期">
          <span>{{ replenishForm.productionDate }}</span>
        </el-form-item>
        <el-form-item label="有效期至">
          <span>{{ replenishForm.expiryDate }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ replenishForm.currentStock }}</span>
        </el-form-item>
        <el-form-item label="补货数量" required>
          <el-input-number v-model="replenishForm.quantity" :min="1" style="width: 160px" />
          <div class="form-tip">
            当前可用 {{ replenishForm.availableNum }}，最低预警 {{ replenishForm.warningNum }}；
            建议补货 {{ replenishForm.suggestQty }}（需大于最低预警才解除预警）
          </div>
        </el-form-item>
        <el-form-item label="入库单价" required>
          <el-input-number v-model="replenishForm.purchasePrice" :min="0.01" :precision="2" style="width: 160px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replenishVisible = false">取消</el-button>
        <el-button type="primary" :loading="replenishSubmitLoading" @click="submitReplenish">确认补货</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="lockVisible"
      title="药品锁定"
      width="600px"
    >
      <el-form :model="lockForm" label-width="100px">
        <el-form-item label="药品名称">
          <span>{{ lockForm.drugName }}</span>
        </el-form-item>
        <el-form-item label="仓库">
          <span>{{ lockForm.warehouseName }}</span>
        </el-form-item>
        <el-form-item label="批次号">
          <span>{{ lockForm.batchNo }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ lockForm.stockNum }}</span>
        </el-form-item>
        <el-form-item label="已锁定数量">
          <span>{{ lockForm.currentLockNum || 0 }}</span>
        </el-form-item>
        <el-form-item label="可用库存">
          <span>{{ lockForm.stockNum - (lockForm.currentLockNum || 0) }}</span>
        </el-form-item>
        <el-form-item label="目标锁定数量" prop="lockNum">
          <el-input-number v-model="lockForm.lockNum" :min="0" :max="lockForm.stockNum" placeholder="请输入要设定的锁定数量" />
        </el-form-item>
        <el-form-item label="锁定原因">
          <el-input v-model="lockForm.lockReason" type="textarea" rows="3" placeholder="请输入锁定原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lockVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLock">确定锁定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="warningSetVisible"
      title="预警值设置"
      width="600px"
    >
      <el-form :model="warningSetForm" label-width="120px">
        <el-form-item label="药品名称">
          <span>{{ warningSetForm.drugName }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ warningSetForm.stockNum }}</span>
        </el-form-item>
        <el-form-item label="最低预警值">
          <el-input-number v-model="warningSetForm.minWarningNum" :min="0" placeholder="库存低于此值时触发预警" />
          <div class="form-tip">提示：库存数量低于最低预警值时会触发预警</div>
        </el-form-item>
        <el-form-item label="最高预警值">
          <el-input-number v-model="warningSetForm.maxWarningNum" :min="0" placeholder="库存高于此值时触发预警" />
          <div class="form-tip">提示：必须高于最低预警值，库存数量高于最高预警值时会触发预警</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warningSetVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWarningSet">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getStockList } from '@/api/stock'
import { lockDrug } from '@/api/drugLock'
import { replenishDrugIn } from '@/api/drugIn'
import { updateDrugWarning } from '@/api/drug'
import { getWarehouseList } from '@/api/warehouse'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const lockVisible = ref(false)
const warningSetVisible = ref(false)
const replenishVisible = ref(false)
const replenishSubmitLoading = ref(false)
const showWarningOnly = ref(false)

const searchForm = reactive({
  drugName: '',
  drugCode: '',
  warehouseId: null
})

const lockForm = reactive({
  stockId: null,
  drugId: null,
  warehouseId: null,
  drugName: '',
  warehouseName: '',
  batchNo: '',
  stockNum: 0,
  currentLockNum: 0,
  lockNum: 0,
  lockReason: ''
})

const warningSetForm = reactive({
  drugId: null,
  drugName: '',
  stockNum: 0,
  minWarningNum: 0,
  maxWarningNum: 0
})

const replenishForm = reactive({
  drugName: '',
  warehouseName: '',
  stockId: null,
  batchNo: '',
  productionDate: '',
  expiryDate: '',
  currentStock: 0,
  quantity: 1,
  purchasePrice: 0,
  suggestQty: 1,
  warningNum: 0,
  availableNum: 0,
  batchOptions: []
})

const warehouseList = ref([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 50,
  total: 0
})

const sortState = reactive({
  sortField: 'drugCode',
  sortOrder: 'asc'
})

const tableData = ref([])

const tableRowClassName = ({ row }) => {
  if (row.availableNum <= row.warningNum) {
    return 'warning-row'
  }
  return ''
}

const mergeDrugColumns = ['drugCode', 'drugName', 'spec', 'unit']

const spanMethod = ({ row, column, rowIndex }) => {
  if (!mergeDrugColumns.includes(column.property)) {
    return { rowspan: 1, colspan: 1 }
  }
  // 仅在按药品编码排序时合并，避免其他排序下误合并
  if (sortState.sortField !== 'drugCode') {
    return { rowspan: 1, colspan: 1 }
  }

  const data = tableData.value
  if (rowIndex > 0 && data[rowIndex - 1].drugId === row.drugId) {
    return { rowspan: 0, colspan: 0 }
  }

  let rowspan = 1
  for (let i = rowIndex + 1; i < data.length; i++) {
    if (data[i].drugId === row.drugId) {
      rowspan++
    } else {
      break
    }
  }
  return { rowspan, colspan: 1 }
}

const isExpired = (expiryDate) => {
  if (!expiryDate) return false
  const date = new Date(expiryDate)
  return date < new Date()
}

const isNearExpiry = (expiryDate) => {
  if (!expiryDate) return false
  const date = new Date(expiryDate)
  const now = new Date()
  const diff = (date - now) / (1000 * 60 * 60 * 24)
  return diff > 0 && diff <= 90
}

const isLowStock = (row) => row.availableNum <= row.warningNum && row.warehouseStatus !== 0

const formatStockDate = (val) => (val ? String(val).split('T')[0] : '-')

const handleRowCommand = (command, row) => {
  if (command === 'replenish') {
    openReplenish(row)
  } else if (command === 'warning') {
    handleSetWarning(row)
  }
}

const openReplenish = (groupRow, batchRow = null) => {
  const validBatches = (groupRow.batchDetails || []).filter(batch => batch.batchNo && !isExpired(batch.expiryDate))
  if (validBatches.length === 0) {
    ElMessage.warning('没有可补货的有效批次，请前往「入库管理」新建批次')
    return
  }

  const selected = batchRow && !isExpired(batchRow.expiryDate)
    ? batchRow
    : validBatches[0]
  const gap = Math.max(0, (groupRow.warningNum || 0) - (groupRow.availableNum || 0))
  // 预警条件为「可用库存 <= 最低预警」，补到相等仍会预警，故建议多补 1
  const suggestQty = gap > 0 ? gap + 1 : 1

  replenishForm.batchOptions = validBatches
  replenishForm.drugName = groupRow.drugName
  replenishForm.warehouseName = groupRow.warehouseName
  replenishForm.stockId = selected.stockId
  replenishForm.batchNo = selected.batchNo
  replenishForm.productionDate = formatStockDate(selected.productionDate)
  replenishForm.expiryDate = formatStockDate(selected.expiryDate)
  replenishForm.currentStock = selected.stockNum || 0
  replenishForm.quantity = suggestQty
  replenishForm.suggestQty = suggestQty
  replenishForm.warningNum = groupRow.warningNum || 0
  replenishForm.availableNum = groupRow.availableNum || 0
  replenishForm.purchasePrice = Number(groupRow.purchasePrice) || 0
  replenishVisible.value = true
}

const onReplenishBatchChange = (stockId) => {
  const batch = replenishForm.batchOptions.find(item => item.stockId === stockId)
  if (!batch) return
  replenishForm.batchNo = batch.batchNo
  replenishForm.productionDate = formatStockDate(batch.productionDate)
  replenishForm.expiryDate = formatStockDate(batch.expiryDate)
  replenishForm.currentStock = batch.stockNum || 0
}

const submitReplenish = async () => {
  if (!replenishForm.stockId) {
    ElMessage.warning('请选择批次')
    return
  }
  if (!replenishForm.quantity || replenishForm.quantity <= 0) {
    ElMessage.warning('请输入补货数量')
    return
  }
  if (!replenishForm.purchasePrice || replenishForm.purchasePrice <= 0) {
    ElMessage.warning('请输入入库单价')
    return
  }

  replenishSubmitLoading.value = true
  try {
    await replenishDrugIn({
      stockId: replenishForm.stockId,
      quantity: replenishForm.quantity,
      purchasePrice: replenishForm.purchasePrice
    })
    ElMessage.success('补货成功')
    replenishVisible.value = false
    loadData()
  } catch {
    // 错误由拦截器提示
  } finally {
    replenishSubmitLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStockList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...searchForm,
      warning: showWarningOnly.value,
      sortField: sortState.sortField,
      sortOrder: sortState.sortOrder
    })
    tableData.value = (res.data || []).map(item => ({
      ...item,
      rowKey: `${item.drugId}_${item.warehouseId}`
    }))
    pagination.total = Number(res.total) || 0
  } catch (error) {
    ElMessage.error('加载库存数据失败')
  } finally {
    loading.value = false
  }
}

const loadWarehouseList = async () => {
  try {
    const res = await getWarehouseList()
    warehouseList.value = res.data || []
  } catch (error) {
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  searchForm.drugName = ''
  searchForm.drugCode = ''
  searchForm.warehouseId = null
  showWarningOnly.value = false
  sortState.sortField = 'drugCode'
  sortState.sortOrder = 'asc'
  handleSearch()
}

const handleSortChange = ({ prop, order }) => {
  if (!order) {
    sortState.sortField = 'drugCode'
    sortState.sortOrder = 'asc'
  } else {
    sortState.sortField = prop
    sortState.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  }
  pagination.currentPage = 1
  loadData()
}

const toggleWarningOnly = () => {
  showWarningOnly.value = !showWarningOnly.value
  pagination.currentPage = 1
  loadData()
}

const handleLockBatch = (groupRow, batchRow) => {
  Object.assign(lockForm, {
    stockId: batchRow.stockId,
    drugId: groupRow.drugId,
    warehouseId: groupRow.warehouseId,
    drugName: groupRow.drugName,
    warehouseName: groupRow.warehouseName,
    batchNo: batchRow.batchNo,
    stockNum: batchRow.stockNum,
    currentLockNum: batchRow.lockNum || 0,
    lockNum: batchRow.lockNum || 0,
    lockReason: ''
  })
  lockVisible.value = true
}

const submitLock = async () => {
  if (!lockForm.lockReason) {
    ElMessage.warning('请输入锁定原因')
    return
  }

  const deltaLockNum = lockForm.lockNum - lockForm.currentLockNum

  if (deltaLockNum === 0) {
    ElMessage.warning('目标锁定数量与当前锁定数量相同，无需修改')
    return
  }

  if (lockForm.lockNum < 0 || lockForm.lockNum > lockForm.stockNum) {
    ElMessage.warning('目标锁定数量必须在 0 到当前库存之间')
    return
  }

  try {
    const requestData = {
      drugId: lockForm.drugId,
      warehouseId: lockForm.warehouseId,
      batchNo: lockForm.batchNo,
      lockNum: lockForm.lockNum,
      currentLockNum: lockForm.currentLockNum,
      deltaLockNum: deltaLockNum,
      lockReason: lockForm.lockReason,
      lockUserId: userStore.userInfo.userId
    }

    await lockDrug(requestData)
    ElMessage.success('锁定成功')
    lockVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '锁定失败')
  }
}

const handleSetWarning = (row) => {
  if (!row || !row.drugId) {
    ElMessage.error('药品信息不完整，无法设置预警值')
    return
  }

  Object.assign(warningSetForm, {
    drugId: row.drugId,
    drugName: row.drugName || '未知药品',
    stockNum: row.totalStockNum || 0,
    minWarningNum: row.warningNum || 0,
    maxWarningNum: row.maxWarningNum || 0
  })
  warningSetVisible.value = true
}

const submitWarningSet = async () => {
  if (!warningSetForm.drugId) {
    ElMessage.error('药品 ID 不能为空')
    return
  }

  if (warningSetForm.maxWarningNum > 0 && warningSetForm.maxWarningNum <= warningSetForm.minWarningNum) {
    ElMessage.error('最高预警值必须高于最低预警值')
    return
  }

  try {
    const data = {
      drugId: warningSetForm.drugId,
      warningNum: warningSetForm.minWarningNum,
      maxWarningNum: warningSetForm.maxWarningNum
    }
    await updateDrugWarning(data)
    ElMessage.success('设置成功')
    warningSetVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '设置失败')
  }
}

const handleSizeChange = () => {
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

onMounted(() => {
  loadData()
  loadWarehouseList()
})
</script>

<style scoped>
.expand-content {
  padding: 15px 20px;
}

.expand-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid var(--primary);
}

.text-danger {
  color: var(--danger);
  font-weight: 600;
}

.form-tip {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 5px;
  line-height: 1.5;
}

:deep(.stock-table .warning-row > td.el-table__cell) {
  background-color: #fdf2f2 !important;
}

:deep(.stock-table .warning-row:hover > td.el-table__cell) {
  background-color: #fce8e8 !important;
}

:deep(.stock-table.el-table--striped .warning-row.el-table__row--striped > td.el-table__cell) {
  background-color: #fdf2f2 !important;
}

/* 固定列需独立设置不透明背景，避免横向滚动时透底 */
:deep(.stock-table .el-table__fixed-right .el-table__body tr > td.el-table__cell) {
  background-color: #fff;
}

:deep(.stock-table.el-table--striped .el-table__fixed-right .el-table__body tr.el-table__row--striped > td.el-table__cell) {
  background-color: #fafafa;
}

:deep(.stock-table .el-table__fixed-right .el-table__body tr.warning-row > td.el-table__cell) {
  background-color: #fdf2f2 !important;
}

:deep(.stock-table .el-table__fixed-right .el-table__body tr.warning-row:hover > td.el-table__cell) {
  background-color: #fce8e8 !important;
}

:deep(.stock-table .el-table__fixed-right-patch) {
  background-color: #f6f8fa;
}

:deep(.el-table .warning-row) {
  background-color: transparent;
}

:deep(.el-table__expanded-cell) {
  padding: 0 !important;
}

:deep(.el-table__expanded-cell > .expand-content) {
  background-color: var(--bg-base);
}
</style>
