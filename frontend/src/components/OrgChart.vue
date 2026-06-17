<template>
  <div class="org-chart">
    <div class="org-node" v-for="node in treeData" :key="node.departmentId">
      <details class="org-details" :open="node.children && node.children.length > 0">
        <summary class="org-summary">
          <div class="org-node-info">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-folder"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>
            <span class="org-dept-name">{{ node.name }}</span>
            <span class="org-leader-badge" v-if="node.leaderUserId" title="部门负责人">
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-star"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
              {{ getUserName(node.leaderUserId) }}
            </span>
          </div>
        </summary>
        <div class="org-children">
          <!-- Render sub-departments -->
          <org-chart v-if="node.children && node.children.length" :tree-data="node.children" :users="users" />
          
          <!-- Render users in this department -->
          <div class="org-user-list" v-if="getUsersInDept(node.departmentId).length">
            <div class="org-user-item" v-for="user in getUsersInDept(node.departmentId)" :key="user.platformUserId">
              <div class="org-user-avatar">
                <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.name" />
                <span v-else>{{ user.name.slice(0, 1) }}</span>
              </div>
              <span class="org-user-name">{{ user.name }}</span>
              <span class="org-user-role" v-if="user.role === 'ADMIN'">管理员</span>
            </div>
          </div>
        </div>
      </details>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OrgChart',
  props: {
    treeData: {
      type: Array,
      required: true
    },
    users: {
      type: Array,
      required: true
    }
  },
  methods: {
    getUserName(userId) {
      const user = this.users.find(u => u.platformUserId === userId);
      return user ? user.name : userId;
    },
    getUsersInDept(deptId) {
      return this.users.filter(u => u.departmentId === deptId);
    }
  }
}
</script>

<style scoped>
.org-chart {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.org-details {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  overflow: hidden;
}
.org-summary {
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
  font-weight: 500;
  display: flex;
  align-items: center;
  background: var(--bg-color);
  transition: background 0.2s;
}
.org-summary:hover {
  background: var(--border-color);
}
.org-summary::-webkit-details-marker {
  display: none;
}
.org-node-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.org-dept-name {
  font-size: 14px;
}
.org-leader-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: rgba(234, 179, 8, 0.1);
  color: #ca8a04;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.org-children {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.org-user-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
  padding: 8px 0;
}
.org-user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: var(--bg-color);
  border-radius: 20px;
  border: 1px solid var(--border-color);
}
.org-user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  overflow: hidden;
}
.org-user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.org-user-name {
  font-size: 13px;
  color: var(--text-color);
}
.org-user-role {
  font-size: 10px;
  padding: 2px 6px;
  background: var(--danger-color);
  color: white;
  border-radius: 8px;
}
</style>
