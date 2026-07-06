<template>
  <div class="purchase-audit-list page-shell">
    <div class="page-header">
      <h2>采购审核</h2>
      <p>审核待处理的采购订单</p>
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

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
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
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button 
              v-if="row.status === 0" 
              type="success" 
              size="small" 
              @click="handleAudit(row)"
            >
              审核
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPurchaseOrderList, getPurchaseOrderDetail, auditPurchaseOrder } from '@/api/purchase'
import { getSupplierList } from '@/api/supplier'

const loading = ref(false)
const detailVisible = ref(false)
const auditVisible = ref(false)

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  supplierId: null
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
    const params = { ...searchForm }
    if (params.status === null || params.status === undefined || params.status === '') {
      params.status = 0
    }
    const res = await getPurchaseOrderList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...params
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
    const res = await getSupplierList()
    supplierList.value = res.data || []
  } catch (error) {
    console.error('加载供应商失败:', error)
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
    if (error.response && error.response.data && error.response.data.msg) {
      ElMessage.error(error.response.data.msg)
    } else {
      ElMessage.error('审核失败')
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
