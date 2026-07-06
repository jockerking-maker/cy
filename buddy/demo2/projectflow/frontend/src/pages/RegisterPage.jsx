import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function RegisterPage() {
  const { register, error, clearError } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const validate = () => {
    const errors = {};
    if (!form.username.trim()) errors.username = '请输入用户名';
    else if (form.username.trim().length < 3) errors.username = '用户名至少3个字符';
    else if (form.username.trim().length > 30) errors.username = '用户名不超过30个字符';

    if (!form.email.trim()) errors.email = '请输入邮箱';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = '邮箱格式不正确';

    if (!form.password) errors.password = '请输入密码';
    else if (form.password.length < 6) errors.password = '密码至少6个字符';

    if (!form.confirmPassword) errors.confirmPassword = '请确认密码';
    else if (form.password !== form.confirmPassword) errors.confirmPassword = '两次密码不一致';

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    if (fieldErrors[field]) {
      setFieldErrors({ ...fieldErrors, [field]: '' });
    }
    if (localError) setLocalError('');
    clearError();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLocalError('');
    clearError();

    if (!validate()) return;

    setLoading(true);
    try {
      await register(form.username, form.email, form.password);
      navigate('/login', { state: { registerSuccess: true } });
    } catch (err) {
      setLocalError(err?.message || '注册失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const displayError = localError || error;

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <div className="login-brand-icon">PF</div>
          <h1>注册账号</h1>
          <p>加入 ProjectFlow 项目管理平台</p>
        </div>

        {displayError && (
          <div className="login-error">{displayError}</div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">用户名</label>
            <input
              type="text"
              className="form-input"
              placeholder="请输入用户名（3-30个字符）"
              value={form.username}
              onChange={handleChange('username')}
            />
            {fieldErrors.username && <div className="form-error">{fieldErrors.username}</div>}
          </div>
          <div className="form-group">
            <label className="form-label">邮箱</label>
            <input
              type="email"
              className="form-input"
              placeholder="请输入邮箱地址"
              value={form.email}
              onChange={handleChange('email')}
            />
            {fieldErrors.email && <div className="form-error">{fieldErrors.email}</div>}
          </div>
          <div className="form-group">
            <label className="form-label">密码</label>
            <input
              type="password"
              className="form-input"
              placeholder="请输入密码（至少6位）"
              value={form.password}
              onChange={handleChange('password')}
            />
            {fieldErrors.password && <div className="form-error">{fieldErrors.password}</div>}
          </div>
          <div className="form-group">
            <label className="form-label">确认密码</label>
            <input
              type="password"
              className="form-input"
              placeholder="请再次输入密码"
              value={form.confirmPassword}
              onChange={handleChange('confirmPassword')}
            />
            {fieldErrors.confirmPassword && (
              <div className="form-error">{fieldErrors.confirmPassword}</div>
            )}
          </div>
          <button
            type="submit"
            className="btn btn-primary btn-block btn-lg"
            disabled={loading}
          >
            {loading ? <><span className="spinner"></span> 注册中...</> : '注 册'}
          </button>
        </form>

        <div className="login-footer">
          已有账号？<Link to="/login">返回登录</Link>
        </div>
      </div>
    </div>
  );
}
