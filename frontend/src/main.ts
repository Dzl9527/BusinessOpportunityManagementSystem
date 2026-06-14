import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/theme.css'

import { createPinia } from 'pinia'
import axios from 'axios'
import { useAuthStore } from './stores/useAuthStore'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

// Configure Axios Interceptor
axios.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.user && authStore.user.token) {
    config.headers.Authorization = `Bearer ${authStore.user.token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

app.use(router)
app.mount('#app')
