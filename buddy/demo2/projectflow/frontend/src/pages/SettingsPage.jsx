import React, { useState, useEffect } from 'react';
import { settingsAPI } from '../services/api';
import { useToast } from '../contexts/ToastContext';
import DataTable from '../components/DataTable';

const TABS = [
  { key: 'site', label: '站点设置' },
  { key: 'mail', label: '邮件设置' },
  { key: 'cache', label: '缓存管理' },
  { key: 'logs', label: '操作日志' },
];

export default function SettingsPage() {
  const { success, error: showError } = useToast();
  const [activeTab, setActiveTab] = useState('site');

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">系统设置</h1>
        <p className="page-subtitle">管理系统配置和参数</p>
      </div>

      <div className="tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            className={`tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'site' && <SiteSettings onSuccess={success} onError={showError} />}
      {activeTab === 'mail' && <MailSettings onSuccess={success} onError={showError} />}
      {activeTab === 'cache' && <CacheSettings onSuccess={success} onError={showError} />}
      {activeTab === 'logs' && <LogSettings />}
    </div>
  );
}

function SiteSettings({ onSuccess, onError }) {
  const [form, setForm] = useState({
    site_name: '',
    site_description: '',
    site_footer: '',
    maintenance_mode: false,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await settingsAPI.getSite();
        if (res.data) {
          setForm({
            site_name: res.data.site_name || '',
            site_description: res.data.site_description || '',
            site_footer: res.data.site_footer || '',
            maintenance_mode: res.data.maintenance_mode === 'true',
          });
        }
      } catch (err) {
        onError('获取站点设置失败');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [onError]);

  const handleSave = async () => {
    setSaving(true);
    try {
      // Send maintenance_mode as string to match backend storage format
      await settingsAPI.updateSite({
        ...form,
        maintenance_mode: String(form.maintenance_mode),
      });
      onSuccess('站点设置已保存');
    } catch (err) {
      onError(err?.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="card">
        <div className="card-body">
          <div className="loading-center">
            <div className="spinner spinner-lg"></div>
            <span>加载中...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="card-body">
        <div className="form-group">
          <label className="form-label">站点名称</label>
          <input
            className="form-input"
            value={form.site_name}
            onChange={(e) => setForm({ ...form, site_name: e.target.value })}
            placeholder="请输入站点名称"
          />
        </div>
        <div className="form-group">
          <label className="form-label">站点描述</label>
          <textarea
            className="form-textarea"
            value={form.site_description}
            onChange={(e) => setForm({ ...form, site_description: e.target.value })}
            placeholder="请输入站点描述"
            rows={3}
          />
        </div>
        <div className="form-group">
          <label className="form-label">页脚信息</label>
          <input
            className="form-input"
            value={form.site_footer}
            onChange={(e) => setForm({ ...form, site_footer: e.target.value })}
            placeholder="请输入页脚信息"
          />
        </div>
        <div className="form-group">
          <div className="setting-field">
            <div>
              <div className="setting-field-label">维护模式</div>
              <div className="setting-field-desc">开启后仅管理员可访问系统</div>
            </div>
            <label className="toggle-switch">
              <input
                type="checkbox"
                checked={form.maintenance_mode}
                onChange={(e) => setForm({ ...form, maintenance_mode: e.target.checked })}
              />
              <span className="toggle-slider"></span>
            </label>
          </div>
        </div>
        <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
          {saving ? '保存中...' : '保存设置'}
        </button>
      </div>
    </div>
  );
}

function MailSettings({ onSuccess, onError }) {
  const [form, setForm] = useState({
    mail_host: '',
    mail_port: '',
    mail_user: '',
    mail_pass: '',
    mail_from: '',
    mail_encryption: 'tls',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await settingsAPI.getMail();
        if (res.data) {
          setForm({
            mail_host: res.data.mail_host || '',
            mail_port: res.data.mail_port || '',
            mail_user: res.data.mail_user || '',
            mail_pass: res.data.mail_pass || '',
            mail_from: res.data.mail_from || '',
            mail_encryption: res.data.mail_encryption || 'tls',
          });
        }
      } catch (err) {
        onError('获取邮件设置失败');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [onError]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await settingsAPI.updateMail(form);
      onSuccess('邮件设置已保存');
    } catch (err) {
      onError(err?.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleTest = async () => {
    setTesting(true);
    try {
      await settingsAPI.testMail();
      onSuccess('测试邮件发送成功，请检查收件箱');
    } catch (err) {
      onError(err?.message || '测试邮件发送失败');
    } finally {
      setTesting(false);
    }
  };

  if (loading) {
    return (
      <div className="card">
        <div className="card-body">
          <div className="loading-center">
            <div className="spinner spinner-lg"></div>
            <span>加载中...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="card-body">
        <div className="form-row">
          <div className="form-group">
            <label className="form-label">SMTP 服务器</label>
            <input
              className="form-input"
              value={form.mail_host}
              onChange={(e) => setForm({ ...form, mail_host: e.target.value })}
              placeholder="smtp.example.com"
            />
          </div>
          <div className="form-group">
            <label className="form-label">端口</label>
            <input
              className="form-input"
              value={form.mail_port}
              onChange={(e) => setForm({ ...form, mail_port: e.target.value })}
              placeholder="587"
            />
          </div>
        </div>
        <div className="form-row">
          <div className="form-group">
            <label className="form-label">邮箱用户名</label>
            <input
              className="form-input"
              value={form.mail_user}
              onChange={(e) => setForm({ ...form, mail_user: e.target.value })}
              placeholder="user@example.com"
            />
          </div>
          <div className="form-group">
            <label className="form-label">邮箱密码</label>
            <input
              type="password"
              className="form-input"
              value={form.mail_pass}
              onChange={(e) => setForm({ ...form, mail_pass: e.target.value })}
              placeholder="留空则不修改"
            />
            <div className="form-help">显示为 ****** 时留空表示不修改</div>
          </div>
        </div>
        <div className="form-row">
          <div className="form-group">
            <label className="form-label">发件人地址</label>
            <input
              className="form-input"
              value={form.mail_from}
              onChange={(e) => setForm({ ...form, mail_from: e.target.value })}
              placeholder="noreply@example.com"
            />
          </div>
          <div className="form-group">
            <label className="form-label">加密方式</label>
            <select
              className="form-select"
              value={form.mail_encryption}
              onChange={(e) => setForm({ ...form, mail_encryption: e.target.value })}
            >
              <option value="none">无加密</option>
              <option value="tls">TLS</option>
              <option value="ssl">SSL</option>
            </select>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '保存设置'}
          </button>
          <button className="btn btn-ghost" onClick={handleTest} disabled={testing}>
            {testing ? '发送中...' : '测试连接'}
          </button>
        </div>
      </div>
    </div>
  );
}

function CacheSettings({ onSuccess, onError }) {
  const [clearing, setClearing] = useState(false);

  const handleClear = async () => {
    setClearing(true);
    try {
      await settingsAPI.clearCache();
      onSuccess('缓存已清除');
    } catch (err) {
      onError(err?.message || '清除缓存失败');
    } finally {
      setClearing(false);
    }
  };

  return (
    <div className="card">
      <div className="card-body">
        <div style={{ textAlign: 'center', padding: '32px 0' }}>
          <div style={{ fontSize: '3rem', marginBottom: 16, opacity: 0.5 }}>
            \uD83D\uDDD1\uFE0F
          </div>
          <h3 style={{ marginBottom: 8 }}>清除系统缓存</h3>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: 20, fontSize: '0.9rem' }}>
            清除缓存可以释放存储空间，解决一些显示异常问题。
          </p>
          <button
            className="btn btn-primary btn-lg"
            onClick={handleClear}
            disabled={clearing}
          >
            {clearing ? '清除中...' : '立即清除缓存'}
          </button>
        </div>
      </div>
    </div>
  );
}

// Action label map for log display
const ACTION_LABELS = {
  login: '登录',
  logout: '登出',
  create: '创建',
  update: '更新',
  delete: '删除',
  upload: '上传',
  test: '测试',
  clear: '清除',
};

function LogSettings() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState(null);
  const [page, setPage] = useState(1);

  useEffect(() => {
    const fetchLogs = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await settingsAPI.getLogs({ page, pageSize: 30 });
        setLogs(res.data.list || []);
        setPagination(res.data.pagination);
      } catch (err) {
        setError(err?.message || '获取日志失败');
      } finally {
        setLoading(false);
      }
    };
    fetchLogs();
  }, [page]);

  // Columns mapped to actual API response fields
  const logColumns = [
    {
      key: 'created_at',
      label: '时间',
      render: (val) => <span style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>{val}</span>,
    },
    {
      key: 'action',
      label: '操作',
      render: (val) => {
        const labels = { login: '登录', create: '创建', update: '更新', delete: '删除', upload: '上传', test: '测试', clear: '清除' };
        return <span className="badge badge-info">{labels[val] || val || '-'}</span>;
      },
    },
    {
      key: 'description',
      label: '内容',
      render: (val) => <span>{val || '-'}</span>,
    },
    {
      key: 'display_name',
      label: '用户',
      render: (val, row) => <span>{val || row.username || '系统'}</span>,
    },
    {
      key: 'ip_address',
      label: 'IP',
      render: (val) => <span style={{ fontSize: '0.85rem' }}>{val || '-'}</span>,
    },
  ];

  return (
    <div className="card">
      <div className="card-header">
        <span className="card-header-title">操作日志</span>
      </div>
      <div className="card-body" style={{ padding: 0 }}>
        <DataTable
          columns={logColumns}
          data={logs}
          loading={loading}
          error={error}
          pagination={pagination}
          onPageChange={(p) => setPage(p)}
          emptyText="暂无日志记录"
        />
      </div>
    </div>
  );
}
