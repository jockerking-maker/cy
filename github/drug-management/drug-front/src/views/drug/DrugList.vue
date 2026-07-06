<template>
  <div class="drug-list page-shell">
    <div class="page-header">
      <h2>药品管理</h2>
      <p>维护药品基础信息与上下架状态</p>
    </div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :model="searchForm" inline>
        <el-form-item label="药品名称">
          <el-input v-model="searchForm.drugName" placeholder="请输入药品名称" clearable />
        </el-form-item>
        <el-form-item label="药品编码">
          <el-input v-model="searchForm.drugCode" placeholder="请输入药品编码" clearable />
        </el-form-item>
        <el-form-item label="药品类型">
          <el-select v-model="searchForm.drugType" placeholder="请选择类型" clearable style="width: 150px">
            <el-option label="西药" value="西药" />
            <el-option label="中药" value="中药" />
            <el-option label="中成药" value="中成药" />
            <el-option label="耗材" value="耗材" />
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
          新增药品
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
        <el-table-column prop="drugCode" label="药品编码" width="120" />
        <el-table-column prop="drugName" label="药品名称" width="200" />
        <el-table-column prop="drugType" label="药品类型" width="100" />
        <el-table-column prop="spec" label="规格" width="150" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="price" label="销售单价" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="purchasePrice" label="采购单价" width="100">
          <template #default="{ row }">
            ¥{{ row.purchasePrice }}
          </template>
        </el-table-column>
        <el-table-column prop="productionEnterprise" label="生产企业" width="200" show-overflow-tooltip />
        <el-table-column prop="approvalNum" label="批准文号" width="150" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
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
        :hide-on-single-page="false"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination-wrap"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form
        ref="drugFormRef"
        :model="drugForm"
        :rules="drugRules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品编码" prop="drugCode">
              <el-input v-model="drugForm.drugCode" placeholder="系统自动生成，可修改" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="药品名称" prop="drugName">
              <el-input v-model="drugForm.drugName" placeholder="请输入药品名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品类型" prop="drugType">
              <el-select v-model="drugForm.drugType" placeholder="请选择类型" style="width: 100%">
                <el-option label="西药" value="西药" />
                <el-option label="中药" value="中药" />
                <el-option label="中成药" value="中成药" />
                <el-option label="耗材" value="耗材" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="drugForm.spec" placeholder="如：0.25g*24 粒/瓶" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="drugForm.unit" placeholder="请选择单位" style="width: 100%">
                <el-option label="片" value="片" />
                <el-option label="瓶" value="瓶" />
                <el-option label="盒" value="盒" />
                <el-option label="袋" value="袋" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售单价" prop="price">
              <el-input-number v-model="drugForm.price" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购单价" prop="purchasePrice">
              <el-input-number v-model="drugForm.purchasePrice" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存预警值" prop="warningNum">
              <el-input-number v-model="drugForm.warningNum" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="保质期 (月)" prop="shelfLife">
              <el-input-number v-model="drugForm.shelfLife" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="drugForm.status">
                <el-radio :label="1">上架</el-radio>
                <el-radio :label="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="生产企业" prop="supplierId">
          <el-select v-model="drugForm.supplierId" placeholder="请选择生产企业" style="width: 100%" @change="handleSupplierChange">
            <el-option 
              v-for="supplier in supplierList" 
              :key="supplier.supplierId" 
              :label="supplier.supplierName" 
              :value="supplier.supplierId" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="批准文号" prop="approvalNum">
          <el-input v-model="drugForm.approvalNum" placeholder="系统自动生成，可修改" />
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDrugList, getNextDrugCode, saveDrug, updateDrug, deleteDrug } from '@/api/drug'
import { getSupplierList, getSupplierDetail } from '@/api/supplier'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增药品')
const drugFormRef = ref(null)

// 搜索表单
const searchForm = reactive({
  drugName: '',
  drugCode: '',
  drugType: ''
})

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 50,
  total: 0
})

// 表格数据
const tableData = ref([])

// 供应商列表
const supplierList = ref([])

