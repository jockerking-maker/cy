import React, { useState, useRef, useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Breadcrumb from './Breadcrumb';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const closeSidebar = () => setSidebarOpen(false);

  const navItems = [
    { path: '/dashboard', label: '仪表盘', icon: '\u2302' },
    { path: '/data', label: '数据管理', icon: '\u2630' },
    { path: '/content', label: '内容管理', icon: '\u270E' },
    { path: '/users', label: '用户管理', icon: '\u263C' },
    { path: '/settings', label: '系统设置', icon: '\u2699' },
  ];

  const userInitial = user?.display_name
    ? user.display_name.charAt(0).toUpperCase()
    : user?.username
      ? user.username.charAt(0).toUpperCase()
      : '?';

  return (
    <div className="app-layout">
      {/* Mobile overlay */}
      <div
        className={`sidebar-overlay ${sidebarOpen ? 'open' : ''}`}
        onClick={closeSidebar}
      />

      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon">PF</div>
          <span>ProjectFlow</span>
        </div>
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `sidebar-nav-item ${isActive ? 'active' : ''}`
              }
              onClick={closeSidebar}
            >
              <span>{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          ProjectFlow v1.0.0
        </div>
      </aside>

      {/* Main wrapper */}
      <div className="main-wrapper">
        {/* Topbar */}
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="hamburger-btn"
              onClick={() => setSidebarOpen(!sidebarOpen)}
              aria-label="切换菜单"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 6h18M3 12h18M3 18h18" />
              </svg>
            </button>
            <Breadcrumb />
          </div>
          <div className="topbar-right">
            <div className="user-dropdown" ref={dropdownRef}>
              <button
                className="user-dropdown-trigger"
                onClick={() => setDropdownOpen(!dropdownOpen)}
              >
                <div className="user-avatar">{userInitial}</div>
                <span>{user?.display_name || user?.username || '用户'}</span>
              </button>
              <div className={`user-dropdown-menu ${dropdownOpen ? 'open' : ''}`}>
                <button
                  className="user-dropdown-item"
                  onClick={() => { setDropdownOpen(false); navigate('/profile'); }}
                >
                  \uD83D\uDC64 个人信息
                </button>
                <button
                  className="user-dropdown-item"
                  onClick={() => { setDropdownOpen(false); navigate('/settings'); }}
                >
                  \u2699 系统设置
                </button>
                <div className="dropdown-divider" />
                <button
                  className="user-dropdown-item danger"
                  onClick={handleLogout}
                >
                  \u2192 退出登录
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
