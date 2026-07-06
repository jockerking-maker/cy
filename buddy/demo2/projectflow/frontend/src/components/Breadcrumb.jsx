import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const breadcrumbMap = {
  '/dashboard': '仪表盘',
  '/data': '数据管理',
  '/content': '内容管理',
  '/users': '用户管理',
  '/settings': '系统设置',
  '/profile': '个人中心',
};

export default function Breadcrumb() {
  const location = useLocation();
  const pathParts = location.pathname.split('/').filter(Boolean);

  if (pathParts.length === 0 || pathParts[0] === '') return null;

  return (
    <div className="breadcrumb">
      <Link to="/dashboard">首页</Link>
      {pathParts.map((part, index) => {
        const path = '/' + pathParts.slice(0, index + 1).join('/');
        const label = breadcrumbMap[path] || part.charAt(0).toUpperCase() + part.slice(1);
        const isLast = index === pathParts.length - 1;
        return (
          <React.Fragment key={path}>
            <span className="breadcrumb-separator">/</span>
            {isLast ? (
              <span className="breadcrumb-current">{label}</span>
            ) : (
              <Link to={path}>{label}</Link>
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}
