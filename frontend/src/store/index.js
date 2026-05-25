import { reactive, toRefs } from 'vue'
import axios from 'axios'

const API_HOST = window.location.hostname || 'localhost'
const API_BASE = `http://${API_HOST}:8080/api`

export { API_BASE }

// Simple Toast helper
export const toastState = reactive({
  message: '',
  type: 'info',
  show: false
})

export function showToast(message, type = 'info') {
  toastState.message = message
  toastState.type = type
  toastState.show = true
  setTimeout(() => {
    toastState.show = false
  }, 3000)
}

// Main reactive store
const state = reactive({
  opportunities: [],
  opportunityOptions: {
    stages: {},
    industries: [],
    deviceTypes: [],
    authorizedCategories: [],
    winRates: [],
    purchaseTypes: [],
    businessProgressStatuses: [],
    amountUnits: [],
    regions: []
  },
  metrics: {
    totalPipeline: 0,
    activeCount: 0,
    winRate: '0.0',
    avgValue: 0
  },
  contacts: [],
  users: [],
  user: JSON.parse(localStorage.getItem('crm_user')) || null,
  theme: localStorage.getItem('crm-theme') || 'light'
})

const currentUserParams = () => ({
  userId: state.user?.userId,
  userName: state.user?.name
})

const adminParams = () => ({
  adminUserId: state.user?.userId,
  adminName: state.user?.name
})

