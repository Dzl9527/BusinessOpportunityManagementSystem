<template>
  <section class="content-panel user-center-panel">
    <div class="user-center-shell">
      <div class="user-center-grid">
        <section class="user-card">
          <div class="user-card-header">
            <div class="user-card-avatar">{{ avatarText }}</div>
            <div>
              <h2>{{ user?.name || '未登录用户' }}</h2>
              <p>{{ roleLabel }}</p>
            </div>
          </div>

          <div class="user-card-meta">
            <div class="meta-item">
              <span>登录状态</span>
              <strong>{{ user ? '已登录' : '未登录' }}</strong>
            </div>
            <div class="meta-item">
              <span>企业微信 ID</span>
              <strong>{{ user?.userId || '-' }}</strong>
            </div>
            <div class="meta-item">
              <span>当前角色</span>
              <strong>{{ roleLabel }}</strong>
            </div>
            <div class="meta-item">
              <span>可见范围</span>
              <strong>{{ permissionSummary }}</strong>
            </div>
          </div>

          <div class="user-card-actions">
            <button class="btn-secondary" type="button" @click="toggleTheme">
              {{ themeButtonText }}
            </button>
            <button class="btn-primary" type="button" @click="logout">
              退出登录
            </button>
          </div>
        </section>

        <section class="user-center-section">
          <div class="section-heading">
            <h3>我的使用说明</h3>
            <p>这里说明当前账号在手机端可以做什么，避免看得见却不能操作的困惑。</p>
          </div>

          <div class="info-list">
            <article class="info-card">
              <strong>工作台</strong>
              <p>查看我的商机摘要、关键提醒和常用入口。</p>
            </article>
            <article class="info-card">
              <strong>商机提报</strong>
              <p>新增商机并补充完整字段，提交后进入我的业务范围。</p>
            </article>
            <article class="info-card">
              <strong>商机列表</strong>
              <p>按关键词、行业、供货省区和业务进度搜索筛选商机。</p>
            </article>
            <article class="info-card">
              <strong>商机看板</strong>
              <p>按阶段查看我有权限访问的商机，适合跟进推进情况。</p>
            </article>
            <article class="info-card">
              <strong>用户中心</strong>
              <p>查看账号资料、权限范围、主题设置，并支持退出登录。</p>
            </article>
          </div>
        </section>
      </div>

      <section class="user-center-section">
        <div class="section-heading">
          <h3>权限说明</h3>
          <p>权限最终以后端校验为准，这里只做可读化展示。</p>
        </div>

        <div class="permission-grid">
          <article class="permission-card">
            <span>查看范围</span>
            <strong>{{ permissionSummary }}</strong>
            <p>{{ viewDescription }}</p>
          </article>
          <article class="permission-card">
            <span>编辑范围</span>
            <strong>{{ editSummary }}</strong>
            <p>{{ editDescription }}</p>
          </article>
          <article class="permission-card">
            <span>登录方式</span>
            <strong>企业微信沙箱 / 真实 SSO 预留</strong>
            <p>当前环境支持沙箱身份切换，后续可接入真实企业微信登录。</p>
          </article>
        </div>
      </section>

      <section v-if="isAdmin" class="user-center-section">
        <div class="section-heading">
          <h3>管理入口</h3>
          <p>管理员可从这里进入用户管理、白名单配置和系统设置中心。</p>
        </div>

        <div class="admin-shortcuts">
          <button class="shortcut-card" type="button" @click="goUsers">
            <strong>用户管理</strong>
            <p>维护角色、启用状态和白名单。</p>
          </button>
          <button class="shortcut-card" type="button" @click="goAdminTools">
            <strong>系统设置</strong>
            <p>集中处理联调、通知偏好、数据备份和演示环境配置。</p>
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<script>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from '../store'

export default {
  setup() {
    const router = useRouter()
    const store = useStore()

    const user = computed(() => store.user.value)
    const isAdmin = computed(() => user.value?.role === 'ADMIN' || user.value?.canViewAll)
    const avatarText = computed(() => (user.value?.name || '我').slice(0, 1))
    const roleLabel = computed(() => {
      if (isAdmin.value) return '管理员'
      if (user.value?.role === 'LEADER') return '领导 / 授权用户'
      return '普通用户'
    })
    const permissionSummary = computed(() => {
      if (isAdmin.value) return '全部商机'
      if (user.value?.role === 'LEADER') return '本人 + 白名单范围商机'
      return '本人商机'
    })
    const editSummary = computed(() => {
      if (isAdmin.value) return '可编辑全部商机'
      if (user.value?.role === 'LEADER') return '可编辑本人和白名单范围商机'
      return '可编辑本人商机'
    })
    const viewDescription = computed(() => {
      if (isAdmin.value) {
        return '包括历史未归属商机、全部用户商机和管理页面可见数据。'
      }
      if (user.value?.role === 'LEADER') {
        return '可查看自己的商机，以及管理员配置到白名单范围内的人员商机。'
      }
      return '默认只能查看自己创建、提报或负责的商机。'
    })
    const editDescription = computed(() => {
      if (isAdmin.value) return '可编辑全部商机，并可使用用户管理和系统设置功能。'
      if (user.value?.role === 'LEADER') return '可编辑自己的商机，以及白名单范围内的商机。'
      return '默认只能编辑自己有权限的商机。'
    })
    const themeButtonText = computed(() => store.theme.value === 'dark' ? '切换为浅色模式' : '切换为深色模式')

    const logout = () => {
      store.logout()
      router.push({ name: 'Login' })
    }

    const toggleTheme = () => {
      store.toggleTheme()
    }

    const goUsers = () => {
      router.push({ name: 'Users' })
    }

    const goAdminTools = () => {
      router.push({ name: 'Settings' })
    }

    return {
      user,
      isAdmin,
      avatarText,
      roleLabel,
      permissionSummary,
      editSummary,
      viewDescription,
      editDescription,
      themeButtonText,
      logout,
      toggleTheme,
      goUsers,
      goAdminTools
    }
  }
}
</script>
