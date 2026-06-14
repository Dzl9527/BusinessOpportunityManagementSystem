import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/useAuthStore'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true, title: '工作台' }
  },
  {
    path: '/kanban',
    name: 'Kanban',
    component: () => import('../views/Kanban.vue'),
    meta: { requiresAuth: true, title: '商机看板' }
  },
  {
    path: '/list',
    name: 'List',
    component: () => import('../views/List.vue'),
    meta: { requiresAuth: true, title: '商机列表' }
  },
  {
    path: '/submit',
    name: 'Submit',
    component: () => import('../views/Submit.vue'),
    meta: { requiresAuth: true, title: '商机提报' }
  },
  {
    path: '/profile',
    name: 'UserCenter',
    component: () => import('../views/UserCenter.vue'),
    meta: { requiresAuth: true, title: '用户中心' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, title: '系统设置' }
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('../views/Users.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, title: '用户管理' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false, title: '企业微信登录' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // Set window title
  if (to.meta && to.meta.title) {
    document.title = `${to.meta.title} | 商机宝 CRM`
  }

  // Auth Guard
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!authStore.user) {
      next({ name: 'Login' })
    } else if (to.meta.requiresAdmin && !(authStore.user.role === 'ADMIN' || authStore.user.canViewAll)) {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
  } else {
    // Redirect logged in user from login to dashboard
    if (to.name === 'Login' && authStore.user) {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
  }
})

export default router
