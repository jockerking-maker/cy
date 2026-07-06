<template>
  <div class="drug-out-list page-shell">
    <div class="page-header">
      <h2>药品出库</h2>
      <p>登记领用出库与报损出库</p>
    </div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :model="searchForm" inline>
        <el-form-item label="出库单号">
          <el-input v-model="searchForm.outNo" placeholder="请输入出库单号" clearable />
        </el-form-item>
        <el-form-item label="药品名称">
          <el-input v-model="searchForm.drugName" placeholder="请输入药品名称" clearable />
        </el-form-item>
        <el-form-item label="出库类型">
          <el-select v-model="searchForm.outType" placeholder="请选择类型" clearable style="width: 200px">
            <el-option label="门诊领药" value="门诊领药" />
            <el-option label="住院领药" value="住院领药" />
            <el-option label="调拨" value="调拨" />
            <el-option label="报废" value="报废" />
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
          新增出库单
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="outNo" label="出库单号" width="160" />
        <el-table-column prop="drugName" label="药品名称" width="200" />
        <el-table-column prop="spec" label="规格" width="150" />
        <el-table-column prop="warehouseName" label="仓库" width="150" />
        <el-table-column prop="outNum" label="出库数量" width="100" />
        <el-table-column prop="outType" label="出库类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.outType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="outDate" label="操作时间" width="160" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
      title="出库单详情"
      width="700px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ currentOut.outNo }}</el-descriptions-item>
        <el-descriptions-item label="药品名称">{{ currentOut.drugName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentOut.spec }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentOut.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="出库数量">{{ currentOut.outNum }}</el-descriptions-item>
        <el-descriptions-item label="出库类型">{{ currentOut.outType }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentOut.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentOut.outDate }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOut.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form
        ref="drugOutFormRef"
        :model="drugOutForm"
        :rules="drugOutRules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品" prop="drugId">
              <el-select v-model="drugOutForm.drugId" placeholder="请选择药品" filterable @change="handleDrugChange">
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
              <el-select v-model="drugOutForm.warehouseId" placeholder="请选择仓库" style="width: 100%" @change="handleWarehouseChange">
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
            <el-form-item label="批次号" prop="batchNo">
              <el-select v-model="drugOutForm.batchNo" placeholder="请选择批次" style="width: 100%" @change="handleBatchChange">
                <el-option
                  v-for="item in batchList"
                  :key="item.batchNo"
                  :label="`${item.batchNo} (可用: ${(item.stockNum || 0) - (item.lockNum || 0)}, 效期: ${item.expiryDate ? item.expiryDate.split('T')[0] : '-'})`"
                  :value="item.batchNo"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可用库存">
              <span style="font-weight: bold; color: #409EFF;">{{ availableStock }}</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出库数量" prop="outNum">
              <el-input-number v-model="drugOutForm.outNum" :min="1" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出库类型" prop="outType">
              <el-select v-model="drugOutForm.outType" placeholder="请选择类型" style="width: 100%">
                <el-option label="门诊领药" value="门诊领药" />
                <el-option label="住院领药" value="住院领药" />
                <el-option label="调拨" value="调拨" />
                <el-option label="报废" value="报废" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出库单价" prop="salePrice">
              <el-input-number v-model="drugOutForm.salePrice" :min="0.01" :precision="2" :step="0.1" placeholder="请输入出库单价" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input
            v-model="drugOutForm.remark"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDrugOutList, saveDrugOut, deleteDrugOut } from '@/api/drugOut'
import { getDrugList } from '@/api/drug'
import { getWarehouseList } from '@/api/warehouse'
import { getStockList } from '@/api/stock'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('新增出库单')
const drugOutFormRef = ref(null)

// 搜索表单
const searchForm = reactive({
  outNo: '',
  drugName: '',
  outType: ''
})

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref([])

// 当前出库单
const currentOut = ref({})

// 表单数据
const drugOutForm = reactive({
  outId: null,
  drugId: null,
  warehouseId: null,
  batchNo: '',
  outNum: 1,
  outType: '',
  salePrice: 0,
  remark: ''
})

// 仓库列表
const warehouseList = ref([])

// 药品列表
const drugList = ref([])