// 表单数据
const drugForm = reactive({
  drugId: null,
  drugCode: '',
  drugName: '',
  drugType: '',
  spec: '',
  unit: '',
  price: 0,
  purchasePrice: 0,
  supplierId: null,
  productionEnterprise: '',
  approvalNum: '',
  shelfLife: 24,
  warningNum: 10,
  status: 1
})

// 表单验证规则
const drugRules = {
  drugCode: [{ required: true, message: '请输入药品编码', trigger: 'blur' }],
  drugName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  drugType: [{ required: true, message: '请选择药品类型', trigger: 'change' }],
  spec: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
  price: [{ required: true, message: '请输入销售单价', trigger: 'blur' }],
  purchasePrice: [{ required: true, message: '请输入采购单价', trigger: 'blur' }],
  supplierId: [{ required: true, message: '请选择生产企业', trigger: 'change' }],
  approvalNum: [{ required: true, message: '请输入批准文号', trigger: 'blur' }]
}

// 加载供应商列表（仅启用；编辑时保留当前已绑定的供应商）
const loadSupplierList = async (currentSupplierId) => {
  try {
    const res = await getSupplierList({ status: 1 })
    const list = res.data || []
    if (currentSupplierId && !list.some(item => item.supplierId === currentSupplierId)) {
      const detail = await getSupplierDetail(currentSupplierId)
      if (detail.data) {
        list.push(detail.data)
      }
    }
    supplierList.value = list
  } catch (error) {
    console.error('加载供应商列表失败:', error)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.currentPage,
      size: pagination.pageSize
    }
    if (searchForm.drugName) params.drugName = searchForm.drugName
    if (searchForm.drugCode) params.drugCode = searchForm.drugCode
    if (searchForm.drugType) params.drugType = searchForm.drugType

    const res = await getDrugList(params)
    tableData.value = res.data || []
    pagination.total = Number(res.total) || 0
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.drugName = ''
  searchForm.drugCode = ''
  searchForm.drugType = ''
  handleSearch()
}

// 自动生成编码与批准文号
const fillNextCodes = async (drugType, { updateDrugCode = true, updateApprovalNum = true } = {}) => {
  try {
    const res = await getNextDrugCode(drugType ? { drugType } : {})
    if (res.data) {
      if (updateDrugCode) drugForm.drugCode = res.data.drugCode || ''
      if (updateApprovalNum) drugForm.approvalNum = res.data.approvalNum || ''
    }
  } catch (error) {
    console.error('生成编码失败:', error)
  }
}

// 新增
const handleAdd = async () => {
  handleDialogClose()
  await loadSupplierList()
  dialogTitle.value = '新增药品'
  dialogVisible.value = true
  await fillNextCodes()
}

watch(
  () => drugForm.drugType,
  async (drugType) => {
    if (dialogVisible.value && !drugForm.drugId && drugType) {
      await fillNextCodes(drugType, { updateDrugCode: false, updateApprovalNum: true })
    }
  }
)

// 监听 supplierId 变化，自动设置 productionEnterprise
const handleSupplierChange = (supplierId) => {
  const supplier = supplierList.value.find(s => s.supplierId === supplierId)
  if (supplier) {
    drugForm.productionEnterprise = supplier.supplierName
  }
}

// 编辑
const handleEdit = async (row) => {
  dialogTitle.value = '编辑药品'
  Object.assign(drugForm, row)
  await loadSupplierList(row.supplierId)
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除药品"${row.drugName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDrug(row.drugId)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!drugFormRef.value) return
  
  await drugFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (drugForm.drugId) {
          await updateDrug(drugForm)
          ElMessage.success('更新成功')
        } else {
          await saveDrug(drugForm)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        console.error('保存失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  drugFormRef.value?.resetFields()
  Object.assign(drugForm, {
    drugId: null,
    drugCode: '',
    drugName: '',
    drugType: '',
    spec: '',
    unit: '',
    price: 0,
    purchasePrice: 0,
    supplierId: null,
    productionEnterprise: '',
    approvalNum: '',
    shelfLife: 24,
    warningNum: 10,
    status: 1
  })
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
  loadSupplierList()
})
</script>

