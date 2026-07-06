<template>
  <div class="stock-check-list page-shell">
    <div class="page-header">
      <h2>库存盘点</h2>
      <p>发起盘点任务并核对账实</p>
    </div>
    <el-card shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="仓库">
          <el-select v-model="searchForm.warehouseId" placeholder="请选择仓库" clearable style="width: 180px;">
            <el-option
              v-for="item in warehouseList"
              :key="item.warehouseId"
              :label="item.warehouseName"
              :value="item.warehouseId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px;">
            <el-option label="盘点中" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="已取消" :value="2" />
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
          新建盘点单
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="checkNo" label="盘点单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库名称" width="180" />
        <el-table-column prop="checkTime" label="盘点日期" width="180">
          <template #default="{ row }">
            {{ row.checkTime ? row.checkTime.replace('T', ' ').substring(0, 16) : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'">
              {{ row.status === 0 ? '盘点中' : row.status === 1 ? '已完成' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.replace('T', ' ').substring(0, 19) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">明细</el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              size="small"
              @click="handleComplete(row)"
            >
              完成盘点
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="warning"
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <el-button
              v-if="row.status !== 1"
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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
      v-model="addDialogVisible"
      title="新建盘点单"
      width="500px"
      @close="handleAddDialogClose"
    >
      <el-form
        ref="addFormRef"
        :model="addForm"
        :rules="addRules"
        label-width="100px"
      >
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="addForm.warehouseId" placeholder="请选择仓库" style="width: 100%;">
            <el-option
              v-for="item in enabledWarehouseList"
              :key="item.warehouseId"
              :label="item.warehouseName"
              :value="item.warehouseId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="addForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddSubmit" :loading="addSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="盘点明细"
      width="900px"
    >
      <el-descriptions :column="3" border style="margin-bottom: 16px;">
        <el-descriptions-item label="盘点单号">{{ detailInfo.checkNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailInfo.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailInfo.status === 0 ? 'warning' : detailInfo.status === 1 ? 'success' : 'info'">
            {{ detailInfo.status === 0 ? '盘点中' : detailInfo.status === 1 ? '已完成' : '已取消' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-table
        :data="detailList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="drugName" label="药品名称" width="160" />
        <el-table-column prop="batchNo" label="批号" width="120" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="systemNum" label="系统数量" width="100" />
        <el-table-column label="实际数量" width="140">
          <template #default="{ row }">
            <el-input-number
              v-if="detailStatus === 0"
              v-model="row.actualNum"
              :min="0"
              size="small"
              controls-position="right"
            />
            <span v-else>{{ row.actualNum }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异数量" width="100">
          <template #default="{ row }">
            <span :class="{ 'diff-negative': (row.actualNum - row.systemNum) < 0, 'diff-positive': (row.actualNum - row.systemNum) > 0 }">
              {{ row.actualNum - row.systemNum }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="handleWay" label="处理方式" width="120">
          <template #default="{ row }">
            <el-select v-if="detailStatus === 0 && (row.actualNum - row.systemNum) !== 0" v-model="row.handleWay" size="small" placeholder="选择" style="width: 100%">
              <el-option label="调整库存" value="调整库存" />
              <el-option label="报损" value="报损" />
              <el-option label="待处理" value="待处理" />
            </el-select>
            <span v-else>{{ row.handleWay || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="detailStatus === 0"
          type="primary"
          @click="handleSaveDetail"
          :loading="saveDetailLoading"
        >
          保存并完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStockCheckList, getStockCheckDetail, saveStockCheck, completeStockCheck, cancelStockCheck, deleteStockCheck } from '@/api/stockCheck'
import { getWarehouseList } from '@/api/warehouse'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const addSubmitLoading = ref(false)
const saveDetailLoading = ref(false)
const addDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const addFormRef = ref(null)
const currentCheckId = ref(null)
const detailStatus = ref(0)

const searchForm = reactive({
  warehouseId: null,
  status: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const warehouseList = ref([])
const enabledWarehouseList = ref([])
const detailList = ref([])
const detailInfo = reactive({
  checkNo: '',
  warehouseName: '',
  status: 0
})

const addForm = reactive({
  warehouseId: null,
  remark: ''
})

const addRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStockCheckList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...searchForm
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('加载盘点列表失败')
  } finally {
    loading.value = false
  }
}

const loadWarehouseList = async () => {
  try {
    const [allRes, enabledRes] = await Promise.all([
      getWarehouseList(),
      getWarehouseList({ status: 1 })
    ])
    warehouseList.value = allRes.data || []
    enabledWarehouseList.value = enabledRes.data || []
  } catch (error) {
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadData()
}

const handleReset = () => {
  searchForm.warehouseId = null
  searchForm.status = null
  handleSearch()
}

const handleAdd = () => {
  addDialogVisible.value = true
}

const handleAddSubmit = async () => {
  if (!addFormRef.value) return

  await addFormRef.value.validate(async (valid) => {
    if (valid) {
      addSubmitLoading.value = true
      try {
        await saveStockCheck({
          ...addForm,
          createUserId: userStore.userInfo?.userId || 1
        })
        ElMessage.success('创建成功')
        addDialogVisible.value = false
        loadData()
      } catch (error) {
        ElMessage.error('创建盘点单失败')
      } finally {
        addSubmitLoading.value = false
      }
    }
  })
}

const handleAddDialogClose = () => {
  addFormRef.value?.resetFields()
  Object.assign(addForm, {
    warehouseId: null,
    remark: ''
  })
}

const handleViewDetail = async (row) => {
  currentCheckId.value = row.checkId
  detailStatus.value = row.status
  try {
    const res = await getStockCheckDetail(row.checkId)
    const data = res.data || {}
    detailInfo.checkNo = data.checkNo || ''
    detailInfo.warehouseName = data.warehouseName || row.warehouseName || ''
    detailInfo.status = data.status

    detailList.value = (data.items || []).map(item => ({
      ...item,
      actualNum: item.actualNum ?? item.systemNum
    }))
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载盘点明细失败')
  }
}

const handleSaveDetail = async () => {
  saveDetailLoading.value = true
  try {
    await completeStockCheck({
      checkId: currentCheckId.value,
      items: detailList.value.map(item => ({
        itemId: item.itemId,
        actualNum: item.actualNum,
        handleWay: item.handleWay || '',
        handleRemark: item.handleRemark || ''
      }))
    })
    ElMessage.success('盘点完成')
    detailDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saveDetailLoading.value = false
  }
}

const handleComplete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要完成该盘点单吗？完成后将不可修改。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await completeStockCheck({ checkId: row.checkId })
    ElMessage.success('盘点完成')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消该盘点单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelStockCheck(row.checkId)
    ElMessage.success('已取消')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该盘点单吗？删除后不可恢复！', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteStockCheck(row.checkId)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
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
.diff-negative {
  color: var(--danger);
  font-weight: bold;
}

.diff-positive {
  color: var(--accent);
  font-weight: bold;
}
</style>