const batchList = ref([])
const availableStock = computed(() => {
  if (!drugOutForm.batchNo) return '请先选择批次'
  const batch = batchList.value.find(b => b.batchNo === drugOutForm.batchNo)
  return batch ? ((batch.stockNum || 0) - (batch.lockNum || 0)) : 0
})

// 表单验证规则
const drugOutRules = {
  drugId: [{ required: true, message: '请选择药品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  batchNo: [{ required: true, message: '请选择批次', trigger: 'change' }],
  outNum: [{ required: true, message: '请输入出库数量', trigger: 'blur' }],
  outType: [{ required: true, message: '请选择出库类型', trigger: 'change' }],
  salePrice: [
    { required: true, message: '请输入出库单价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '出库单价必须大于 0', trigger: 'blur' }
  ]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getDrugOutList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...searchForm
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取出库列表失败')
  } finally {
    loading.value = false
  }
}

// 加载药品列表
const loadDrugs = async () => {
  try {
    const res = await getDrugList({ page: 1, size: 1000 })
    drugList.value = res.data || []
  } catch (error) {
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.outNo = ''
  searchForm.drugName = ''
  searchForm.outType = ''
  handleSearch()
}

// 查看详情
const handleView = (row) => {
  currentOut.value = row
  detailVisible.value = true
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增出库单'
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除出库单"${row.outNo}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDrugOut(row.outId)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!drugOutFormRef.value) return
  
  await drugOutFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        // 确保 salePrice 是数字类型
        const formData = {
          ...drugOutForm,
          salePrice: Number(drugOutForm.salePrice),
          createUserId: userStore.userInfo.userId
        }
        await saveDrugOut(formData)
        ElMessage.success('添加成功')
        dialogVisible.value = false
        loadData()
      } catch (error) {
        const errorMsg = error.response?.data?.msg || error.message || '保存失败'
        ElMessage.closeAll() // 关闭所有已打开的消息提示
        ElMessage.error(errorMsg)
      } finally {
        submitLoading.value = false
      }
    } else {
      ElMessage.warning('请填写完整的表单信息')
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  drugOutFormRef.value?.resetFields()
  Object.assign(drugOutForm, {
    outId: null,
    drugId: null,
    warehouseId: null,
    batchNo: '',
    outNum: 1,
    outType: '',
    salePrice: 0,
    remark: ''
  })
  batchList.value = []
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
  loadDrugs()
  loadWarehouses()
})

// 加载仓库列表
const loadWarehouses = async () => {
  try {
    const res = await getWarehouseList({ status: 1 })
    warehouseList.value = res.data || []
  } catch (error) {
  }
}

// 药品选择变化处理
const handleDrugChange = (drugId) => {
  drugOutForm.batchNo = ''
  batchList.value = []
  if (drugId) {
    const selectedDrug = drugList.value.find(drug => drug.drugId === drugId)
    if (selectedDrug && selectedDrug.price) {
      drugOutForm.salePrice = selectedDrug.price
    } else {
      drugOutForm.salePrice = selectedDrug?.purchasePrice || 0
    }
    if (drugOutForm.warehouseId) {
      loadBatchList()
    }
  }
}

const handleWarehouseChange = () => {
  drugOutForm.batchNo = ''
  batchList.value = []
  if (drugOutForm.warehouseId && drugOutForm.drugId) {
    loadBatchList()
  }
}

const loadBatchList = async () => {
  if (drugOutForm.drugId && drugOutForm.warehouseId) {
    try {
      const res = await getStockList({
        drugName: '',
        drugCode: '',
        warehouseId: drugOutForm.warehouseId,
        page: 1,
        size: 1000
      })
      const allGroups = res.data || []
      const matchedGroup = allGroups.find(item => item.drugId === drugOutForm.drugId)
      if (matchedGroup && matchedGroup.batchDetails) {
        batchList.value = matchedGroup.batchDetails.filter(batch =>
          (batch.stockNum - (batch.lockNum || 0)) > 0
        )
      } else {
        batchList.value = []
      }
    } catch (error) {
      batchList.value = []
    }
  } else {
    batchList.value = []
  }
}

const handleBatchChange = () => {
}
</script>

