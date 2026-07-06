<template>
  <div class="warehouse-list page-shell">
    <div class="page-header">
      <h2>仓库管理</h2>
      <p>配置库房信息与存放区域</p>
    </div>
    <el-card shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="仓库名称">
          <el-input v-model="searchForm.warehouseName" placeholder="请输入仓库名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
          新增仓库
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="warehouseCode" label="仓库编码" width="120" />
        <el-table-column prop="warehouseName" label="仓库名称" width="200" />
        <el-table-column prop="address" label="仓库地址" show-overflow-tooltip />
        <el-table-column prop="manager" label="管理员" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
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
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form
        ref="warehouseFormRef"
        :model="warehouseForm"
        :rules="warehouseRules"
        label-width="100px"
      >
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input
            v-model="warehouseForm.warehouseCode"
            :placeholder="warehouseForm.warehouseId ? '请输入仓库编码' : '系统自动生成'"
            :readonly="!warehouseForm.warehouseId"
          />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="warehouseForm.warehouseName" placeholder="请输入仓库名称" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input
            v-model="warehouseForm.address"
            type="textarea"
            :rows="3"
            placeholder="请输入仓库地址"
          />
        </el-form-item>
        <el-form-item label="管理员" prop="manager">
          <el-input v-model="warehouseForm.manager" placeholder="请输入管理员姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="warehouseForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="warehouseForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { getWarehouseList, saveWarehouse, updateWarehouse, deleteWarehouse, getNextWarehouseCode } from '@/api/warehouse'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增仓库')
const warehouseFormRef = ref(null)

const searchForm = reactive({
  warehouseName: '',
  status: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const warehouseForm = reactive({
  warehouseId: null,
  warehouseCode: '',
  warehouseName: '',
  address: '',
  manager: '',
  phone: '',
  status: 1
})

const warehouseRules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入仓库地址', trigger: 'blur' }],
  manager: [{ required: true, message: '请输入管理员姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWarehouseList({
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

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  searchForm.warehouseName = ''
  searchForm.status = null
  handleSearch()
}

const handleAdd = async () => {
  dialogTitle.value = '新增仓库'
  handleDialogClose()
  dialogVisible.value = true
  try {
    const res = await getNextWarehouseCode()
    if (res.data) {
      warehouseForm.warehouseCode = res.data
    }
  } catch {
    ElMessage.error('获取仓库编码失败')
  }
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑仓库'
  Object.assign(warehouseForm, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除仓库"${row.warehouseName}"吗？若仓库内仍有库存、锁定药品或进行中的盘点，将无法删除。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteWarehouse(row.warehouseId)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  if (!warehouseFormRef.value) return

  await warehouseFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (warehouseForm.warehouseId) {
          await updateWarehouse(warehouseForm)
          ElMessage.success('更新成功')
        } else {
          await saveWarehouse(warehouseForm)
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

const handleDialogClose = () => {
  warehouseFormRef.value?.resetFields()
  Object.assign(warehouseForm, {
    warehouseId: null,
    warehouseCode: '',
    warehouseName: '',
    address: '',
    manager: '',
    phone: '',
    status: 1
  })
}

const handleSizeChange = () => {
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

