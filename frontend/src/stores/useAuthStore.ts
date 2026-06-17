import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { API_BASE, useAppStore } from './useAppStore'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<any>(JSON.parse(localStorage.getItem('crm_user') || 'null'))

  const loginWithCode = async (code: string) => {
    const appStore = useAppStore()
    try {
      const response = await axios.post(`${API_BASE}/feishu/auth`, { code })
      const { user: userData, token } = response.data
      // Merge token into user object so Axios interceptor can read it
      const mergedUser = { ...userData, token }
      user.value = mergedUser
      localStorage.setItem('crm_user', JSON.stringify(mergedUser))
      appStore.showToast(`登录成功，欢迎您，${userData.name}！`, 'success')
      return true
    } catch (e) {
      console.error('SSO Code auth failed', e)
      appStore.showToast('登录验证失败，请使用沙箱模拟登录', 'error')
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
