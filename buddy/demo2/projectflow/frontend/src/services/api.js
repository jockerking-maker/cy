import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    const payload = error.response && error.response.data;
    const message = (payload && payload.message) || error.message || '请求失败';
    return Promise.reject({ message, data: payload, status: error.response && error.response.status });
  }
);

// ===== Auth =====
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getProfile: () => api.get('/auth/profile'),
  updateProfile: (data) => api.put('/auth/profile', data),
  changePassword: (data) => api.put('/auth/password', data),
};

// ===== Users (Admin) =====
export const usersAPI = {
  list: (params) => api.get('/users', { params }),
  getById: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`),
  updateRole: (id, data) => api.put(`/users/${id}/role`, data),
};

// ===== Data Items =====
export const itemsAPI = {
  list: (params) => api.get('/items', { params }),
  getById: (id) => api.get(`/items/${id}`),
  create: (data) => api.post('/items', data),
  update: (id, data) => api.put(`/items/${id}`, data),
  delete: (id) => api.delete(`/items/${id}`),
  batchDelete: (data) => api.post('/items/batch-delete', data),
};

// ===== Articles =====
export const articlesAPI = {
  list: (params) => api.get('/articles', { params }),
  getById: (id) => api.get(`/articles/${id}`),
  create: (data) => api.post('/articles', data),
  update: (id, data) => api.put(`/articles/${id}`, data),
  delete: (id) => api.delete(`/articles/${id}`),
};

// ===== Upload =====
export const uploadAPI = {
  image: (formData) =>
    api.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  file: (formData) =>
    api.post('/upload/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};

// ===== Dashboard =====
export const dashboardAPI = {
  stats: () => api.get('/dashboard/stats'),
  recentActivities: () => api.get('/dashboard/recent-activities'),
  overview: () => api.get('/dashboard/overview'),
};

// ===== Settings =====
export const settingsAPI = {
  getSite: () => api.get('/settings/site'),
  updateSite: (data) => api.put('/settings/site', data),
  getMail: () => api.get('/settings/mail'),
  updateMail: (data) => api.put('/settings/mail', data),
  testMail: () => api.post('/settings/mail/test'),
  clearCache: () => api.post('/settings/cache/clear'),
  getLogs: (params) => api.get('/settings/logs', { params }),
};

export default api;
