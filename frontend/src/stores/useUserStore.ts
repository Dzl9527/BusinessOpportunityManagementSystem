import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { API_BASE, useAppStore } from './useAppStore'
import { useAuthStore } from './useAuthStore'

export const useUserStore = defineStore('user', () => {
  const contacts = ref<any[]>([])
  const users = ref<any[]>([])

  const fetchContacts = async () => {
    try {
      const response = await axios.get(`${API_BASE}/users`)
      contacts.value = response.data
      users.value = response.data
    } catch (e) {
      console.error('Failed to fetch contacts', e)
      throw e
    }
  }

  const fetchUsers = async () => {
    const appStore = useAppStore()
    try {
      const response = await axios.get(`${API_BASE}/users`)
      users.value = response.data
    } catch (e) {
      console.error('Failed to fetch users', e)
      appStore.showToast('获取用户列表失败', 'error')
    }
  }

  const syncFeishuUsers = async () => {
    const appStore = useAppStore()
    const authStore = useAuthStore()
    try {
      const response = await axios.post(`${API_BASE}/users/sync-platform`, null, {
        params: {
          adminUserId: authStore.user?.userId,
          adminName: authStore.user?.name
        }
      })
      users.value = response.data
      contacts.value = response.data
      appStore.showToast('飞书用户同步完成', 'success')
    } catch (e) {
      console.error('Failed to sync users', e)
      appStore.showToast('飞书用户同步失败', 'error')
    }
  }

  const updateUserRole = async (platformUserId: string, payload: any) => {
    await axios.put(`${API_BASE}/users/${platformUserId}/role`, payload)
    await fetchUsers()
  }

  const updateUserEnabled = async (platformUserId: string, enabled: boolean) => {
    await axios.put(`${API_BASE}/users/${platformUserId}/enabled`, { enabled })
    await fetchUsers()
  }

  const fetchVisibilityRules = async (platformUserId: string) => {
    const response = await axios.get(`${API_BASE}/users/${platformUserId}/visibility-rules`)
    return response.data
  }

  const saveVisibilityRules = async (platformUserId: string, visibleUserIds: string[]) => {
    const appStore = useAppStore()
    const response = await axios.put(`${API_BASE}/users/${platformUserId}/visibility-rules`, { visibleUserIds })
    appStore.showToast('白名单已保存', 'success')
    return response.data
  }

  return {
    contacts,
    users,
    fetchContacts,
    fetchUsers,
    syncFeishuUsers,
    updateUserRole,
    updateUserEnabled,
    fetchVisibilityRules,
    saveVisibilityRules
  }
})