// Axios configuration to handle standard authentication headers (if required later)
axios.interceptors.request.use(config => {
  if (state.user && state.user.token) {
    config.headers.Authorization = `Bearer ${state.user.token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

export function useStore() {
  
  const fetchOpportunities = async (filters = {}) => {
    try {
      const response = await axios.get(`${API_BASE}/opportunities`, { params: { ...filters, ...currentUserParams() } })
      state.opportunities = response.data
    } catch (e) {
      console.error('Failed to fetch opportunities', e)
      showToast('获取商机列表数据失败，请确认后端已启动', 'error')
    }
  }

  const fetchMyOpportunities = async (filters = {}) => {
    try {
      const response = await axios.get(`${API_BASE}/opportunities/mine`, { params: { ...filters, ...currentUserParams() } })
      state.opportunities = response.data
    } catch (e) {
      console.error('Failed to fetch my opportunities', e)
      showToast('获取我的商机失败', 'error')
    }
  }

  const fetchOpportunityOptions = async () => {
    try {
      const response = await axios.get(`${API_BASE}/opportunities/options`)
      state.opportunityOptions = response.data
    } catch (e) {
      console.error('Failed to fetch opportunity options', e)
      showToast('获取商机选项失败，将使用默认选项', 'error')
    }
  }

  const fetchMetrics = async () => {
    try {
      const response = await axios.get(`${API_BASE}/opportunities/metrics`, { params: currentUserParams() })
      state.metrics = response.data
    } catch (e) {
      console.error('Failed to fetch metrics', e)
    }
  }

  const fetchContacts = async () => {
    try {
      const response = await axios.get(`${API_BASE}/wecom/contacts`)
      state.contacts = response.data
    } catch (e) {
      console.error('Failed to fetch contacts', e)
    }
  }

  const fetchUsers = async () => {
    try {
      const response = await axios.get(`${API_BASE}/users`, { params: adminParams() })
      state.users = response.data
    } catch (e) {
      console.error('Failed to fetch users', e)
      showToast('获取用户列表失败', 'error')
    }
  }

  const syncWeComUsers = async () => {
    try {
      const response = await axios.post(`${API_BASE}/users/sync-wecom`, null, { params: adminParams() })
      state.users = response.data
      showToast('企业微信用户同步完成', 'success')
    } catch (e) {
      console.error('Failed to sync users', e)
      showToast('企业微信用户同步失败', 'error')
    }
  }

  const updateUserRole = async (wecomUserId, payload) => {
    await axios.put(`${API_BASE}/users/${wecomUserId}/role`, payload, { params: adminParams() })
    await fetchUsers()
  }

  const updateUserEnabled = async (wecomUserId, enabled) => {
    await axios.put(`${API_BASE}/users/${wecomUserId}/enabled`, { enabled }, { params: adminParams() })
    await fetchUsers()
  }

  const fetchVisibilityRules = async (wecomUserId) => {
    const response = await axios.get(`${API_BASE}/users/${wecomUserId}/visibility-rules`, { params: adminParams() })
    return response.data
  }

  const saveVisibilityRules = async (wecomUserId, visibleUserIds) => {
    const response = await axios.put(`${API_BASE}/users/${wecomUserId}/visibility-rules`, { visibleUserIds }, {
      params: adminParams()
    })
    showToast('白名单已保存', 'success')
    return response.data
  }

  const fetchOpportunityChangeLogs = async (oppId) => {
    const response = await axios.get(`${API_BASE}/opportunities/${oppId}/change-logs`, { params: currentUserParams() })
    return response.data
  }

  const loginWithCode = async (code) => {
    try {
      const response = await axios.get(`${API_BASE}/wecom/auth`, { params: { code } })
      state.user = response.data
      localStorage.setItem('crm_user', JSON.stringify(response.data))
      showToast(`企业微信免登成功，欢迎您，${response.data.name}！`, 'success')
      return true
    } catch (e) {
      console.error('SSO Code auth failed', e)
      showToast('企业微信登录验证失败，请使用沙箱模拟登录', 'error')
      return false
    }
  }

  const loginSandbox = (userId = 'zhang_jingli') => {
    return loginWithCode(`mock_code:${userId}`)
  }

  const logout = () => {
    state.user = null
    localStorage.removeItem('crm_user')
    showToast('已成功退出系统登录', 'info')
  }

  const toggleTheme = () => {
    const newTheme = state.theme === 'dark' ? 'light' : 'dark'
    state.theme = newTheme
    document.documentElement.setAttribute('data-theme', newTheme)
    localStorage.setItem('crm-theme', newTheme)
  }

  const createOpp = async (oppData) => {
    try {
      await axios.post(`${API_BASE}/opportunities`, oppData, { params: currentUserParams() })
      showToast('新销售商机已成功录入', 'success')
      await fetchMyOpportunities()
      await fetchMetrics()
    } catch (e) {
      console.error('Create failed', e)
      showToast('商机录入失败，请检查字段格式', 'error')
    }
  }

  const submitOpportunity = async (oppData) => {
    try {
      const params = {
        ...currentUserParams()
      }
      await axios.post(`${API_BASE}/opportunities/submissions`, oppData, { params })
      showToast('商机提报已提交，并已生成企业微信提醒', 'success')
      await fetchMyOpportunities()
      await fetchMetrics()
    } catch (e) {
      console.error('Submission failed', e)
      showToast('商机提报失败，请检查字段格式', 'error')
      throw e
    }
  }

  const checkDuplicates = async (oppData) => {
    try {
      const response = await axios.post(`${API_BASE}/opportunities/submissions/check-duplicates`, oppData, { params: currentUserParams() })
      return response.data
    } catch (e) {
      console.error('Duplicate check failed', e)
      return { duplicate: false, matches: [] }
    }
  }

  const updateOpp = async (id, oppData) => {
    try {
      await axios.put(`${API_BASE}/opportunities/${id}`, oppData, { params: currentUserParams() })
      showToast('商机资料保存成功', 'success')
      await fetchMyOpportunities()
      await fetchMetrics()
    } catch (e) {
      console.error('Update failed', e)
      if (e.response?.status === 409) {
        showToast('需求设备类型已被OA流程锁定，不能修改', 'error')
      } else {
        showToast('更新商机资料失败', 'error')
      }
    }
  }

  const startOaReportFlow = async (oppId, reportFlowNo = '') => {
    try {
      const payload = {
        ...currentUserParams(),
        reportFlowNo
      }
      const response = await axios.post(`${API_BASE}/oa/opportunities/${oppId}/report-flow/start`, payload)
      showToast('OA商机报备流程已发起', 'success')
      await fetchMyOpportunities()
      await fetchMetrics()
      return response.data
    } catch (e) {
      console.error('Start OA report flow failed', e)
      if (e.response?.status === 403) {
        showToast('无权发起该商机的OA报备流程', 'error')
      } else {
        showToast('发起OA商机报备失败', 'error')
      }
      return null
    }
  }

  const fetchBidDocumentPrefill = async (reportFlowNo) => {
    try {
      const response = await axios.get(`${API_BASE}/oa/report-flows/${reportFlowNo}/bid-document-prefill`)
      return response.data
    } catch (e) {
      console.error('Fetch OA bid prefill failed', e)
      if (e.response?.status === 409) {
        showToast('商机报备流程未归档，请先完成OA归档', 'error')
      } else {
        showToast('获取投标文件制作预填数据失败', 'error')
      }
      return null
    }
  }

  const addAttachment = async (oppId, attachment) => {
    try {
      const response = await axios.post(`${API_BASE}/opportunities/${oppId}/attachments`, attachment, {
        params: currentUserParams()
      })
      showToast('附件记录已添加', 'success')
      return response.data
    } catch (e) {
      console.error('Add attachment failed', e)
      showToast('附件记录添加失败', 'error')
      return null
    }
  }

  const deleteOpp = async (id) => {
    try {
      await axios.delete(`${API_BASE}/opportunities/${id}`, { params: currentUserParams() })
      showToast('商机已成功删除', 'success')
      await fetchMyOpportunities()
      await fetchMetrics()
    } catch (e) {
      console.error('Delete failed', e)
      showToast('删除商机失败', 'error')
    }
  }

  // Task methods
  const addTask = async (oppId, text) => {
    try {
      const response = await axios.post(`${API_BASE}/opportunities/${oppId}/tasks`, { text }, { params: currentUserParams() })
      showToast('任务已成功添加', 'success')
      return response.data
    } catch (e) {
      console.error('Add task failed', e)
      showToast('添加任务失败', 'error')
      return null;
    }
  }

  const toggleTask = async (oppId, taskId) => {
    try {
      const response = await axios.put(`${API_BASE}/opportunities/${oppId}/tasks/${taskId}/toggle`, null, { params: currentUserParams() })
      showToast(response.data.done ? '任务已标记为完成' : '任务已重新开启', 'info')
      return response.data
    } catch (e) {
      console.error('Toggle task failed', e)
      return null
    }
  }

  // Activity methods
  const addActivity = async (oppId, type, content) => {
    try {
      const response = await axios.post(`${API_BASE}/opportunities/${oppId}/activities`, { type, content }, { params: currentUserParams() })
      showToast('已录入跟进记录', 'success')
      return response.data
    } catch (e) {
      console.error('Add activity failed', e)
      return null
    }
  }

  return {
    ...toRefs(state),
    fetchOpportunities,
    fetchMyOpportunities,
    fetchOpportunityOptions,
    fetchMetrics,
    fetchContacts,
    fetchUsers,
    syncWeComUsers,
    updateUserRole,
    updateUserEnabled,
    fetchVisibilityRules,
    saveVisibilityRules,
    fetchOpportunityChangeLogs,
    loginWithCode,
    loginSandbox,
    logout,
    toggleTheme,
    createOpp,
    submitOpportunity,
    checkDuplicates,
    updateOpp,
    startOaReportFlow,
    fetchBidDocumentPrefill,
    deleteOpp,
    addAttachment,
    addTask,
    toggleTask,
    addActivity
  }
}
