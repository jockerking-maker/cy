import React, { useState, useEffect, useCallback } from 'react';
import DataTable from '../components/DataTable';
import { itemsAPI } from '../services/api';
import { useToast } from '../contexts/ToastContext';

const initialForm = {
  title: '',
  description: '',
  category: '',
  status: 'active',
  priority: 'normal',
  due_date: '',
  tags: '',
};

// Categories match backend seed data
const categoryOptions = [
  { value: 'development', label: '开发' },
  { value: 'design', label: '设计' },
  { value: 'marketing', label: '市场' },
  { value: 'research', label: '研究' },
  { value: 'operation', label: '运营' },
];

const statusOptions = [
  { value: 'active', label: '进行中' },
  { value: 'completed', label: '已完成' },
  { value: 'pending', label: '待处理' },
  { value: 'archived', label: '已归档' },
];

const priorityOptions = [
  { value: 'low', label: '低' },
  { value: 'normal', label: '中' },
  { value: 'high', label: '高' },
  { value: 'urgent', label: '紧急' },
];

// Lookup maps for rendering
const statusMap = { active: 'badge-success', completed: 'badge-primary', pending: 'badge-warning', archived: 'badge-secondary' };
const statusLabels = { active: '进行中', completed: '已完成', pending: '待处理', archived: '已归档' };
const priorityLabels = { low: '低', normal: '中', high: '高', urgent: '紧急' };

