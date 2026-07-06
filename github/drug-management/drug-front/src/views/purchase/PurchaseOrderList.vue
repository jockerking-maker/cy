<template>
  <div class="purchase-order-list page-shell">
    <div class="page-header">
      <h2>采购订单</h2>
      <p>创建与管理药品采购申请</p>
    </div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :model="searchForm" inline>
        <el-form-item label="采购单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入采购单号" clearable />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="searchForm.supplierId" placeholder="请选择供应商" clearable style="width: 180px">
            <el-option
              v-for="item in supplierList"
              :key="item.supplierId"
              :label="item.supplierName"
              :value="item.supplierId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="待审核" :value="0" />
            <el-option label="已审核" :value="1" />
            <el-option label="已入库" :value="2" />
            <el-option label="已取消" :value="3" />
            <el-option label="审核不通过" :value="4" />
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

      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新建采购单
        </el-button>
        <el-button
          v-if="hasDeletePermission"
          type="danger"
          plain
          :disabled="deletableSelectedCount === 0"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除{{ deletableSelectedCount > 0 ? ` (${deletableSelectedCount})` : '' }}
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          v-if="hasDeletePermission"
          type="selection"
          width="50"
          :selectable="canDeleteOrder"
        />
        <el-table-column prop="orderNo" label="采购单号" width="180" />
        <el-table-column prop="supplierName" label="供应商" width="200" />
        <el-table-column prop="totalAmount" label="采购金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" label="订单日期" width="160" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button 
              v-if="row.status === 0 && hasAuditPermission" 
              type="success" 
              size="small" 
              @click="handleAudit(row)"
            >
              审核
            </el-button>
            <el-button 
              v-if="row.status === 0 || row.status === 1" 
              type="danger" 
              size="small" 
              @click="handleCancel(row)"
            >
              作废
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="采购单详情"
      width="900px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="采购单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ currentOrder.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="采购金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentOrder.status)">
            {{ getStatusLabel(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单日期">{{ currentOrder.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>采购明细</el-divider>

      <el-table :data="currentOrder.items" border>
        <el-table-column prop="drugName" label="药品名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column prop="unit" label="单位" />
        <el-table-column prop="purchaseNum" label="采购数量" />
        <el-table-column prop="purchasePrice" label="采购单价">
          <template #default="{ row }">
            ¥{{ row.purchasePrice }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="小计金额">
          <template #default="{ row }">
            ¥{{ row.amount }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 审核记录 -->
      <el-divider v-if="currentOrder.auditRecords && currentOrder.auditRecords.length > 0">审核记录</el-divider>
      <el-table v-if="currentOrder.auditRecords && currentOrder.auditRecords.length > 0" :data="currentOrder.auditRecords" border style="margin-top: 10px;">
        <el-table-column prop="auditUserName" label="审核人" width="120" />
        <el-table-column prop="auditTime" label="审核时间" width="180" />
        <el-table-column prop="auditResult" label="审核结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.auditResult === 1 ? 'success' : 'danger'">
              {{ row.auditResult === 1 ? '通过' : '驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核意见" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="auditVisible"
      title="采购单审核"
      width="500px"
    >
      <el-form :model="auditForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.passed">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建采购单对话框 -->
    <el-dialog
      v-model="addVisible"
      title="新建采购单"
      width="900px"
    >
      <el-form :model="addForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购单号" prop="orderNo">
              <el-input v-model="addForm.orderNo" :disabled="true" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="addForm.supplierId" placeholder="请选择供应商" style="width: 100%" @change="handleSupplierChange">
                <el-option
                  v-for="item in enabledSupplierList"
                  :key="item.supplierId"
                  :label="item.supplierName"
                  :value="item.supplierId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="订单日期" prop="orderDate">
              <el-date-picker
                v-model="addForm.orderDate"
                type="datetime"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择日期时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="addForm.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="待审核" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input
            v-model="addForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <el-divider>采购明细</el-divider>

      <el-button type="primary" size="small" @click="addItem" style="margin-bottom: 10px">
        <el-icon><Plus /></el-icon>
        添加明细
      </el-button>

      <el-table :data="addForm.items" border style="width: 100%">
        <el-table-column prop="drugId" label="药品" width="200">
          <template #default="{ row, $index }">
            <el-select v-model="row.drugId" placeholder="请选择药品" style="width: 100%" @change="handleDrugSelect($index, row.drugId)">
                <el-option
                  v-for="item in filteredDrugList"
                  :key="item.drugId"
                  :label="item.drugName + ' ' + item.spec"
                  :value="item.drugId"
                />
              </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseNum" label="采购数量" width="120">
          <template #default="{ row, $index }">
            <el-input-number v-model="row.purchaseNum" :min="1" controls-position="right" @change="calculateAmount($index)" />
          </template>
        </el-table-column>
        <el-table-column prop="purchasePrice" label="采购单价" width="120">
          <template #default="{ row, $index }">
            <el-input-number v-model="row.purchasePrice" :min="0" :precision="2" controls-position="right" @change="calculateAmount($index)" />
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="小计金额" width="120">
          <template #default="{ row }">
            ¥{{ formatMoney(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row, $index }">
            <el-button type="danger" size="small" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider>
        <div style="font-weight: bold">总计：¥{{ totalAmount }}</div>
      </el-divider>

      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPurchaseOrderList, getPurchaseOrderDetail, auditPurchaseOrder, savePurchaseOrder, cancelPurchaseOrder, batchDeletePurchaseOrders } from '@/api/purchase'
import { getSupplierList } from '@/api/supplier'
import { getDrugList } from '@/api/drug'
import { useUserStore } from '@/store/user'

const loading = ref(false)
const detailVisible = ref(false)
const auditVisible = ref(false)
const addVisible = ref(false)
const submitLoading = ref(false)
const selectedRows = ref([])

const userStore = useUserStore()

// 检查用户是否有审核权限
const hasAuditPermission = computed(() => {
  const roles = userStore.userRoles
  return roles.includes('ADMIN') || roles.includes('AUDITOR')
})

const hasDeletePermission = computed(() => {
  const roles = userStore.userRoles
  return roles.includes('ADMIN') || roles.includes('PURCHASER')
})

const canDeleteOrder = (row) => [0, 3, 4].includes(row.status)

const deletableSelectedCount = computed(() =>
  selectedRows.value.filter(row => canDeleteOrder(row)).length
)

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  supplierId: null,
  status: null
})

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref([])

// 供应商列表
const supplierList = ref([])
const enabledSupplierList = ref([])

// 药品列表
const drugList = ref([])

// 过滤后的药品列表（根据当前选择的供应商）
const filteredDrugList = computed(() => {
  if (!addForm.supplierId) {
    return [] // 未选择供应商时，不显示任何药品
  }
  return drugList.value.filter(drug => drug.supplierId === addForm.supplierId)
})

// 当前订单
const currentOrder = ref({
  items: []
})

// 审核表单
const auditForm = reactive({
  orderId: null,
  passed: true,
  remark: ''
})

// 本地日期时间（与采购单号一致，避免 toISOString 的 UTC 偏差）
const formatLocalDateTime = (date = new Date()) => {
  const y = date.getFullYear()
  const M = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${M}-${d}T${h}:${m}:${s}`
}

// 新建采购单表单
const addForm = reactive({
  orderNo: '',
  supplierId: null,
  orderDate: formatLocalDateTime(),
  status: 0,
  remark: '',
  items: []
})

// 金额计算与格式化（避免浮点精度问题）
const roundMoney = (value) => Math.round((Number(value) || 0) * 100) / 100

const calcAmount = (num, price) => roundMoney((Number(num) || 0) * (Number(price) || 0))

const formatMoney = (value) => roundMoney(value).toFixed(2)

// 计算总金额
const totalAmount = computed(() => {
  const sum = addForm.items.reduce((total, item) => total + (Number(item.amount) || 0), 0)
  return formatMoney(sum)
})

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    0: 'warning',
    1: 'success',
    2: 'success',
    3: 'danger',
    4: 'danger'
  }
  return types[status] || 'info'
}

// 获取状态标签
const getStatusLabel = (status) => {
  const labels = {
    0: '待审核',
    1: '已审核',
    2: '已入库',
    3: '已取消',
    4: '审核不通过'
  }
  return labels[status] || '未知'
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getPurchaseOrderList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...searchForm
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载供应商列表
const loadSuppliers = async () => {
  try {
    const [allRes, enabledRes] = await Promise.all([
      getSupplierList(),
      getSupplierList({ status: 1 })
    ])
    supplierList.value = allRes.data || []
    enabledSupplierList.value = enabledRes.data || []
  } catch (error) {
    console.error('加载供应商失败:', error)
  }
}

// 加载药品列表
const loadDrugs = async () => {
  try {
    const res = await getDrugList({ page: 1, size: 1000 })
    drugList.value = res.data || []
  } catch (error) {
    console.error('加载药品失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.supplierId = null
  searchForm.status = null
  handleSearch()
}

// 查看详情
const handleView = async (row) => {
  try {
    const res = await getPurchaseOrderDetail(row.orderId)
    currentOrder.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error('获取采购单详情失败:', error)
    ElMessage.error('获取采购单详情失败')
  }
}

// 审核
const handleAudit = (row) => {
  auditForm.orderId = row.orderId
  auditForm.passed = true
  auditForm.remark = ''
  auditVisible.value = true
}

// 提交审核
const submitAudit = async () => {
  try {
    await auditPurchaseOrder(auditForm.orderId, {
      passed: auditForm.passed,
      remark: auditForm.remark
    })
    ElMessage.success('审核成功')
    auditVisible.value = false
    loadData()
  } catch (error) {
    console.error('审核失败:', error)
  }
}

// 作废
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要作废采购单"${row.orderNo}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await cancelPurchaseOrder(row.orderId, {})
    ElMessage.success('作废成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('作废失败:', error)
    }
  }
}

// 新增
const generateOrderNo = () => {
  return 'CG' + formatLocalDateTime().replace(/[-:T]/g, '')
}

const handleAdd = () => {
  addForm.orderNo = generateOrderNo()
  addForm.supplierId = null
  addForm.orderDate = formatLocalDateTime()
  addForm.status = 0
  addForm.remark = ''
  addForm.items = []
  loadDrugs()
  addVisible.value = true
}

// 添加采购明细
const addItem = () => {
  addForm.items.push({
    drugId: null,
    purchaseNum: 1,
    purchasePrice: 0,
    amount: 0
  })
}

// 删除采购明细
const removeItem = (index) => {
  addForm.items.splice(index, 1)
}

// 处理药品选择
const handleDrugSelect = (index, drugId) => {
  const drug = drugList.value.find(item => item.drugId === drugId)
  if (drug) {
    addForm.items[index].purchasePrice = drug.purchasePrice
    calculateAmount(index)
  }
}

// 计算明细金额
const calculateAmount = (index) => {
  const item = addForm.items[index]
  if (item.drugId && item.purchaseNum && item.purchasePrice) {
    item.amount = calcAmount(item.purchaseNum, item.purchasePrice)
  } else {
    item.amount = 0
  }
}

// 处理供应商选择变化
const handleSupplierChange = async () => {
  // 清空现有采购明细
  addForm.items = []
  
  // 加载药品列表
  if (drugList.value.length === 0) {
    await loadDrugs()
  }
}

// 提交新建采购单
const submitAdd = async () => {
  // 验证表单
  if (!addForm.orderNo) {
    ElMessage.error('请输入采购单号')
    return
  }
  if (!addForm.supplierId) {
    ElMessage.error('请选择供应商')
    return
  }
  if (addForm.items.length === 0) {
    ElMessage.error('请添加采购明细')
    return
  }
  
  // 验证所有选择的药品是否属于当前供应商
  for (const item of addForm.items) {
    const drug = drugList.value.find(d => d.drugId === item.drugId)
    if (!drug || drug.supplierId !== addForm.supplierId) {
      ElMessage.error('存在不属于当前供应商的药品，请重新选择')
      return
    }
  }
  
  // 计算所有明细的金额
  addForm.items.forEach((item, index) => {
    calculateAmount(index)
  })
  
  // 计算总金额
  const total = addForm.items.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)

  submitLoading.value = true
  try {
    await savePurchaseOrder({
      ...addForm,
      totalAmount: roundMoney(total)
    })
    ElMessage.success('新建采购单成功')
    addVisible.value = false
    loadData()
  } catch (error) {
    console.error('新建采购单失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleBatchDelete = async () => {
  const deletableRows = selectedRows.value.filter(row => canDeleteOrder(row))
  if (deletableRows.length === 0) {
    ElMessage.warning('请勾选可删除的采购单（待审核、已取消、审核不通过）')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${deletableRows.length} 条采购单吗？将同时删除明细与审核记录，且不可恢复。`,
      '批量删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const res = await batchDeletePurchaseOrders({
      orderIds: deletableRows.map(row => row.orderId)
    })
    ElMessage.success(res.msg || '批量删除成功')
    selectedRows.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

// 分页大小改变
const handleSizeChange = () => {
  loadData()
}

// 页码改变
const handleCurrentChange = () => {
  loadData()
}

onMounted(() => {
  loadData()
  loadSuppliers()
})
</script>

