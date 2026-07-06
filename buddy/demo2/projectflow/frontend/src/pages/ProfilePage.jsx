import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';

export default function ProfilePage() {
  const { user, updateProfile, changePassword } = useAuth();
  const { success, error: showError } = useToast();

  // Profile form
  const [profileForm, setProfileForm] = useState({
    display_name: user?.display_name || '',
    email: user?.email || '',
    phone: user?.phone || '',
  });
  const [profileErrors, setProfileErrors] = useState({});
  const [profileLoading, setProfileLoading] = useState(false);

  // Password form
  const [passwordForm, setPasswordForm] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [passwordErrors, setPasswordErrors] = useState({});
  const [passwordLoading, setPasswordLoading] = useState(false);

  const userInitial = user?.display_name
    ? user.display_name.charAt(0).toUpperCase()
    : user?.username
      ? user.username.charAt(0).toUpperCase()
      : '?';

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    const errors = {};
    if (!profileForm.email.trim()) errors.email = '请输入邮箱';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profileForm.email)) errors.email = '邮箱格式不正确';
    setProfileErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setProfileLoading(true);
    try {
      await updateProfile(profileForm);
      success('个人信息已更新');
    } catch (err) {
      showError(err?.message || '更新失败');
    } finally {
      setProfileLoading(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    const errors = {};
    if (!passwordForm.oldPassword) errors.oldPassword = '请输入当前密码';
    if (!passwordForm.newPassword) errors.newPassword = '请输入新密码';
    else if (passwordForm.newPassword.length < 6) errors.newPassword = '新密码至少6个字符';
    if (!passwordForm.confirmPassword) errors.confirmPassword = '请确认新密码';
    else if (passwordForm.newPassword !== passwordForm.confirmPassword) errors.confirmPassword = '两次密码不一致';
    setPasswordErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setPasswordLoading(true);
    try {
      await changePassword(passwordForm.oldPassword, passwordForm.newPassword);
      success('密码已修改');
      setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      showError(err?.message || '修改密码失败');
    } finally {
      setPasswordLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">个人中心</h1>
        <p className="page-subtitle">管理您的个人信息和密码</p>
      </div>

      <div className="profile-grid">
        {/* Profile Info */}
        <div className="card">
          <div className="card-header">
            <span className="card-header-title">个人信息</span>
          </div>
          <div className="card-body">
            <div className="avatar-upload">
              <div className="avatar-preview">
                {user?.avatar ? (
                  <img src={user.avatar} alt="avatar" />
                ) : (
                  userInitial
                )}
              </div>
              <div>
                <div style={{ fontWeight: 600, fontSize: '1rem' }}>{user?.display_name || user?.username}</div>
                <div style={{ color: 'var(--color-text-secondary)', fontSize: '0.85rem' }}>
                  {user?.role_name === 'admin' ? '管理员' : '普通用户'}
                </div>
              </div>
            </div>

            <form onSubmit={handleProfileSubmit}>
              <div className="form-group">
                <label className="form-label">用户名</label>
                <input className="form-input" value={user?.username || ''} disabled />
                <div className="form-help">用户名不可修改</div>
              </div>
              <div className="form-group">
                <label className="form-label">显示名称</label>
                <input
                  className="form-input"
                  value={profileForm.display_name}
                  onChange={(e) => setProfileForm({ ...profileForm, display_name: e.target.value })}
                  placeholder="显示名称"
                />
              </div>
              <div className="form-group">
                <label className="form-label">邮箱</label>
                <input
                  className="form-input"
                  value={profileForm.email}
                  onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                  placeholder="邮箱地址"
                />
                {profileErrors.email && <div className="form-error">{profileErrors.email}</div>}
              </div>
              <div className="form-group">
                <label className="form-label">手机号</label>
                <input
                  className="form-input"
                  value={profileForm.phone}
                  onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                  placeholder="手机号"
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={profileLoading}>
                {profileLoading ? '保存中...' : '保存修改'}
              </button>
            </form>
          </div>
        </div>

        {/* Change Password */}
        <div className="card">
          <div className="card-header">
            <span className="card-header-title">修改密码</span>
          </div>
          <div className="card-body">
            <form onSubmit={handlePasswordSubmit}>
              <div className="form-group">
                <label className="form-label">当前密码</label>
                <input
                  type="password"
                  className="form-input"
                  value={passwordForm.oldPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, oldPassword: e.target.value })}
                  placeholder="请输入当前密码"
                />
                {passwordErrors.oldPassword && <div className="form-error">{passwordErrors.oldPassword}</div>}
              </div>
              <div className="form-group">
                <label className="form-label">新密码</label>
                <input
                  type="password"
                  className="form-input"
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                  placeholder="请输入新密码（至少6位）"
                />
                {passwordErrors.newPassword && <div className="form-error">{passwordErrors.newPassword}</div>}
              </div>
              <div className="form-group">
                <label className="form-label">确认新密码</label>
                <input
                  type="password"
                  className="form-input"
                  value={passwordForm.confirmPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                  placeholder="请再次输入新密码"
                />
                {passwordErrors.confirmPassword && (
                  <div className="form-error">{passwordErrors.confirmPassword}</div>
                )}
              </div>
              <button type="submit" className="btn btn-primary" disabled={passwordLoading}>
                {passwordLoading ? '修改中...' : '修改密码'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
