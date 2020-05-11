import axios from 'axios';
import * as auth from '@/common/utils/auth';
import store from '@/store'
import { Message } from 'element-ui'


// 创建实例
const instance = axios.create({
  baseURL: '/',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// 拦截请求
instance.interceptors.request.use(
  (config: any) => {
    store.dispatch('app/setLoading', true)
    // const token = auth.getToken();
    // if (token) { // 如果本地存在token，请求时带上
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  }, (error: any) => {
    return Promise.reject(error)
  }
  
);

// 拦截响应
instance.interceptors.response.use((response: any) => {
  // 全局统一处理 Session超时
  if (response.headers.session_time_out === 'timeout') {
    Message({ type: 'error', message: '会话超时：session_time_out' })
  }
  const { code, message } = response.data
  // 处理请求错误
  if (code !== 0) {
    if (message) {
      Message({ type: 'error', message: `错误请求：${message}` })
    }
    return Promise.reject(message)
  }
  store.dispatch('app/setLoading', false)
  return response.data
}, (error: any) => {
  // 请求被拦截被跳到这里
  Message({ type: 'error', message: `被拦截请求：${error.message}` })
  store.dispatch('app/setLoading', false)
  return Promise.reject(error)
})


export default instance;
