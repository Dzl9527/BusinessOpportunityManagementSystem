<template>
  <section class="content-panel users-panel">
    <div style="display: flex; justify-content: flex-end; margin-bottom: 16px;">
      <button class="btn-primary" @click="syncUsers">同步企业微信用户</button>
    </div>

    <div class="admin-mobile-layout">
      <section class="admin-mobile-panel">
        <div class="panel-heading">
          <h3>用户列表</h3>
          <p>先选中一个用户，再配置角色、状态和白名单。</p>
        </div>

        <div class="admin-user-list">
          <button
            v-for="item in users"
            :key="item.wecomUserId"
            type="button"
            class="admin-user-card"
            :class="{ active: selectedUser?.wecomUserId === item.wecomUserId }"
            @click="selectUser(item)"
          >
            <div class="admin-user-card-top">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.departmentName || '未同步部门' }}</p>
              </div>
              <span class="role-chip">{{ roleText(item.role) }}</span>
            </div>
            <div class="admin-user-card-meta">
              <span>{{ item.wecomUserId }}</span>
              <span>{{ item.position || '未填写职位' }}</span>
            </div>
            <div class="admin-user-card-status">
              <span :class="['status-pill', item.enabled ? 'enabled' : 'disabled']">{{ item.enabled ? '启用中' : '已禁用' }}</span>
              <span class="link-text">点按配置</span>
            </div>
          </button>
        </div>
      </section>

      <section class="admin-mobile-panel detail-panel">
        <div class="panel-heading">
          <h3>权限配置</h3>
          <p v-if="!selectedUser">请先从左侧选择一个用户。</p>
          <p v-else>当前正在配置 {{ selectedUser.name }} 的角色、启用状态和白名单。</p>
        </div>

        <template v-if="selectedUser">
          <div class="selected-user-summary">
            <div>
              <strong>{{ selectedUser.name }}</strong>
              <span>{{ selectedUser.wecomUserId }}</span>
            </div>
            <span :class="['status-pill', selectedUser.enabled ? 'enabled' : 'disabled']">{{ selectedUser.enabled ? '启用中' : '已禁用' }}</span>
          </div>

          <div class="admin-form-stack">
            <div class="admin-setting-card">
              <span>角色</span>
              <select class="form-control" :value="selectedUser.role" @change="changeRole(selectedUser, $event.target.value)">
                <option value="USER">普通用户</option>
                <option value="LEADER">领导</option>
                <option value="ADMIN">管理员</option>
              </select>
            </div>

            <div class="admin-setting-card">
              <span>账号状态</span>
              <button class="status-toggle full-width" :class="{ enabled: selectedUser.enabled }" @click="toggleEnabled(selectedUser)">
                {{ selectedUser.enabled ? '当前已启用，点此禁用' : '当前已禁用，点此启用' }}
              </button>
            </div>
          </div>

          <div class="admin-setting-card whitelist-card">
            <div class="whitelist-header">
              <div>
                <span>白名单范围</span>
                <p>控制该用户可查看和编辑的人员商机范围。</p>
              </div>
            </div>

            <div class="whitelist-chip-list">
              <label v-for="item in selectableUsers" :key="item.wecomUserId" class="whitelist-chip" :class="{ active: visibleUserIds.includes(item.wecomUserId) }">
                <input type="checkbox" :value="item.wecomUserId" v-model="visibleUserIds">
                <strong>{{ item.name }}</strong>
                <small>{{ item.wecomUserId }}</small>
              </label>
            </div>

            <button class="btn-primary save-rules-btn" @click="saveRules">保存白名单</button>
          </div>
        </template>
      </section>
    </div>
  </section>
</template>

<script>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/useAuthStore'
import { useOppStore } from '../stores/useOppStore'
import { useAppStore } from '../stores/useAppStore'
import { useUserStore } from '../stores/useUserStore'
import { useMetricsStore } from '../stores/useMetricsStore'

export default {
  setup() {
        const authStore = useAuthStore()
    const oppStore = useOppStore()
    const appStore = useAppStore()
    const userStore = useUserStore()
    const metricsStore = useMetricsStore()
const selectedUser = ref(null)
    const visibleUserIds = ref([])
    const users = computed(() => userStore.users)
    const selectableUsers = computed(() => users.value.filter(item => item.wecomUserId !== selectedUser.value?.wecomUserId))

    const roleText = (role) => {
      if (role === 'ADMIN') return '管理员'
      if (role === 'LEADER') return '领导'
      return '普通用户'
    }

    const syncUsers = async () => {
      await userStore.syncWeComUsers()
    }

    const changeRole = async (user, role) => {
      await userStore.updateUserRole(user.wecomUserId, { role, canViewAll: role === 'ADMIN' })
      const latest = userStore.users.find(item => item.wecomUserId === user.wecomUserId)
      if (latest) {
        selectedUser.value = latest
      }
    }

    const toggleEnabled = async (user) => {
      await userStore.updateUserEnabled(user.wecomUserId, !user.enabled)
      const latest = userStore.users.find(item => item.wecomUserId === user.wecomUserId)
      if (latest) {
        selectedUser.value = latest
      }
    }

    const selectUser = async (user) => {
      selectedUser.value = user
      const rules = await userStore.fetchVisibilityRules(user.wecomUserId)
      visibleUserIds.value = rules.map(rule => rule.visibleUserId)
    }

    const saveRules = async () => {
      if (!selectedUser.value) return
      await userStore.saveVisibilityRules(selectedUser.value.wecomUserId, visibleUserIds.value)
    }

    onMounted(async () => {
      await userStore.fetchUsers()
    })

    return {
      users,
      selectableUsers,
      selectedUser,
      visibleUserIds,
      roleText,
      syncUsers,
      changeRole,
      toggleEnabled,
      selectUser,
      saveRules
    }
  }
}
</script>
