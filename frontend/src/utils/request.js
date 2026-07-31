import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import Cookies from 'js-cookie';

const request = axios.create({
  baseURL: import.meta.env.VITE_APP_API_BASE_URL || '/api',
  // 默认 10s；导出等接口可在调用处单独加大
  timeout: 30000,
});

request.interceptors.request.use(
  (config) => {
    const token = Cookies.get('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data;
    }

    const res = response.data;

    if (res.code === 200) {
      return res;
    } else if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录');
      Cookies.remove('token');
      router.push('/login');
      return Promise.reject(new Error('未授权'));
    } else {
      ElMessage.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message || '请求失败'));
    }
  },
  (error) => {
    console.error('API Error:', error);
    if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
      ElMessage.error('请求超时，服务器较忙，请稍后重试或刷新页面');
    } else if (!error.response) {
      ElMessage.error('网络异常，请检查网络后重试');
    } else {
      ElMessage.error(error.message || '网络错误');
    }
    return Promise.reject(error);
  }
);

export default request;
