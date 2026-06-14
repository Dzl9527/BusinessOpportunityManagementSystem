import { defineStore } from 'pinia'
import { ref } from 'vue'

export const API_HOST = window.location.hostname || 'localhost'
export const API_BASE = `http://${API_HOST}:8080/api`

export const useAppStore = defineStore('app', () => {
  const toastState = ref({
    message: '',
    type: 'info',
    show: false
  })

  const theme = ref(localStorage.getItem('crm-theme') || 'light')

  function showToast(message: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') {
    toastState.value.message = message
    toastState.value.type = type
    toastState.value.show = true
    setTimeout(() => {
      toastState.value.show = false
    }, 3000)
  }

  function toggleTheme() {
    const newTheme = theme.value === 'dark' ? 'light' : 'dark'
    theme.value = newTheme
    document.documentElement.setAttribute('data-theme', newTheme)
    localStorage.setItem('crm-theme', newTheme)
  }

  return { toastState, theme, showToast, toggleTheme }
})
