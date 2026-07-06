import React, { useState, useEffect } from 'react';
import { dashboardAPI } from '../services/api';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';

const PIE_COLORS = ['#2563eb', '#22c55e', '#f59e0b', '#ef4444', '#9333ea', '#06b6d4'];

// Map API snake_case keys to human-readable labels
const OVERVIEW_LABELS = {
  db_size: '数据库大小',
  db_size_formatted: '数据库大小',
  storage_used: '存储用量',
  storage_used_formatted: '存储用量',
  version: '系统版本',
  environment: '运行环境',
  node_version: 'Node.js 版本',
  sqlite_version: 'SQLite 版本',
  uptime: '运行时长',
  uptime_formatted: '运行时长',
  item_status_counts: '项目状态分布',
};

// Keys to display in the overview card (prefer formatted versions)
const OVERVIEW_DISPLAY_KEYS = [
  'version',
  'environment',
  'node_version',
  'sqlite_version',
  'db_size_formatted',
  'storage_used_formatted',
  'uptime_formatted',
];

// Map action codes to readable labels
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

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [activities, setActivities] = useState([]);
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [statsRes, activitiesRes, overviewRes] = await Promise.all([
          dashboardAPI.stats(),
          dashboardAPI.recentActivities(),
          dashboardAPI.overview(),
        ]);
        setStats(statsRes.data);
        setActivities(activitiesRes.data || []);
        setOverview(overviewRes.data);
      } catch (err) {
        setError(err?.message || '获取仪表盘数据失败');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return (
      <div>
        <div className="page-header">
          <h1 className="page-title">仪表盘</h1>
          <p className="page-subtitle">系统概览和数据统计</p>
        </div>
        <div className="stats-grid">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="stats-card">
              <div className="skeleton skeleton-card" style={{ width: 48, height: 48 }} />
              <div style={{ flex: 1 }}>
                <div className="skeleton skeleton-text" style={{ width: '60%' }} />
                <div className="skeleton skeleton-text" style={{ width: '40%', height: 24 }} />
              </div>
            </div>
          ))}
        </div>
        <div className="dashboard-grid">
          <div className="card">
            <div className="card-header"><div className="skeleton skeleton-text" style={{ width: 120 }} /></div>
            <div className="card-body"><div className="skeleton" style={{ height: 280 }} /></div>
          </div>
          <div className="card">
            <div className="card-header"><div className="skeleton skeleton-text" style={{ width: 120 }} /></div>
            <div className="card-body"><div className="skeleton" style={{ height: 280 }} /></div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <div className="page-header">
          <h1 className="page-title">仪表盘</h1>
          <p className="page-subtitle">系统概览和数据统计</p>
        </div>
        <div className="error-state">
          <div className="error-state-icon">!</div>
          <div className="error-state-text">{error}</div>
          <button className="btn btn-primary" onClick={() => window.location.reload()}>
            重新加载
          </button>
        </div>
      </div>
    );
  }

  // Prepare chart data from API arrays: [{ category: 'dev', count: 6 }, ...]
  const categoryData = (stats?.items_by_category || []).map((item) => ({
    name: item.category || '未分类',
    value: item.count,
  }));

  const roleData = (stats?.users_by_role || []).map((item) => ({
    name: item.role === 'admin' ? '管理员' : '普通用户',
    value: item.count,
  }));

  const statCards = [
    { label: '用户总数', value: stats?.total_users ?? 0, icon: '\uD83D\uDC65', color: 'blue' },
    { label: '数据条目', value: stats?.total_items ?? 0, icon: '\uD83D\uDCCB', color: 'green' },
    { label: '文章总数', value: stats?.total_articles ?? 0, icon: '\uD83D\uDCDD', color: 'orange' },
    { label: '总访问量', value: stats?.total_views ?? 0, icon: '\uD83D\uDCC8', color: 'purple' },
  ];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">仪表盘</h1>
        <p className="page-subtitle">系统概览和数据统计</p>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid">
        {statCards.map((card, idx) => (
          <div key={idx} className="stats-card">
            <div className={`stats-card-icon ${card.color}`}>
              <span>{card.icon}</span>
            </div>
            <div className="stats-card-info">
              <div className="stats-card-label">{card.label}</div>
              <div className="stats-card-value">{card.value?.toLocaleString()}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="dashboard-grid">
        <div className="card">
          <div className="card-header">
            <span className="card-header-title">分类数据统计</span>
          </div>
          <div className="card-body">
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={categoryData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#2563eb" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty-state">
                <div className="empty-state-text">暂无分类数据</div>
              </div>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <span className="card-header-title">用户角色分布</span>
          </div>
          <div className="card-body">
            {roleData.length > 0 ? (
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={roleData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={90}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  >
                    {roleData.map((_, idx) => (
                      <Cell key={idx} fill={PIE_COLORS[idx % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty-state">
                <div className="empty-state-text">暂无用户数据</div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Recent Activities & Overview */}
      <div className="dashboard-grid">
        <div className="card">
          <div className="card-header">
            <span className="card-header-title">最近活动</span>
          </div>
          <div className="card-body" style={{ padding: 0 }}>
            {activities.length > 0 ? (
              <div style={{ maxHeight: 320, overflowY: 'auto' }}>
                {activities.map((act, idx) => (
                  <div
                    key={act.id || idx}
                    style={{
                      padding: '12px 20px',
                      borderBottom: idx < activities.length - 1 ? '1px solid var(--color-border)' : 'none',
                      fontSize: '0.9rem',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>{act.description || ACTION_LABELS[act.action] || act.action}</span>
                      <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                        {act.created_at}
                      </span>
                    </div>
                    {(act.display_name || act.username) && (
                      <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: 2 }}>
                        {'\u2014'} {act.display_name || act.username}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-state" style={{ padding: '32px 20px' }}>
                <div className="empty-state-text">暂无最近活动</div>
              </div>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <span className="card-header-title">系统概览</span>
          </div>
          <div className="card-body">
            {overview ? (
              <div>
                {OVERVIEW_DISPLAY_KEYS.map((key) => {
                  if (overview[key] === undefined) return null;
                  return (
                    <div
                      key={key}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        padding: '10px 0',
                        borderBottom: '1px solid var(--color-border)',
                        fontSize: '0.9rem',
                      }}
                    >
                      <span style={{ color: 'var(--color-text-secondary)' }}>
                        {OVERVIEW_LABELS[key] || key}
                      </span>
                      <span style={{ fontWeight: 500 }}>{String(overview[key])}</span>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="empty-state">
                <div className="empty-state-text">暂无概览信息</div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
