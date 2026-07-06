import React, { useState, useEffect, useCallback } from 'react';
import DataTable from '../components/DataTable';
import { usersAPI } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';

// Role options mapped to backend role IDs
const roleOptions = [
  { value: 1, label: '管理员' },
  { value: 2, label: '普通用户' },
];

const roleLabels = { admin: '管理员', user: '普通用户' };
const roleColors = { admin: 'badge-danger', user: 'badge-secondary' };

const initialForm = {
  username: '',
  email: '',
  password: '',
  display_name: '',
  phone: '',
  role_id: 2,
};

export default function UsersPage() {
  const { user: currentUser } = useAuth();
  const { success, error: showError } = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);

  // Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form, setForm] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // Role change
  const [roleModal, setRoleModal] = useState(false);
  const [roleTarget, setRoleTarget] = useState(null);
  const [newRoleId, setNewRoleId] = useState(2);

  // Delete confirm
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, pageSize: 20 };
      if (search) params.search = search;
      const res = await usersAPI.list(params);
      setUsers(res.data.list || []);
      setPagination(res.data.pagination);
    } catch (err) {
      setError(err?.message || '获取用户列表失败');
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const validateForm = () => {
    const errors = {};
    if (!form.username.trim()) errors.username = '请输入用户名';
    if (!form.email.trim()) errors.email = '请输入邮箱';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = '邮箱格式不正确';
    if (!editingUser && !form.password) errors.password = '请输入密码';
    else if (!editingUser && form.password.length < 6) errors.password = '密码至少6个字符';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleOpenAdd = () => {
    setEditingUser(null);
    setForm(initialForm);
    setFormErrors({});
    setModalOpen(true);
  };

  const handleOpenEdit = (row) => {
    setEditingUser(row);
    setForm({
      username: row.username || '',
      email: row.email || '',
      password: '',
      display_name: row.display_name || '',
      phone: row.phone || '',
      role_id: row.role_id || 2,
    });
    setFormErrors({});
    setModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    try {
      const payload = { ...form };
      // Don't send empty password on edit
      if (editingUser && !payload.password) {
        delete payload.password;
      }

      if (editingUser) {
        await usersAPI.update(editingUser.id, payload);
        success('用户已更新');
      } else {
        await usersAPI.create(payload);
        success('用户已创建');
      }
      setModalOpen(false);
      fetchUsers();
    } catch (err) {
      showError(err?.message || '操作失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleOpenRoleChange = (row) => {
    setRoleTarget(row);
    setNewRoleId(row.role_id || 2);
    setRoleModal(true);
  };

  const handleRoleChange = async () => {
    if (!roleTarget || !newRoleId) return;
    try {
      // Backend expects { role_id: number }
      await usersAPI.updateRole(roleTarget.id, { role_id: newRoleId });
      success('角色已更新');
      setRoleModal(false);
      setRoleTarget(null);
      fetchUsers();
    } catch (err) {
      showError(err?.message || '角色更新失败');
    }
  };

  const handleDeleteClick = (row) => {
    setDeleteTarget(row);
    setConfirmOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    try {
      await usersAPI.delete(deleteTarget.id);
      success('用户已删除');
      setConfirmOpen(false);
      setDeleteTarget(null);
      fetchUsers();
    } catch (err) {
      showError(err?.message || '删除失败');
    }
  };

  const columns = [
    {
      key: 'username',
      label: '用户名',
      render: (val, row) => (
        <span>
          {val}
          {row.id === currentUser?.id && (
            <span className="badge badge-primary" style={{ marginLeft: 6 }}>当前用户</span>
          )}
        </span>
      ),
    },
    { key: 'email', label: '邮箱' },
    { key: 'display_name', label: '显示名称' },
    { key: 'phone', label: '手机号' },
    {
      key: 'role_name',
      label: '角色',
      render: (val) => (
        <span className={`badge ${roleColors[val] || 'badge-secondary'}`}>
          {roleLabels[val] || val || '-'}
        </span>
      ),
    },
    {
      key: 'status',
      label: '状态',
      render: (val) => (
        <span className={`badge ${val === 'active' ? 'badge-success' : 'badge-warning'}`}>
          {val === 'active' ? '正常' : '禁用'}
        </span>
      ),
    },
    { key: 'created_at', label: '注册时间', sortable: true },
    {
      key: 'actions',
      label: '角色操作',
      sortable: false,
      render: (_, row) => (
        <button className="action-btn" onClick={() => handleOpenRoleChange(row)}>
          变更角色
        </button>
      ),
    },
  ];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">用户管理</h1>
        <p className="page-subtitle">管理系统用户</p>
        <div style={{ marginTop: 12 }}>
          <button className="btn btn-primary" onClick={handleOpenAdd}>
            + 添加用户
          </button>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={users}
        loading={loading}
        error={error}
        pagination={pagination}
        onPageChange={(p) => setPage(p)}
        onSearch={(val) => { setSearch(val); setPage(1); }}
        onEdit={handleOpenEdit}
        onDelete={handleDeleteClick}
        searchPlaceholder="搜索用户名或邮箱..."
        emptyText="暂无用户"
      />

      {/* Add/Edit Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">{editingUser ? '编辑用户' : '添加用户'}</span>
              <button className="modal-close" onClick={() => setModalOpen(false)}>&times;</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">用户名</label>
                    <input
                      className="form-input"
                      value={form.username}
                      onChange={(e) => setForm({ ...form, username: e.target.value })}
                      placeholder="用户名"
                    />
                    {formErrors.username && <div className="form-error">{formErrors.username}</div>}
                  </div>
                  <div className="form-group">
                    <label className="form-label">邮箱</label>
                    <input
                      className="form-input"
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                      placeholder="邮箱"
                    />
                    {formErrors.email && <div className="form-error">{formErrors.email}</div>}
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">显示名称</label>
                    <input
                      className="form-input"
                      value={form.display_name}
                      onChange={(e) => setForm({ ...form, display_name: e.target.value })}
                      placeholder="显示名称"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">手机号</label>
                    <input
                      className="form-input"
                      value={form.phone}
                      onChange={(e) => setForm({ ...form, phone: e.target.value })}
                      placeholder="手机号"
                    />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">密码{editingUser ? '（留空不修改）' : ''}</label>
                    <input
                      type="password"
                      className="form-input"
                      value={form.password}
                      onChange={(e) => setForm({ ...form, password: e.target.value })}
                      placeholder={editingUser ? '留空则不修改密码' : '至少6位密码'}
                    />
                    {formErrors.password && <div className="form-error">{formErrors.password}</div>}
                  </div>
                  <div className="form-group">
                    <label className="form-label">角色</label>
                    <select
                      className="form-select"
                      value={form.role_id}
                      onChange={(e) => setForm({ ...form, role_id: parseInt(e.target.value, 10) })}
                    >
                      {roleOptions.map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={() => setModalOpen(false)}>
                  取消
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? '保存中...' : '保存'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Role Change Modal */}
      {roleModal && (
        <div className="modal-overlay" onClick={() => setRoleModal(false)}>
          <div className="modal-content" style={{ maxWidth: 400 }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">变更用户角色</span>
              <button className="modal-close" onClick={() => setRoleModal(false)}>&times;</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">用户：{roleTarget?.username}</label>
                <select
                  className="form-select"
                  value={newRoleId}
                  onChange={(e) => setNewRoleId(parseInt(e.target.value, 10))}
                >
                  {roleOptions.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setRoleModal(false)}>
                取消
              </button>
              <button className="btn btn-primary" onClick={handleRoleChange}>
                确认变更
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirm */}
      {confirmOpen && (
        <div className="modal-overlay" onClick={() => setConfirmOpen(false)}>
          <div className="modal-content" style={{ maxWidth: 400 }} onClick={(e) => e.stopPropagation()}>
            <div className="confirm-dialog">
              <div className="confirm-dialog-icon danger">!</div>
              <div className="confirm-dialog-title">确认删除</div>
              <div className="confirm-dialog-message">
                确定要删除用户"{deleteTarget?.username}"吗？此操作不可恢复。
              </div>
              <div className="confirm-dialog-actions">
                <button className="btn btn-ghost" onClick={() => setConfirmOpen(false)}>
                  取消
                </button>
                <button className="btn btn-danger" onClick={handleDeleteConfirm}>
                  确认删除
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
