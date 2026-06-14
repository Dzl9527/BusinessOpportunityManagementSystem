import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { API_BASE, useAppStore } from './useAppStore'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<any>(JSON.parse(localStorage.getItem('crm_user') || 'null'))

  const loginWithCode = async (code: string) => {
    const appStore = useAppStore()
    try {
      const response = await axios.get(`${API_BASE}/wecom/auth`, { params: { code } })
      user.value = response.data
      localStorage.setItem('crm_user', JSON.stringify(response.data))
      appStore.showToast(`企业微信免登成功，欢迎您，${response.data.name}！`, 'success')
      return true
    } catch (e) {
      console.error('SSO Code auth failed', e)
      appStore.showToast('企业微信登录验证失败，请使用沙箱模拟登录', 'error')
      return false
    }
  }

  const loginSandbox = (userId = 'zhang_jingli') => {
    return loginWithCode(`mock_code:${userId}`)
  }

  const logout = () => {
    const appStore = useAppStore()
    user.value = null
    localStorage.removeItem('crm_user')
    appStore.showToast('已成功退出系统登录', 'info')
  }

  return { user, loginWithCode, loginSandbox, logout }
})
