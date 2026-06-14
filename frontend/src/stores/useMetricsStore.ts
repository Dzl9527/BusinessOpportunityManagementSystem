import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import { API_BASE } from './useAppStore'

export const useMetricsStore = defineStore('metrics', () => {
  const metrics = ref<any>({
    totalPipeline: 0,
    activeCount: 0,
    winRate: '0.0',
    avgValue: 0
  })

  const fetchMetrics = async () => {
    try {
      const response = await axios.get(`${API_BASE}/opportunities/metrics`)
      metrics.value = response.data
    } catch (e) {
      console.error('Failed to fetch metrics', e)
    }
  }

  return { metrics, fetchMetrics }
})
