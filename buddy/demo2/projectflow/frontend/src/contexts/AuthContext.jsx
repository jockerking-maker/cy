import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authAPI } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const isAuthenticated = !!token && !!user;

  // Fetch profile on mount if token exists
  useEffect(() => {
    const loadUser = async () => {
      const storedToken = localStorage.getItem('token');
      if (!storedToken) {
        setLoading(false);
        return;
      }
      try {
        const res = await authAPI.getProfile();
        setUser(res.data);
        setToken(storedToken);
      } catch (err) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    loadUser();
  }, []);

  const login = useCallback(async (username, password) => {
    setError(null);
    try {
      const res = await authAPI.login({ username, password });
      const { token: newToken, user: userData } = res.data;
      localStorage.setItem('token', newToken);
      localStorage.setItem('user', JSON.stringify(userData));
      setToken(newToken);
      setUser(userData);
      return res;
    } catch (err) {
      const msg = err?.message || err?.data?.message || '登录失败，请检查用户名和密码';
      setError(msg);
      throw new Error(msg);
    }
  }, []);

  const register = useCallback(async (username, email, password) => {
    setError(null);
    try {
      const res = await authAPI.register({ username, email, password });
      return res;
    } catch (err) {
      const msg = err?.message || err?.data?.message || '注册失败，请重试';
      setError(msg);
      throw new Error(msg);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
    setError(null);
  }, []);

  const updateProfile = useCallback(async (data) => {
    setError(null);
    try {
      const res = await authAPI.updateProfile(data);
      setUser(res.data);
      localStorage.setItem('user', JSON.stringify(res.data));
      return res;
    } catch (err) {
      const msg = err?.message || '更新失败';
      setError(msg);
      throw new Error(msg);
    }
  }, []);

  const changePassword = useCallback(async (oldPassword, newPassword) => {
    setError(null);
    try {
      const res = await authAPI.changePassword({ old_password: oldPassword, new_password: newPassword });
      return res;
    } catch (err) {
      const msg = err?.message || '修改密码失败';
      setError(msg);
      throw new Error(msg);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  const value = {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    login,
    register,
    logout,
    updateProfile,
    changePassword,
    clearError,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
