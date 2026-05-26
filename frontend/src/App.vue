<template>
  <div class="app-container" v-if="user">
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-brand">
          <div class="sidebar-logo">
            <span class="sidebar-logo-dot"></span>
          </div>
          <div>
            <span class="sidebar-title">商机宝 CRM</span>
            <span class="sidebar-subtitle">Business Workspace</span>
          </div>
        </div>
        <button class="sidebar-collapse-btn" type="button" aria-label="控制台菜单">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="13 17 18 12 13 7"/><polyline points="6 17 11 12 6 7"/></svg>
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item nav-item-workbench">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="7" height="7" x="3" y="3" rx="1.5"/><rect width="7" height="7" x="14" y="3" rx="1.5"/><rect width="7" height="7" x="3" y="14" rx="1.5"/><rect width="7" height="7" x="14" y="14" rx="1.5"/></svg>
          <span>工作台</span>
        </router-link>
        <router-link to="/submit" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14"/><path d="M5 12h14"/><rect width="18" height="18" x="3" y="3" rx="2"/></svg>
          <span>商机提报</span>
        </router-link>
        <router-link to="/list" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" x2="21" y1="6" y2="6"/><line x1="8" x2="21" y1="12" y2="12"/><line x1="8" x2="21" y1="18" y2="18"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/></svg>
          <span>商机列表</span>
        </router-link>
        <router-link to="/kanban" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M7 7h3"/><path d="M14 7h3"/><path d="M7 12h10"/><path d="M7 17h6"/></svg>
          <span>商机看板</span>
        </router-link>
        <router-link to="/profile" class="nav-item">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21a8 8 0 0 0-16 0"/><circle cx="12" cy="7" r="4"/></svg>
          <span>用户中心</span>
        </router-link>
        <template v-if="isAdmin">
          <div class="sidebar-nav-divider">
            <span>管理</span>
          </div>
          <router-link to="/settings" class="nav-item nav-item-admin">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.1a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
            <span>系统设置</span>
          </router-link>
        </template>
      </nav>

      <div class="sidebar-footer">
        <div class="avatar">{{ userInitial }}</div>
        <div class="user-info">
          <span class="user-name">{{ user.name }}</span>
          <span class="user-role">{{ userRoleLabel }}</span>
        </div>
        <button class="btn-icon sidebar-logout" @click="handleLogout" title="退出登录">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-log-out"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>
        </button>
      </div>
    </aside>

    <div class="main-wrapper">
      <header class="header">
        <div class="header-title-section">
          <h1>{{ currentRouteTitle }}</h1>
          <p>{{ currentRouteDescription }}</p>
        </div>
        <div class="header-actions">
          <button class="btn-icon header-ghost-btn" @click="handleToggleTheme" :title="theme === 'dark' ? '切回浅色' : '切至深色'">
            <svg v-if="theme === 'light'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-sun"><circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-moon"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>
          </button>
          <button v-if="isAdmin" class="btn-icon header-ghost-btn" @click="goToAdminTools" title="系统设置">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.1a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
          </button>
          <button class="btn-primary" @click="triggerNewOpp" title="新增提报" v-if="showNewOppButton">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-plus"><path d="M5 12h14"/><path d="M12 5v14"/></svg>
            <span>新建商机</span>
          </button>
        </div>
      </header>

      <div class="workspace-body">
        <div class="workspace-main">
          <router-view></router-view>
        </div>

        <aside class="workspace-rail">
          <div class="rail-profile-card">
            <div class="rail-profile-top">
              <div class="rail-profile-avatar">{{ userInitial }}</div>
              <div>
                <strong>{{ user.name }}</strong>
                <span>{{ userRoleLabel }}</span>
              </div>
            </div>
            <h3>{{ greeting }}，{{ user.name }}！</h3>
            <p>今天继续盯住关键商机，把跟进动作收得更稳一些。</p>
          </div>

          <div class="rail-stats-stack">
            <article class="rail-stat-card">
              <span>我的商机</span>
              <strong>{{ sideStats.myCount }}</strong>
            </article>
            <article class="rail-stat-card">
              <span>活跃商机</span>
              <strong>{{ sideStats.activeCount }}</strong>
            </article>
            <article class="rail-stat-card">
              <span>商机加权总额</span>
              <strong>{{ sideStats.pipelineValue }}</strong>
            </article>
            <article class="rail-stat-card">
              <span>赢单率</span>
              <strong>{{ sideStats.winRate }}</strong>
            </article>
          </div>

          <div class="rail-quick-card">
            <span>快捷入口</span>
            <button class="rail-link-btn" type="button" @click="goToSubmit">发起商机提报</button>
            <button class="rail-link-btn" type="button" @click="goToWorkbench">查看工作台</button>
            <button class="rail-link-btn" type="button" @click="goToKanban">进入商机看板</button>
            <button class="rail-link-btn" type="button" @click="goToList">查看商机列表</button>
            <button class="rail-link-btn" type="button" @click="goToProfile">打开用户中心</button>
            <button v-if="isAdmin" class="rail-link-btn rail-link-btn-admin" type="button" @click="goToAdminTools">打开系统设置</button>
          </div>
        </aside>
      </div>
    </div>
  </div>

  <div v-else>
    <router-view></router-view>
  </div>

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
    const routeDescriptions = {
      Dashboard: '查看我的商机概览、待跟进事项和关键业务指标。',
      Submit: '发起商机提报，按手机端流程补齐业务字段。',
      List: '通过折叠筛选和搜索快速定位目标商机。',
      Kanban: '按阶段推进商机，随时掌握当前项目状态。',
      UserCenter: '查看个人资料、权限范围与登录状态。',
      Users: '维护企业微信用户、角色和白名单范围。',
      Settings: '统一处理系统联调、用户权限、通知和数据维护。'
    }

    const currentRouteTitle = computed(() => {
      return route.meta ? route.meta.title : '加载中'
    })

    const currentRouteDescription = computed(() => {
      return routeDescriptions[route.name] || '保持视图、数据和跟进动作都在同一处完成。'
    })

    const showNewOppButton = computed(() => {
      return ['Kanban', 'List'].includes(route.name)
    })

    const isAdmin = computed(() => {
      return store.user.value?.role === 'ADMIN' || store.user.value?.canViewAll
    })

    const userInitial = computed(() => (store.user.value?.name || '我').slice(0, 1))

    const userRoleLabel = computed(() => {
      if (isAdmin.value) return '管理员'
      if (store.user.value?.role === 'LEADER') return '领导 / 授权用户'
      return '普通用户'
    })

    const greeting = computed(() => {
      const hour = new Date().getHours()
      if (hour < 12) return '上午好'
      if (hour < 18) return '下午好'
      return '晚上好'
    })

    const formatCompactCurrency = (value) => {
      const num = Number(value || 0)
      if (num >= 100000000) return `￥${(num / 100000000).toFixed(2)}亿`
      if (num >= 10000) return `￥${(num / 10000).toFixed(1)}万`
      return `￥${num.toFixed(0)}`
    }

    const sideStats = computed(() => ({
      myCount: store.opportunities.value?.length || 0,
      activeCount: store.metrics.value?.activeCount || 0,
      pipelineValue: formatCompactCurrency(store.metrics.value?.totalPipeline || 0),
      winRate: `${Number(store.metrics.value?.winRate || 0).toFixed(1)}%`
    }))

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

    const goToAdminTools = () => {
      router.push({ name: 'Settings' })
    }

    const goToWorkbench = () => {
      router.push({ name: 'Dashboard' })
    }

    const goToSubmit = () => {
      router.push({ name: 'Submit' })
    }

    const goToKanban = () => {
      router.push({ name: 'Kanban' })
    }

    const goToList = () => {
      router.push({ name: 'List' })
    }

    const goToProfile = () => {
      router.push({ name: 'UserCenter' })
    }

    onMounted(() => {
      document.documentElement.setAttribute('data-theme', store.theme.value)
      if (window.lucide) {
        window.lucide.createIcons()
      }
      if (store.user.value) {
        store.fetchMetrics()
        if (!store.opportunities.value?.length) {
          store.fetchMyOpportunities()
        }
      }
    })

    watch(route, () => {
      setTimeout(() => {
        if (window.lucide) {
          window.lucide.createIcons()
        }
      }, 50)
      if (store.user.value) {
        store.fetchMetrics()
      }
    })

    return {
      user: store.user,
      theme: store.theme,
      toast: toastState,
      currentRouteTitle,
      currentRouteDescription,
      showNewOppButton,
      isAdmin,
      userInitial,
      userRoleLabel,
      greeting,
      sideStats,
      handleLogout,
      handleToggleTheme,
      triggerNewOpp,
      goToAdminTools,
      goToWorkbench,
      goToSubmit,
      goToKanban,
      goToList,
      goToProfile
    }
  }
}
</script>
