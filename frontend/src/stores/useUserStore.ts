import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { API_BASE, useAppStore } from './useAppStore'

export const useUserStore = defineStore('user', () => {
  const contacts = ref<any[]>([])
  const users = ref<any[]>([])

  const fetchContacts = async () => {
    try {
      const response = await axios.get(`${API_BASE}/wecom/contacts`)
      contacts.value = response.data
    } catch (e) {
      console.error('Failed to fetch contacts', e)
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

  const syncWeComUsers = async () => {
    const appStore = useAppStore()
    try {
      const response = await axios.post(`${API_BASE}/users/sync-wecom`)
      users.value = response.data
      appStore.showToast('企业微信用户同步完成', 'success')
    } catch (e) {
      console.error('Failed to sync users', e)
      appStore.showToast('企业微信用户同步失败', 'error')
    }
  }

  const updateUserRole = async (wecomUserId: string, payload: any) => {
    await axios.put(`${API_BASE}/users/${wecomUserId}/role`, payload)
    await fetchUsers()
  }

  const updateUserEnabled = async (wecomUserId: string, enabled: boolean) => {
    await axios.put(`${API_BASE}/users/${wecomUserId}/enabled`, { enabled })
    await fetchUsers()
  }

  const fetchVisibilityRules = async (wecomUserId: string) => {
    const response = await axios.get(`${API_BASE}/users/${wecomUserId}/visibility-rules`)
    return response.data
  }

  const saveVisibilityRules = async (wecomUserId: string, visibleUserIds: string[]) => {
    const appStore = useAppStore()
    const response = await axios.put(`${API_BASE}/users/${wecomUserId}/visibility-rules`, { visibleUserIds })
    appStore.showToast('白名单已保存', 'success')
    return response.data
  }

  return {
    contacts,
    users,
    fetchContacts,
    fetchUsers,
    syncWeComUsers,
    updateUserRole,
    updateUserEnabled,
    fetchVisibilityRules,
    saveVisibilityRules
  }
})
