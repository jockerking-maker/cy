<template>
  <div class="role-list page-shell">
    <div class="page-header">
      <h2>角色管理</h2>
      <p>配置角色与菜单权限</p>
    </div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :model="searchForm" inline>
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable />
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
          新增角色
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
        <el-table-column prop="roleName" label="角色名称" width="200" />
        <el-table-column prop="roleCode" label="角色编码" width="200" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" size="small" @click="handleAuth(row)">权限分配</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="roleFormRef"
        :model="roleForm"
        :rules="roleRules"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" placeholder="请输入角色编码 (如:ADMIN)" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="roleForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog
      v-model="authVisible"
      title="权限分配"
      width="600px"
    >
      <el-tree
        ref="treeRef"
        :data="menuTree"
        :props="{ children: 'children', label: 'label' }"
        node-key="id"
        default-expand-all
        show-checkbox
      />

      <template #footer>
        <el-button @click="authVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAuth">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, saveRole, updateRole, deleteRole, assignPerms, getMenus, getMenusByRoleId } from '@/api/role'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const authVisible = ref(false)
const dialogTitle = ref('新增角色')
const roleFormRef = ref(null)
const treeRef = ref(null)

// 搜索表单
const searchForm = reactive({
  roleName: ''
})

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表格数据
const tableData = ref([])

// 表单数据
const roleForm = reactive({
  roleId: null,
  roleName: '',
  roleCode: '',
  description: ''
})

// 菜单树
const menuTree = ref([])

// 加载菜单数据
const loadMenus = async () => {
  try {
    const res = await getMenus()
    if (res.code === 200) {
      // 直接使用后端返回的菜单数据，不手动修改
      const menus = res.data || []
      
      // 转换为树形结构
      menuTree.value = buildMenuTree(menus)
    }
  } catch (error) {
    console.error('加载菜单失败:', error)
  }
}

// 构建菜单树
const buildMenuTree = (menus) => {
  const menuMap = new Map()
  const roots = []
  
  // 先将所有菜单放入 map
  menus.forEach(menu => {
    menuMap.set(menu.menuId, { ...menu, id: menu.menuId, label: menu.menuName, children: [] })
  })
  
  // 构建树形结构
  menus.forEach(menu => {
    const parent = menuMap.get(menu.parentId)
    if (parent) {
      parent.children.push(menuMap.get(menu.menuId))
    } else {
      roots.push(menuMap.get(menu.menuId))
    }
  })
  
  return roots
}

// 加载角色的菜单权限
const loadRoleMenus = async (roleId) => {
  try {
    const res = await getMenusByRoleId(roleId)
    if (res.code === 200) {
      // 设置选中状态
      if (treeRef.value) {
        treeRef.value.setCheckedKeys(res.data || [])
      }
    }
  } catch (error) {
    console.error('加载角色菜单失败:', error)
  }
}

// 表单验证规则
const roleRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoleList({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      roleName: searchForm.roleName
    })
    tableData.value = res.data || []
    pagination.total = res.total || 0
  } catch {
    ElMessage.error('加载角色列表失败')
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
  searchForm.roleName = ''
  handleSearch()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑角色'
  // 重置表单数据
  roleForm.roleId = row.roleId
  roleForm.roleName = row.roleName
  roleForm.roleCode = row.roleCode
  roleForm.description = row.description
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色"${row.roleName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRole(row.roleId)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 权限分配
const handleAuth = async (row) => {
  // 存储当前角色的 ID
  roleForm.roleId = row.roleId
  // 打开对话框
  authVisible.value = true
  // 加载角色的菜单权限
  await loadRoleMenus(row.roleId)
}

// 提交权限
const submitAuth = async () => {
  const checkedKeys = treeRef.value?.getCheckedKeys() || []
  const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() || []
  const allKeys = [...checkedKeys, ...halfCheckedKeys]
  try {
    await assignPerms({
      roleId: roleForm.roleId,
      permIds: allKeys.map(id => Number(id))
    })
    ElMessage.success('权限分配成功')
    authVisible.value = false
  } catch (error) {
    console.error('权限分配失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!roleFormRef.value) return
  
  await roleFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (roleForm.roleId) {
          await updateRole(roleForm)
          ElMessage.success('更新成功')
        } else {
          await saveRole(roleForm)
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
  roleFormRef.value?.resetFields()
  Object.assign(roleForm, {
    roleId: null,
    roleName: '',
    roleCode: '',
    description: ''
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
  loadMenus()
})
</script>

