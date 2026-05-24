<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-logo">
        <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-trending-up"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>
      </div>
      
      <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 8px; color: var(--text-primary);">商机宝 CRM</h2>
      <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 32px;">企业微信生态智能销售商机管理中心</p>

      <!-- SSO Loading state -->
      <div v-if="isAuthenticating" style="display: flex; flex-direction: column; align-items: center; gap: 16px; margin: 20px 0;">
        <div class="loading-spinner"></div>
        <span style="font-size: 13px; color: var(--text-secondary);">正在对接企业微信安全身份认证，请稍候...</span>
      </div>

      <!-- General Login Choices -->
      <div v-else style="width: 100%; display: flex; flex-direction: column; align-items: center;">
        
        <!-- Mock WeChat QR Container -->
        <div class="qr-box" @click="handleSandboxLogin" title="点击可直接使用沙箱用户登录">
          <!-- Stylized mock QR -->
          <div style="width: 160px; height: 160px; border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 10px; background: white; display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative;">
            <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 24 24" fill="none" stroke="#0f172a" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-qr-code"><rect width="5" height="5" x="3" y="3" rx="1"/><rect width="5" height="5" x="16" y="3" rx="1"/><rect width="5" height="5" x="3" y="16" rx="1"/><path d="M21 16v5a1 1 0 0 1-1 1h-4"/><path d="M21 12v2"/><path d="M12 21v-2"/><path d="M12 12h.01"/><path d="M16 12h.01"/><path d="M21 8v.01"/><path d="M12 16h.01"/><rect width="1" height="1" x="16" y="16"/><rect width="1" height="1" x="8" y="8"/><rect width="1" height="1" x="8" y="16"/></svg>
            <div style="font-size: 9px; color: #475569; font-weight: bold; margin-top: 6px;">[扫码测试] 点击即可模拟扫码</div>
          </div>
          <div style="margin-top: 14px; font-size: 13px; color: var(--text-secondary); display: flex; align-items: center; gap: 8px;">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-message-circle"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>
            <span>企业微信扫码安全登录</span>
          </div>
        </div>

        <div style="width: 100%; border-top: 1px solid var(--border-color); margin: 32px 0 20px; position: relative;">
          <span style="position: absolute; top: -10px; left: 50%; transform: translateX(-50%); background-color: var(--bg-secondary); padding: 0 12px; font-size: 11px; color: var(--text-muted); text-transform: uppercase;">或者</span>
        </div>

        <!-- Sandbox direct login button -->
        <div class="sandbox-login-grid">
          <button
            v-for="account in sandboxAccounts"
            :key="account.userId"
            class="sandbox-account-btn"
            type="button"
            @click="handleSandboxLogin(account.userId)"
          >
            <strong>{{ account.name }}</strong>
            <span>{{ account.role }}</span>
          </button>
        </div>

        <button class="btn-primary" style="width: 100%; height: 44px; justify-content: center; font-size: 14px;" @click="handleSandboxLogin()">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-shield-check"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 11 2 2 4-4"/></svg>
          <span>进入演示沙箱 (免企微凭证)</span>
        </button>

        <p style="font-size: 11px; color: var(--text-muted); margin-top: 16px; line-height: 1.5; padding: 0 10px;">
          * 说明：本系统已包含“企业微信对接服务”。本地脱机调试可选择不同沙箱账号验证管理员、领导、普通用户权限。
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from '../store'

export default {
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()
    const isAuthenticating = ref(false)
    const sandboxAccounts = [
      { userId: 'zhang_jingli', name: '张经理', role: '管理员' },
      { userId: 'li_zhuguan', name: '李主管', role: '领导/白名单' },
      { userId: 'wang_xiaoshou', name: '王销售', role: '普通用户' }
    ]

    const handleSandboxLogin = async (userId = 'zhang_jingli') => {
      await store.loginSandbox(userId)
      router.push({ name: 'Dashboard' })
    }

    onMounted(async () => {
      // Check if "code" exists in url query params (redirect callback from WeCom OAuth)
      const code = route.query.code
      if (code) {
        isAuthenticating.value = true
        const success = await store.loginWithCode(code)
        isAuthenticating.value = false
        if (success) {
          router.push({ name: 'Dashboard' })
        } else {
          // Stay on page, fallback to sandbox enabled
          router.replace({ query: {} }) // remove bad code query
        }
      }
    })

    return {
      isAuthenticating,
      sandboxAccounts,
      handleSandboxLogin
    }
  }
}
</script>

<style scoped>
.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.qr-box:hover {
  opacity: 0.85;
}

.sandbox-login-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.sandbox-account-btn {
  border: 1px solid var(--border-color);
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  min-height: 58px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
}

.sandbox-account-btn span {
  color: var(--text-muted);
  font-size: 11px;
}

@media (max-width: 520px) {
  .sandbox-login-grid {
    grid-template-columns: 1fr;
  }
}
</style>
