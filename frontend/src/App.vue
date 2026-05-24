<template>
  <div class="app-container" v-if="user">
    <!-- Sidebar navigation -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-trending-up"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>
        </div>
        <span class="sidebar-title">商机宝 WeCom</span>
      </div>
      
      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-layout-dashboard"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>
          <span>仪表盘</span>
        </router-link>
        <router-link to="/kanban" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-kanban-square"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M8 7v7"/><path d="M12 7v10"/><path d="M16 7v4"/></svg>
          <span>商机看板</span>
        </router-link>
        <router-link to="/list" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-list"><line x1="8" x2="21" y1="6" y2="6"/><line x1="8" x2="21" y1="12" y2="12"/><line x1="8" x2="21" y1="18" y2="18"/><line x1="3" x2="3.01" y1="6" y2="6"/><line x1="3" x2="3.01" y1="12" y2="12"/><line x1="3" x2="3.01" y1="18" y2="18"/></svg>
          <span>商机列表</span>
        </router-link>
        <router-link to="/submit" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14"/><path d="M5 12h14"/><path d="M4 4h16v16H4z"/></svg>
          <span>商机提报</span>
        </router-link>
        <router-link to="/users" class="nav-item" v-if="isAdmin">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          <span>用户权限</span>
        </router-link>
        <router-link to="/settings" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-settings"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.1a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
          <span>系统设置</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="avatar">{{ user.avatar }}</div>
        <div class="user-info">
          <span class="user-name">{{ user.name }}</span>
          <span class="user-role">{{ user.role }}</span>
        </div>
        <button class="btn-icon" @click="handleLogout" title="退出登录" style="border: none; background: transparent; margin-left: auto; color: rgba(255,255,255,0.4)">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-log-out"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
        </button>
      </div>
    </aside>

    <!-- Main Wrapper -->
    <div class="main-wrapper">
      <!-- Header -->
      <header class="header">
        <div class="header-title-section">
          <h1>{{ currentRouteTitle }}</h1>
        </div>
        <div class="header-actions">
          <button class="btn-icon" @click="handleToggleTheme" :title="theme === 'dark' ? '切回浅色' : '切至深色'">
            <svg v-if="theme === 'light'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-sun"><circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-moon"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>
          </button>
          <button class="btn-primary" @click="triggerNewOpp" title="新增提报" v-if="showNewOppButton">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-plus"><path d="M5 12h14"/><path d="M12 5v14"/></svg>
            <span>新增提报</span>
          </button>
        </div>
      </header>

      <!-- Panel content viewport -->
      <router-view></router-view>
    </div>
  </div>
  
  <div v-else>
    <!-- Simple login layout -->
    <router-view></router-view>
  </div>

  <!-- Global toast notification -->
  <div class="toast-container" id="toast-container">
    <div v-if="toast.show" :class="`toast toast-${toast.type}`">
      <svg v-if="toast.type === 'success'" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-check-circle"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
      <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-alert-circle"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
      <span class="toast-msg">{{ toast.message }}</span>
    </div>
  </div>
</template>

<script>
import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore, toastState } from './store'

export default {
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()

    const currentRouteTitle = computed(() => {
      return route.meta ? route.meta.title : '加载中'
    })

    const showNewOppButton = computed(() => {
      // Show add opportunity button on Kanban and List panels
      return ['Kanban', 'List', 'Submit'].includes(route.name)
    })

    const isAdmin = computed(() => {
      return store.user.value?.role === 'ADMIN' || store.user.value?.canViewAll
    })

    const handleLogout = () => {
      store.logout()
      router.push({ name: 'Login' })
    }

    const handleToggleTheme = () => {
      store.toggleTheme()
    }

    const triggerNewOpp = () => {
      router.push({ name: 'Submit' })
    }

    onMounted(() => {
      // Sync initial theme
      document.documentElement.setAttribute('data-theme', store.theme.value)
      
      // Update Lucide SVG replacements
      if (window.lucide) {
        window.lucide.createIcons()
      }
    })

    watch(route, () => {
      // Re-trigger Lucide icons parsing when navigation occurs
      setTimeout(() => {
        if (window.lucide) {
          window.lucide.createIcons()
        }
      }, 50)
    })

    return {
      user: store.user,
      theme: store.theme,
      toast: toastState,
      currentRouteTitle,
      showNewOppButton,
      isAdmin,
      handleLogout,
      handleToggleTheme,
      triggerNewOpp
    }
  }
}
</script>
