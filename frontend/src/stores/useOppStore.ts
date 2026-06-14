import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { API_BASE, useAppStore } from './useAppStore'
import { useMetricsStore } from './useMetricsStore'

const UI_PREFS_KEY = 'crm_admin_ui_prefs'

export const useOppStore = defineStore('opportunity', () => {
  const opportunities = ref<any[]>([])
  const page = ref(0)
  const size = ref(20)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const opportunityOptions = ref<any>({
    stages: {},
    industries: [],
    deviceTypes: [],
    authorizedCategories: [],
    winRates: [],
    purchaseTypes: [],
    businessProgressStatuses: [],
    amountUnits: [],
    regions: []
  })

  const getUiPrefs = () => {
    try {
      return JSON.parse(localStorage.getItem(UI_PREFS_KEY) || '{}')
    } catch (e) {
      return {}
    }
  }

  const fetchOpportunities = async (filters: any = {}, p = page.value, s = size.value) => {
    const appStore = useAppStore()
    try {
      const response = await axios.get(`${API_BASE}/opportunities/page`, { 
        params: { ...filters, page: p, size: s } 
      })
      opportunities.value = response.data.content
      totalElements.value = response.data.totalElements
      totalPages.value = response.data.totalPages
      page.value = response.data.pageable?.pageNumber || p
      size.value = response.data.pageable?.pageSize || s
    } catch (e) {
      console.error('Failed to fetch opportunities', e)
      appStore.showToast('获取商机列表数据失败，请确认后端已启动', 'error')
    }
  }

  const exportOpportunities = (filters: any = {}) => {
    const qs = new URLSearchParams()
    for (const key in filters) {
      if (filters[key] && filters[key] !== 'all') {
        qs.append(key, filters[key])
      }
    }
    window.open(`${API_BASE}/opportunities/export-excel?${qs.toString()}`, '_blank')
  }

  const fetchMyOpportunities = async (filters: any = {}, p = page.value, s = size.value) => {
    const appStore = useAppStore()
    try {
      const response = await axios.get(`${API_BASE}/opportunities/mine/page`, { 
        params: { ...filters, page: p, size: s } 
      })
      opportunities.value = response.data.content
      totalElements.value = response.data.totalElements
      totalPages.value = response.data.totalPages
      page.value = response.data.pageable?.pageNumber || p
      size.value = response.data.pageable?.pageSize || s
    } catch (e) {
      console.error('Failed to fetch my opportunities', e)
      appStore.showToast('获取我的商机失败', 'error')
    }
  }

  const fetchOpportunityOptions = async () => {
    const appStore = useAppStore()
    try {
      const response = await axios.get(`${API_BASE}/opportunities/options`)
      opportunityOptions.value = response.data
    } catch (e) {
      console.error('Failed to fetch opportunity options', e)
      appStore.showToast('获取商机选项失败，将使用默认选项', 'error')
    }
  }

  const fetchOpportunityChangeLogs = async (oppId: string | number) => {
    const response = await axios.get(`${API_BASE}/opportunities/${oppId}/change-logs`)
    return response.data
  }

  const createOpp = async (oppData: any) => {
    const appStore = useAppStore()
    const metricsStore = useMetricsStore()
    try {
      await axios.post(`${API_BASE}/opportunities`, oppData)
      appStore.showToast('新销售商机已成功录入', 'success')
      await fetchMyOpportunities()
      await metricsStore.fetchMetrics()
    } catch (e) {
      console.error('Create failed', e)
      appStore.showToast('商机录入失败，请检查字段格式', 'error')
    }
  }

  const submitOpportunity = async (oppData: any) => {
    const appStore = useAppStore()
    const metricsStore = useMetricsStore()
    try {
      await axios.post(`${API_BASE}/opportunities/submissions`, oppData)
      if (getUiPrefs().showSubmitToast !== false) {
        appStore.showToast('商机提报已提交，并已生成企业微信提醒', 'success')
      }
      await fetchMyOpportunities()
      await metricsStore.fetchMetrics()
    } catch (e: any) {
      console.error('Submission failed', e)
      appStore.showToast('商机提报失败，请检查字段格式', 'error')
      throw e
    }
  }

  const checkDuplicates = async (oppData: any) => {
    try {
      const response = await axios.post(`${API_BASE}/opportunities/submissions/check-duplicates`, oppData)
      return response.data
    } catch (e) {
      console.error('Duplicate check failed', e)
      return { duplicate: false, matches: [] }
    }
  }

  const updateOpp = async (id: string | number, oppData: any) => {
    const appStore = useAppStore()
    const metricsStore = useMetricsStore()
    try {
      await axios.put(`${API_BASE}/opportunities/${id}`, oppData)
      appStore.showToast('商机资料保存成功', 'success')
      await fetchMyOpportunities()
      await metricsStore.fetchMetrics()
    } catch (e: any) {
      console.error('Update failed', e)
      if (e.response?.status === 409) {
        appStore.showToast('需求设备类型已被OA流程锁定，不能修改', 'error')
      } else {
        appStore.showToast('更新商机资料失败', 'error')
      }
    }
  }

  const startOaReportFlow = async (oppId: string | number, reportFlowNo = '') => {
    const appStore = useAppStore()
    const metricsStore = useMetricsStore()
    try {
      const payload = { reportFlowNo }
      const response = await axios.post(`${API_BASE}/oa/opportunities/${oppId}/report-flow/start`, payload)
      appStore.showToast('OA商机报备流程已发起', 'success')
      await fetchMyOpportunities()
      await metricsStore.fetchMetrics()
      return response.data
    } catch (e: any) {
      console.error('Start OA report flow failed', e)
      if (e.response?.status === 403) {
        appStore.showToast('无权发起该商机的OA报备流程', 'error')
      } else {
        appStore.showToast('发起OA商机报备失败', 'error')
      }
      return null
    }
  }

  const fetchBidDocumentPrefill = async (reportFlowNo: string) => {
    const appStore = useAppStore()
    try {
      const response = await axios.get(`${API_BASE}/oa/report-flows/${reportFlowNo}/bid-document-prefill`)
      return response.data
    } catch (e: any) {
      console.error('Fetch OA bid prefill failed', e)
      if (e.response?.status === 409) {
        appStore.showToast('商机报备流程未归档，请先完成OA归档', 'error')
      } else {
        appStore.showToast('获取项目授权预填数据失败', 'error')
      }
      return null
    }
  }

  const addAttachment = async (oppId: string | number, attachment: any) => {
    const appStore = useAppStore()
    try {
      const response = await axios.post(`${API_BASE}/opportunities/${oppId}/attachments`, attachment)
      appStore.showToast('附件记录已添加', 'success')
      return response.data
    } catch (e) {
      console.error('Add attachment failed', e)
      appStore.showToast('附件记录添加失败', 'error')
      return null
    }
  }

  const deleteOpp = async (id: string | number) => {
    const appStore = useAppStore()
    const metricsStore = useMetricsStore()
    try {
      await axios.delete(`${API_BASE}/opportunities/${id}`)
      appStore.showToast('商机已成功删除', 'success')
      await fetchMyOpportunities()
      await metricsStore.fetchMetrics()
    } catch (e) {
      console.error('Delete failed', e)
      appStore.showToast('删除商机失败', 'error')
    }
  }

  const addTask = async (oppId: string | number, text: string) => {
    const appStore = useAppStore()
    try {
      const response = await axios.post(`${API_BASE}/opportunities/${oppId}/tasks`, { text })
      appStore.showToast('任务已成功添加', 'success')
      return response.data
    } catch (e) {
      console.error('Add task failed', e)
      appStore.showToast('添加任务失败', 'error')
      return null
    }
  }

  const toggleTask = async (oppId: string | number, taskId: string | number) => {
    const appStore = useAppStore()
    try {
      const response = await axios.put(`${API_BASE}/opportunities/${oppId}/tasks/${taskId}/toggle`)
      appStore.showToast(response.data.done ? '任务已标记为完成' : '任务已重新开启', 'info')
      return response.data
    } catch (e) {
      console.error('Toggle task failed', e)
      return null
    }
  }

  const addActivity = async (oppId: string | number, type: string, content: string) => {
    const appStore = useAppStore()
    try {
      const response = await axios.post(`${API_BASE}/opportunities/${oppId}/activities`, { type, content })
      appStore.showToast('已录入跟进记录', 'success')
      return response.data
    } catch (e) {
      console.error('Add activity failed', e)
      return null
    }
  }

  return {
    opportunities,
    page,
    size,
    totalElements,
    totalPages,
    opportunityOptions,
    fetchOpportunities,
    exportOpportunities,
    fetchMyOpportunities,
    fetchOpportunityOptions,
    fetchOpportunityChangeLogs,
    createOpp,
    submitOpportunity,
    checkDuplicates,
    updateOpp,
    startOaReportFlow,
    fetchBidDocumentPrefill,
    addAttachment,
    deleteOpp,
    addTask,
    toggleTask,
    addActivity
  }
})