export default function DataPage() {
  const { success, error: showError } = useToast();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState(null);
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({});
  const [sortField, setSortField] = useState('created_at');
  const [sortDirection, setSortDirection] = useState('desc');
  const [page, setPage] = useState(1);

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [form, setForm] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // Confirm dialog
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Map frontend sort params to backend expected params
      const params = { page, pageSize: 20 };
      if (search) params.search = search;
      if (sortField) params.sort = sortField;
      if (sortDirection) params.order = sortDirection;
      Object.entries(filters).forEach(([k, v]) => { if (v) params[k] = v; });

      const res = await itemsAPI.list(params);
      setData(res.data.list || []);
      setPagination(res.data.pagination);
    } catch (err) {
      setError(err?.message || '获取数据失败');
    } finally {
      setLoading(false);
    }
  }, [page, search, sortField, sortDirection, filters]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const validateForm = () => {
    const errors = {};
    if (!form.title.trim()) errors.title = '请输入标题';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleOpenAdd = () => {
    setEditingItem(null);
    setForm(initialForm);
    setFormErrors({});
    setModalOpen(true);
  };

  const handleOpenEdit = (row) => {
    setEditingItem(row);
    setForm({
      title: row.title || '',
      description: row.description || '',
      category: row.category || '',
      status: row.status || 'active',
      priority: row.priority || 'normal',
      due_date: row.due_date ? row.due_date.split('T')[0] : '',
      tags: row.tags || '',
    });
    setFormErrors({});
    setModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    try {
      // Tags are stored as comma-separated string in the backend
      const payload = { ...form };

      if (editingItem) {
        await itemsAPI.update(editingItem.id, payload);
        success('更新成功');
      } else {
        await itemsAPI.create(payload);
        success('创建成功');
      }
      setModalOpen(false);
      fetchData();
    } catch (err) {
      showError(err?.message || '操作失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteClick = (row) => {
    setDeleteTarget(row);
    setConfirmOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    try {
      await itemsAPI.delete(deleteTarget.id);
      success('删除成功');
      setConfirmOpen(false);
      setDeleteTarget(null);
      fetchData();
    } catch (err) {
      showError(err?.message || '删除失败');
    }
  };

  const handleBatchDelete = async (ids, clearSelection) => {
    try {
      await itemsAPI.batchDelete({ ids });
      success(`已删除 ${ids.length} 项`);
      clearSelection();
      fetchData();
    } catch (err) {
      showError(err?.message || '批量删除失败');
    }
  };

  const columns = [
    { key: 'title', label: '标题' },
    {
      key: 'category',
      label: '分类',
      render: (val) => {
        const opt = categoryOptions.find((o) => o.value === val);
        return <span className="badge badge-info">{opt ? opt.label : val || '-'}</span>;
      },
    },
    {
      key: 'status',
      label: '状态',
      render: (val) => (
        <span className={`badge ${statusMap[val] || 'badge-secondary'}`}>
          {statusLabels[val] || val || '-'}
        </span>
      ),
    },
    {
      key: 'priority',
      label: '优先级',
      render: (val) => priorityLabels[val] || val || '-',
    },
    { key: 'assignee_name', label: '负责人' },
    { key: 'due_date', label: '截止日期', render: (val) => val ? val.split('T')[0] : '-' },
    {
      key: 'tags',
      label: '标签',
      render: (val) =>
        val ? val.split(',').filter(Boolean).map((t, i) => (
          <span key={i} className="tag">{t.trim()}</span>
        )) : null,
    },
    { key: 'created_at', label: '创建时间', sortable: true },
  ];

  const filterConfig = [
    { key: 'category', label: '分类', placeholder: '全部分类', options: categoryOptions },
    { key: 'status', label: '状态', placeholder: '全部状态', options: statusOptions },
    { key: 'priority', label: '优先级', placeholder: '全部优先级', options: priorityOptions },
  ];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">数据管理</h1>
        <p className="page-subtitle">管理系统数据条目</p>
        <div style={{ marginTop: 12 }}>
          <button className="btn btn-primary" onClick={handleOpenAdd}>
            + 添加数据
          </button>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={data}
        loading={loading}
        error={error}
        pagination={pagination}
        onPageChange={(p) => setPage(p)}
        onSortChange={(field, dir) => { setSortField(field); setSortDirection(dir); }}
        onSearch={(val) => { setSearch(val); setPage(1); }}
        onFilter={(vals) => { setFilters(vals); setPage(1); }}
        onEdit={handleOpenEdit}
        onDelete={handleDeleteClick}
        onSelectionChange={() => {}}
        searchPlaceholder="搜索标题..."
        filters={filterConfig}
        batchActions={(ids, clearSelection) => (
          <button className="btn btn-danger btn-sm" onClick={() => handleBatchDelete(ids, clearSelection)}>
            批量删除 ({ids.length})
          </button>
        )}
        emptyText="暂无数据"
        emptySubtext={'点击"添加数据"按钮创建第一条数据'}
      />

      {/* Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">{editingItem ? '编辑数据' : '添加数据'}</span>
              <button className="modal-close" onClick={() => setModalOpen(false)}>&times;</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">标题</label>
                  <input
                    className="form-input"
                    value={form.title}
                    onChange={(e) => setForm({ ...form, title: e.target.value })}
                    placeholder="请输入标题"
                  />
                  {formErrors.title && <div className="form-error">{formErrors.title}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">描述</label>
                  <textarea
                    className="form-textarea"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    placeholder="请输入描述"
                    rows={3}
                  />
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">分类</label>
                    <select
                      className="form-select"
                      value={form.category}
                      onChange={(e) => setForm({ ...form, category: e.target.value })}
                    >
                      <option value="">请选择分类</option>
                      {categoryOptions.map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">状态</label>
                    <select
                      className="form-select"
                      value={form.status}
                      onChange={(e) => setForm({ ...form, status: e.target.value })}
                    >
                      {statusOptions.map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">优先级</label>
                    <select
                      className="form-select"
                      value={form.priority}
                      onChange={(e) => setForm({ ...form, priority: e.target.value })}
                    >
                      {priorityOptions.map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">截止日期</label>
                    <input
                      type="date"
                      className="form-input"
                      value={form.due_date}
                      onChange={(e) => setForm({ ...form, due_date: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">标签</label>
                  <input
                    className="form-input"
                    value={form.tags}
                    onChange={(e) => setForm({ ...form, tags: e.target.value })}
                    placeholder="用逗号分隔多个标签"
                  />
                  <div className="form-help">多个标签用逗号分隔</div>
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

      {/* Confirm Dialog */}
      {confirmOpen && (
        <div className="modal-overlay" onClick={() => setConfirmOpen(false)}>
          <div className="modal-content" style={{ maxWidth: 400 }} onClick={(e) => e.stopPropagation()}>
            <div className="confirm-dialog">
              <div className="confirm-dialog-icon danger">!</div>
              <div className="confirm-dialog-title">确认删除</div>
              <div className="confirm-dialog-message">
                确定要删除"{deleteTarget?.title}"吗？此操作不可恢复。
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
