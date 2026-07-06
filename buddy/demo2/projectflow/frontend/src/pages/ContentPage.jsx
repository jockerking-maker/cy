import React, { useState, useEffect, useCallback } from 'react';
import DataTable from '../components/DataTable';
import RichTextEditor from '../components/RichTextEditor';
import { articlesAPI, uploadAPI } from '../services/api';
import { useToast } from '../contexts/ToastContext';

const categoryOptions = [
  { value: '技术', label: '技术' },
  { value: '产品', label: '产品' },
  { value: '设计', label: '设计' },
  { value: '运营', label: '运营' },
  { value: '新闻', label: '新闻' },
  { value: '其他', label: '其他' },
];

export default function ContentPage() {
  const { success, error: showError } = useToast();
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);

  // Editor state
  const [editorMode, setEditorMode] = useState(false);
  const [editingArticle, setEditingArticle] = useState(null);
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('');
  const [tags, setTags] = useState('');
  const [content, setContent] = useState('');
  const [coverImage, setCoverImage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Confirm
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const fetchArticles = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, pageSize: 20 };
      if (search) params.search = search;
      const res = await articlesAPI.list(params);
      setArticles(res.data.list || []);
      setPagination(res.data.pagination);
    } catch (err) {
      setError(err?.message || '获取文章列表失败');
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => {
    fetchArticles();
  }, [fetchArticles]);

  const resetEditor = () => {
    setTitle('');
    setCategory('');
    setTags('');
    setContent('');
    setCoverImage('');
    setEditingArticle(null);
    setEditorMode(false);
  };

  const handleNewArticle = () => {
    resetEditor();
    setEditorMode(true);
  };

  const handleEdit = (row) => {
    setEditingArticle(row);
    setTitle(row.title || '');
    setCategory(row.category || '');
    setTags(Array.isArray(row.tags) ? row.tags.join(', ') : (row.tags || ''));
    setContent(row.content || '');
    setCoverImage(row.cover_image || '');
    setEditorMode(true);
  };

  const handleCoverUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await uploadAPI.image(formData);
      setCoverImage(res.data?.url || res.data?.path || URL.createObjectURL(file));
    } catch (err) {
      showError('封面上传失败');
    }
  };

  const handleSave = async (status = 'draft') => {
    if (!title.trim()) {
      showError('请输入文章标题');
      return;
    }
    if (!category) {
      showError('请选择分类');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        title: title.trim(),
        category,
        content,
        // Tags stored as comma-separated string in backend
        tags: tags ? tags.split(',').map((t) => t.trim()).filter(Boolean).join(',') : '',
        cover_image: coverImage,
        status,
      };

      if (editingArticle) {
        await articlesAPI.update(editingArticle.id, payload);
        success('文章已更新');
      } else {
        await articlesAPI.create(payload);
        success('文章已创建');
      }
      resetEditor();
      fetchArticles();
    } catch (err) {
      showError(err?.message || '保存失败');
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
      await articlesAPI.delete(deleteTarget.id);
      success('文章已删除');
      setConfirmOpen(false);
      setDeleteTarget(null);
      fetchArticles();
    } catch (err) {
      showError(err?.message || '删除失败');
    }
  };

  const columns = [
    {
      key: 'cover_image',
      label: '封面',
      sortable: false,
      render: (val) =>
        val ? (
          <img src={val} alt="封面" style={{ width: 48, height: 32, objectFit: 'cover', borderRadius: 4 }} />
        ) : (
          <span style={{ color: 'var(--color-text-muted)' }}>无</span>
        ),
    },
    { key: 'title', label: '标题' },
    {
      key: 'category',
      label: '分类',
      render: (val) => <span className="badge badge-info">{val}</span>,
    },
    {
      key: 'status',
      label: '状态',
      render: (val) => {
        const m = { draft: 'badge-warning', published: 'badge-success', archived: 'badge-secondary' };
        const labels = { draft: '草稿', published: '已发布', archived: '已归档' };
        return <span className={`badge ${m[val] || 'badge-secondary'}`}>{labels[val] || val}</span>;
      },
    },
    {
      key: 'tags',
      label: '标签',
      render: (val) =>
        val ? (Array.isArray(val) ? val : val.split(',')).map((t, i) => (
          <span key={i} className="tag">{t.trim()}</span>
        )) : null,
    },
    { key: 'created_at', label: '创建时间', sortable: true },
  ];

  // Editor view
  if (editorMode) {
    return (
      <div>
        <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <h1 className="page-title">{editingArticle ? '编辑文章' : '新建文章'}</h1>
            <p className="page-subtitle">{editingArticle ? '修改文章内容' : '创建一篇新文章'}</p>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-ghost" onClick={resetEditor}>
              返回列表
            </button>
            <button
              className="btn btn-ghost"
              onClick={() => handleSave('draft')}
              disabled={submitting}
            >
              保存草稿
            </button>
            <button
              className="btn btn-primary"
              onClick={() => handleSave('published')}
              disabled={submitting}
            >
              {submitting ? '发布中...' : '发布'}
            </button>
          </div>
        </div>

        <div className="card" style={{ marginBottom: 20 }}>
          <div className="card-body">
            <div className="form-group">
              <label className="form-label">文章标题</label>
              <input
                className="form-input"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="请输入文章标题"
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">分类</label>
                <select
                  className="form-select"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                >
                  <option value="">请选择分类</option>
                  {categoryOptions.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">标签</label>
                <input
                  className="form-input"
                  value={tags}
                  onChange={(e) => setTags(e.target.value)}
                  placeholder="用逗号分隔多个标签"
                />
                <div className="form-help">多个标签用逗号分隔</div>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">封面图片</label>
              <div>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleCoverUpload}
                  style={{ fontSize: '0.85rem' }}
                />
              </div>
              {coverImage && (
                <div className="image-preview">
                  <img src={coverImage} alt="封面预览" />
                  <button className="image-preview-remove" onClick={() => setCoverImage('')}>
                    &times;
                  </button>
                </div>
              )}
            </div>
            <div className="form-group">
              <label className="form-label">文章内容</label>
              <RichTextEditor value={content} onChange={setContent} />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // List view
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">内容管理</h1>
        <p className="page-subtitle">管理文章内容</p>
        <div style={{ marginTop: 12 }}>
          <button className="btn btn-primary" onClick={handleNewArticle}>
            + 新建文章
          </button>
        </div>
      </div>

      <DataTable
        columns={columns}
        data={articles}
        loading={loading}
        error={error}
        pagination={pagination}
        onPageChange={(p) => setPage(p)}
        onSearch={(val) => { setSearch(val); setPage(1); }}
        onEdit={handleEdit}
        onDelete={handleDeleteClick}
        searchPlaceholder="搜索文章标题..."
        emptyText="暂无文章"
        emptySubtext={'点击"新建文章"开始写作'}
      />

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
