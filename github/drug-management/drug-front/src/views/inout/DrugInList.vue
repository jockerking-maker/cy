<template>
  <div class="drug-in-list page-shell">
    <div class="page-header">
      <h2>药品入库</h2>
      <p>登记采购入库与退货入库</p>
    </div>
    <el-card shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="入库单号">
          <el-input v-model="searchForm.inNo" placeholder="请输入入库单号" clearable />
        </el-form-item>
        <el-form-item label="药品名称">
          <el-input v-model="searchForm.drugName" placeholder="请输入药品名称" clearable />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="searchForm.warehouseId" placeholder="请选择仓库" clearable style="width: 200px">
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
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增入库单
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="inNo" label="入库单号" width="160" />
        <el-table-column prop="inType" label="入库类型" width="100" />
        <el-table-column prop="drugName" label="药品名称" width="200" />
        <el-table-column prop="spec" label="规格" width="150" />
        <el-table-column prop="warehouseName" label="仓库" width="150" />
        <el-table-column prop="quantity" label="入库数量" width="100" />
        <el-table-column prop="batchNo" label="批次号" width="150" />
        <el-table-column prop="purchasePrice" label="入库单价" width="100" />
        <el-table-column prop="productionDate" label="生产日期" width="120" />
        <el-table-column prop="expiryDate" label="过期日期" width="120" />
        <el-table-column prop="inDate" label="操作时间" width="160" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog
      v-model="detailVisible"
      title="入库单详情"
      width="700px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ currentIn.inNo }}</el-descriptions-item>
        <el-descriptions-item label="药品名称">{{ currentIn.drugName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentIn.spec }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentIn.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="入库数量">{{ currentIn.quantity }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ currentIn.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="入库单价">{{ currentIn.purchasePrice }}</el-descriptions-item>
        <el-descriptions-item label="生产日期">{{ currentIn.productionDate }}</el-descriptions-item>
        <el-descriptions-item label="过期日期">{{ currentIn.expiryDate }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentIn.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentIn.inDate }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentIn.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form
        ref="drugInFormRef"
        :model="drugInForm"
        :rules="drugInRules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品" prop="drugId">
              <el-select v-model="drugInForm.drugId" placeholder="请选择药品" filterable style="width: 100%" @change="handleDrugChange">
                <el-option
                  v-for="item in drugList"
                  :key="item.drugId"
                  :label="`${item.drugName} (${item.spec})`"
                  :value="item.drugId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="drugInForm.warehouseId" placeholder="请选择仓库" style="width: 100%">
                <el-option
                  v-for="item in warehouseList"
                  :key="item.warehouseId"
                  :label="item.warehouseName"
                  :value="item.warehouseId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="入库类型" prop="inType">
              <el-select v-model="drugInForm.inType" placeholder="请选择入库类型" style="width: 100%">
                <el-option label="采购入库" value="采购入库" />
                <el-option label="退货入库" value="退货入库" />
                <el-option label="调拨入库" value="调拨入库" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库数量" prop="quantity">
              <el-input-number v-model="drugInForm.quantity" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="批次号" prop="batchNo">
              <el-input v-model="drugInForm.batchNo" readonly>
                <template #suffix>
                  <el-tooltip content="系统自动生成，格式：PC+日期+序号" placement="top">
                    <el-icon style="color: #909399; cursor: pointer"><InfoFilled /></el-icon>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库单价" prop="purchasePrice">
              <el-input v-model.number="drugInForm.purchasePrice" placeholder="请输入入库单价" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生产日期" prop="productionDate">
              <el-date-picker
                v-model="drugInForm.productionDate"
                type="date"
                placeholder="选择生产日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                @change="handleProductionDateChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期日期" prop="expiryDate">
              <el-date-picker
                v-model="drugInForm.expiryDate"
                type="date"
                placeholder="选择过期日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <div v-if="selectedShelfLife" class="form-tip">
                根据保质期（{{ selectedShelfLife }}个月）自动计算，可手动修改
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="关联采购单">
          <el-select v-model="drugInForm.orderId" placeholder="请选择采购单 (可选)" clearable filterable style="width: 100%" @change="handlePurchaseOrderChange">
            <el-option
              v-for="item in purchaseOrderList"
              :key="item.orderId"
              :label="`${item.orderNo} - ${item.supplierName || '未知供应商'} - ¥${item.totalAmount || 0}`"
              :value="item.orderId"
            >
              <div class="purchase-order-option">
                <div class="order-no">{{ item.orderNo }}</div>
                <div class="order-info">
                  <div><span class="label">供应商:</span> {{ item.supplierName || '未知供应商' }}</div>
                  <div><span class="label">采购日期:</span> {{ item.orderDate || '未知' }}</div>
                  <div><span class="label">总金额:</span> ¥{{ item.totalAmount || 0 }}</div>
                </div>
              </div>
            </el-option>
          </el-select>
          <div class="form-tip">可选：关联「已审核」采购单。采购单内所有药品均入库完成后，状态将自动变为「已入库」</div>
        </el-form-item>

        <el-form-item v-if="purchaseOrderItems.length > 0" label="采购明细">
          <el-table :data="purchaseOrderItems" border size="small" max-height="300">
            <el-table-column prop="drugName" label="药品名称" min-width="150" />
            <el-table-column prop="spec" label="规格" width="100" />
            <el-table-column prop="purchaseNum" label="采购数量" width="100" align="right" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="purchasePrice" label="采购单价" width="100" align="right">
              <template #default="{ row }">
                ¥{{ row.purchasePrice }}
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="小计金额" width="100" align="right">
              <template #default="{ row }">
                ¥{{ row.amount }}
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="drugInForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDrugInList, saveDrugIn, deleteDrugIn, getNextBatchNo } from '@/api/drugIn'
import { getDrugList } from '@/api/drug'
import { getWarehouseList } from '@/api/warehouse'
import { getPurchaseOrderList, getPurchaseOrderDetail } from '@/api/purchase'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('新增入库单')
const drugInFormRef = ref(null)
const selectedShelfLife = ref(null)

const searchForm = reactive({
  inNo: '',
  drugName: '',
  warehouseId: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const currentIn = ref({})

const drugInForm = reactive({
  inId: null,
  drugId: null,
  warehouseId: null,
  inType: '采购入库',
  quantity: 1,
  batchNo: '',
  purchasePrice: 0,
  productionDate: '',
  expiryDate: '',
  orderId: null,
  remark: ''
})

const warehouseList = ref([])
const drugList = ref([])
const purchaseOrderList = ref([])
const purchaseOrderItems = ref([])

const drugInRules = {
  drugId: [{ required: true, message: '请选择药品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  inType: [{ required: true, message: '请选择入库类型', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }],
  batchNo: [{ required: true, message: '批次号由系统自动生成', trigger: 'blur' }],
  purchasePrice: [{ required: true, message: '请输入入库单价', trigger: 'blur' }],
  productionDate: [{ required: true, message: '请选择生产日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择过期日期', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDrugInList({
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

const loadDrugs = async () => {
  try {
    const res = await getDrugList({ page: 1, size: 1000 })
    drugList.value = res.data || []
  } catch (error) {
    console.error('加载药品失败:', error)
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  searchForm.inNo = ''
  searchForm.drugName = ''
  searchForm.warehouseId = null
  handleSearch()
}

const handleView = (row) => {
  currentIn.value = row
  detailVisible.value = true
}

const handleAdd = async () => {
  dialogTitle.value = '新增入库单'
  dialogVisible.value = true
  try {
    const res = await getNextBatchNo()
    if (res.data) {
      drugInForm.batchNo = res.data
    }
  } catch (error) {
    console.error('获取批次号失败:', error)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要作废入库单"${row.inNo}"吗？作废后对应库存将回退，此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定作废',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await deleteDrugIn(row.inId)
    ElMessage.success('作废成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('作废失败:', error)
    }
  }
}

const handleSubmit = async () => {
  if (!drugInFormRef.value) return

  await drugInFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const formData = {
          ...drugInForm,
          createUserId: userStore.userInfo.userId
        }
        await saveDrugIn(formData)
        ElMessage.success('添加成功')
        dialogVisible.value = false
        loadData()
      } catch (error) {
        console.error('保存失败:', error)
        ElMessage.error(error.response?.data?.msg || error.message || '保存失败')
      } finally {
        submitLoading.value = false
      }
    } else {
      ElMessage.warning('请填写完整的表单信息')
    }
  })
}

const handleDialogClose = () => {
  drugInFormRef.value?.resetFields()
  Object.assign(drugInForm, {
    inId: null,
    drugId: null,
    warehouseId: null,
    inType: '采购入库',
    quantity: 1,
    batchNo: '',
    purchasePrice: 0,
    productionDate: '',
    expiryDate: '',
    orderId: null,
    remark: ''
  })
  selectedShelfLife.value = null
  purchaseOrderItems.value = []
}

const handleSizeChange = () => {
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

onMounted(() => {
  loadData()
  loadDrugs()
  loadWarehouses()
  loadPurchaseOrders()
})

const loadWarehouses = async () => {
  try {
    const res = await getWarehouseList({ status: 1 })
    warehouseList.value = res.data || []
  } catch (error) {
    console.error('加载仓库失败:', error)
  }
}

const loadPurchaseOrders = async () => {
  try {
    const res = await getPurchaseOrderList({ status: 1 })
    purchaseOrderList.value = res.data || []
  } catch (error) {
    console.error('加载采购单失败:', error)
  }
}

const handleDrugChange = (drugId) => {
  if (drugId) {
    const selectedDrug = drugList.value.find(drug => drug.drugId === drugId)
    if (selectedDrug) {
      if (selectedDrug.purchasePrice) {
        drugInForm.purchasePrice = selectedDrug.purchasePrice
      }
      selectedShelfLife.value = selectedDrug.shelfLife || null
      if (drugInForm.productionDate && selectedShelfLife.value) {
        calcExpiryDate()
      }
    }
  } else {
    selectedShelfLife.value = null
  }
}

const handleProductionDateChange = () => {
  if (drugInForm.productionDate && selectedShelfLife.value) {
    calcExpiryDate()
  }
}

const calcExpiryDate = () => {
  if (!drugInForm.productionDate || !selectedShelfLife.value) return
  const prodDate = new Date(drugInForm.productionDate)
  prodDate.setMonth(prodDate.getMonth() + selectedShelfLife.value)
  const year = prodDate.getFullYear()
  const month = String(prodDate.getMonth() + 1).padStart(2, '0')
  const day = String(prodDate.getDate()).padStart(2, '0')
  drugInForm.expiryDate = `${year}-${month}-${day}`
}

const handlePurchaseOrderChange = async (orderId) => {
  if (orderId) {
    try {
      const res = await getPurchaseOrderDetail(orderId)
      if (res.data && res.data.items) {
        purchaseOrderItems.value = res.data.items || []
      }
    } catch (error) {
      console.error('加载采购单明细失败:', error)
      purchaseOrderItems.value = []
    }
  } else {
    purchaseOrderItems.value = []
  }
}
</script>

<style scoped>
.purchase-order-option {
  padding: 8px 0;
}

.order-no {
  font-weight: bold;
  margin-bottom: 4px;
}

.order-info {
  font-size: 12px;
  color: var(--text-secondary);
}

.order-info .label {
  color: var(--text-tertiary);
  margin-right: 4px;
}

.order-info div {
  margin-bottom: 2px;
}

.form-tip {
  font-size: 12px;
  color: var(--success);
  margin-top: 4px;
  line-height: 1.5;
}
</style>
