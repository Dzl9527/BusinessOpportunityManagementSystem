<template>
  <section class="content-panel users-panel">
    <div class="users-toolbar">
      <div>
        <h2>用户权限</h2>
        <p>企业微信用户、角色和白名单配置</p>
      </div>
      <button class="btn-primary" @click="syncUsers">同步企业微信用户</button>
    </div>

    <div class="users-layout">
      <div class="table-container">
        <table class="custom-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>企业微信ID</th>
              <th>职位</th>
              <th>角色</th>
              <th>状态</th>
              <th style="text-align: right;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in users" :key="item.wecomUserId" :class="{ selected: selectedUser?.wecomUserId === item.wecomUserId }">
              <td>
                <strong>{{ item.name }}</strong>
                <div class="muted-line">{{ item.departmentName || '未同步部门' }}</div>
              </td>
              <td>{{ item.wecomUserId }}</td>
              <td>{{ item.position || '-' }}</td>
              <td>
                <select class="form-control compact-control" :value="item.role" @change="changeRole(item, $event.target.value)">
                  <option value="USER">普通用户</option>
                  <option value="LEADER">领导</option>
                  <option value="ADMIN">管理员</option>
                </select>
              </td>
              <td>
                <button class="status-toggle" :class="{ enabled: item.enabled }" @click="toggleEnabled(item)">
                  {{ item.enabled ? '启用' : '禁用' }}
                </button>
              </td>
              <td style="text-align: right;">
                <button class="btn-secondary" @click="selectUser(item)">配置白名单</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <aside class="visibility-panel">
        <h3>白名单</h3>
        <p v-if="!selectedUser">选择一个用户后配置其可查看和编辑的人员。</p>
        <template v-else>
          <div class="selected-user-card">
            <strong>{{ selectedUser.name }}</strong>
            <span>{{ selectedUser.wecomUserId }}</span>
          </div>
          <div class="visibility-list">
            <label v-for="item in selectableUsers" :key="item.wecomUserId" class="visibility-option">
              <input type="checkbox" :value="item.wecomUserId" v-model="visibleUserIds">
              <span>{{ item.name }}</span>
              <small>{{ item.wecomUserId }}</small>
            </label>
          </div>
          <button class="btn-primary save-rules-btn" @click="saveRules">保存白名单</button>
        </template>
      </aside>
    </div>
  </section>
</template>

<script>
import { computed, onMounted, ref } from 'vue'
import { useStore } from '../store'

export default {
  setup() {
    const store = useStore()
    const selectedUser = ref(null)
    const visibleUserIds = ref([])
    const users = computed(() => store.users.value)
    const selectableUsers = computed(() => users.value.filter(item => item.wecomUserId !== selectedUser.value?.wecomUserId))

    const syncUsers = async () => {
      await store.syncWeComUsers()
    }

    const changeRole = async (user, role) => {
      await store.updateUserRole(user.wecomUserId, { role, canViewAll: role === 'ADMIN' })
    }

    const toggleEnabled = async (user) => {
      await store.updateUserEnabled(user.wecomUserId, !user.enabled)
    }

    const selectUser = async (user) => {
      selectedUser.value = user
      const rules = await store.fetchVisibilityRules(user.wecomUserId)
      visibleUserIds.value = rules.map(rule => rule.visibleUserId)
    }

    const saveRules = async () => {
      if (!selectedUser.value) return
      await store.saveVisibilityRules(selectedUser.value.wecomUserId, visibleUserIds.value)
    }

    onMounted(async () => {
      await store.fetchUsers()
    })

    return {
      users,
      selectableUsers,
      selectedUser,
      visibleUserIds,
      syncUsers,
      changeRole,
      toggleEnabled,
      selectUser,
      saveRules
    }
  }
}
</script>
