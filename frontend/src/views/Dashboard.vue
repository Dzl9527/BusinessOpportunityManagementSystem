<template>
  <section class="content-panel active">
    <!-- Metrics Grid -->
    <div class="metrics-grid">
      <!-- Metric 1 -->
      <div class="metric-card">
        <div class="metric-card-header">
          <span class="metric-card-title">商机加权总额</span>
          <div class="metric-card-icon" style="background-color: var(--primary-light); color: var(--primary);">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-dollar-sign"><line x1="12" x2="12" y1="2" y2="22"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          </div>
        </div>
        <div class="metric-card-value">{{ formatCurrency(metrics.totalPipeline) }}</div>
        <div class="metric-card-footer">
          <span class="metric-trend-up">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-arrow-up-right"><line x1="7" x2="17" y1="17" y2="7"/><polyline points="7 7 17 7 17 17"/></svg>
            +12.5%
          </span>
          <span>较上月环比</span>
        </div>
      </div>
      <!-- Metric 2 -->
      <div class="metric-card">
        <div class="metric-card-header">
          <span class="metric-card-title">活跃商机数量</span>
          <div class="metric-card-icon" style="background-color: var(--info-light); color: var(--info);">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-briefcase"><rect width="20" height="14" x="2" y="7" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
          </div>
        </div>
        <div class="metric-card-value">{{ metrics.activeCount }}</div>
        <div class="metric-card-footer">
          <span class="metric-trend-up">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-arrow-up-right"><line x1="7" x2="17" y1="17" y2="7"/><polyline points="7 7 17 7 17 17"/></svg>
            +4
          </span>
          <span>新增商机本周</span>
        </div>
      </div>
      <!-- Metric 3 -->
      <div class="metric-card">
        <div class="metric-card-header">
          <span class="metric-card-title">赢单转化率</span>
          <div class="metric-card-icon" style="background-color: var(--success-light); color: var(--success);">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-award"><circle cx="12" cy="8" r="7"/><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/></svg>
          </div>
        </div>
        <div class="metric-card-value">{{ Number(metrics.winRate).toFixed(1) }}%</div>
        <div class="metric-card-footer">
          <span class="metric-trend-up">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-arrow-up-right"><line x1="7" x2="17" y1="17" y2="7"/><polyline points="7 7 17 7 17 17"/></svg>
            +2.3%
          </span>
          <span>高于行业均值</span>
        </div>
      </div>
      <!-- Metric 4 -->
      <div class="metric-card">
        <div class="metric-card-header">
          <span class="metric-card-title">商机平均金额</span>
          <div class="metric-card-icon" style="background-color: var(--warning-light); color: var(--warning);">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-trending-up"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>
          </div>
        </div>
        <div class="metric-card-value">{{ formatCurrency(metrics.avgValue) }}</div>
        <div class="metric-card-footer">
          <span class="metric-trend-down">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-arrow-down-right"><line x1="7" x2="17" y1="7" y2="17"/><polyline points="17 7 17 17 7 17"/></svg>
            -1.2%
          </span>
          <span>大客户比重微调</span>
        </div>
      </div>
    </div>

    <!-- Charts Section -->
    <div class="charts-grid">
      <!-- Chart 1: Funnel -->
      <div class="chart-card">
        <div class="chart-card-title">
          <span>销售商机加权额度 (元)</span>
          <span style="font-size: 12px; color: var(--text-muted); font-weight: normal;">按商机阶段统计的加权流水额</span>
        </div>
        <div class="chart-wrapper">
          <canvas ref="canvasFunnel"></canvas>
        </div>
      </div>
      <!-- Chart 2: Stage Distribution -->
      <div class="chart-card">
        <div class="chart-card-title">
          <span>商机阶段占比 (数量)</span>
        </div>
        <div class="chart-wrapper">
          <canvas ref="canvasStages"></canvas>
        </div>
      </div>
    </div>
    
    <!-- Chart 3: Monthly Close Forecast -->
    <div class="chart-card" style="margin-bottom: 24px;">
      <div class="chart-card-title">
        <span>预计结单趋势预测 (流水分布)</span>
      </div>
      <div class="chart-wrapper" style="height: 250px;">
        <canvas ref="canvasTrend"></canvas>
      </div>
    </div>
  </section>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { storeToRefs } from 'pinia'
import Chart from 'chart.js/auto'
import axios from 'axios'
import { useAuthStore } from '../stores/useAuthStore'
import { useOppStore } from '../stores/useOppStore'
import { useAppStore, API_BASE } from '../stores/useAppStore'
import { useUserStore } from '../stores/useUserStore'
import { useMetricsStore } from '../stores/useMetricsStore'

export default {
  setup() {
    const authStore = useAuthStore()
    const oppStore = useOppStore()
    const appStore = useAppStore()
    const userStore = useUserStore()
    const metricsStore = useMetricsStore()
    const { metrics } = storeToRefs(metricsStore)
    const canvasFunnel = ref(null)
    const canvasStages = ref(null)
    const canvasTrend = ref(null)

    let chartFunnelInstance = null
    let chartStagesInstance = null
    let chartTrendInstance = null

    const STAGES_LABEL = {
      prospecting: "发现商机",
      qualification: "资质评估",
      proposal: "方案报价",
      negotiation: "谈判协商",
      won: "赢得商机",
      lost: "流失商机"
    }

    const STAGE_COLORS = [
      "#3b82f6", // prospecting
      "#8b5cf6", // qualification
      "#f59e0b", // proposal
      "#06b6d4", // negotiation
      "#10b981", // won
      "#ef4444"  // lost
    ]

    const formatCurrency = (val) => {
      return new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: 'CNY',
        minimumFractionDigits: 2
      }).format(val || 0)
    }

    const renderCharts = async () => {
      const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
      const textThemeColor = isDark ? "#94a3b8" : "#475569"
      const gridThemeColor = isDark ? "#1e293b" : "#e2e8f0"

      try {
        // Fetch charts endpoints
        const [resFunnel, resStages, resTrend] = await Promise.all([
          axios.get(`${API_BASE}/opportunities/charts/funnel`, { params: { userId: authStore.user?.userId, userName: authStore.user?.name } }),
          axios.get(`${API_BASE}/opportunities/charts/stages`, { params: { userId: authStore.user?.userId, userName: authStore.user?.name } }),
          axios.get(`${API_BASE}/opportunities/charts/trend`, { params: { userId: authStore.user?.userId, userName: authStore.user?.name } })
        ])

        // 1. Funnel Chart
        const funnelData = resFunnel.data
        const funnelValues = Object.keys(STAGES_LABEL).map(k => funnelData[k] || 0)

        if (chartFunnelInstance) chartFunnelInstance.destroy()
        if (canvasFunnel.value) {
          chartFunnelInstance = new Chart(canvasFunnel.value.getContext('2d'), {
            type: 'bar',
            data: {
              labels: Object.values(STAGES_LABEL),
              datasets: [{
                data: funnelValues,
                backgroundColor: STAGE_COLORS,
                borderRadius: 6,
                barThickness: 25
              }]
            },
            options: {
              indexAxis: 'y',
              responsive: true,
              maintainAspectRatio: false,
              plugins: {
                legend: { display: false },
                tooltip: {
                  callbacks: {
                    label: (context) => ` 金额: ${formatCurrency(context.raw)}`
                  }
                }
              },
              scales: {
                x: {
                  grid: { color: gridThemeColor },
                  ticks: {
                    color: textThemeColor,
                    callback: (val) => val >= 10000 ? `${(val / 10000).toFixed(0)}万` : val
                  }
                },
                y: {
                  grid: { display: false },
                  ticks: { color: textThemeColor }
                }
              }
            }
          })
        }

        // 2. Stages Distribution Chart
        const stagesData = resStages.data
        const stagesCounts = Object.keys(STAGES_LABEL).map(k => stagesData[k] || 0)

        if (chartStagesInstance) chartStagesInstance.destroy()
        if (canvasStages.value) {
          chartStagesInstance = new Chart(canvasStages.value.getContext('2d'), {
            type: 'doughnut',
            data: {
              labels: Object.values(STAGES_LABEL),
              datasets: [{
                data: stagesCounts,
                backgroundColor: STAGE_COLORS,
                borderWidth: isDark ? 2 : 1,
                borderColor: isDark ? "#121826" : "#ffffff"
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              cutout: '70%',
              plugins: {
                legend: {
                  position: 'bottom',
                  labels: {
                    color: textThemeColor,
                    boxWidth: 12,
                    padding: 15,
                    font: { size: 11 }
                  }
                }
              }
            }
          })
        }

        // 3. Close Forecast Trend
        const trendData = resTrend.data
        const trendMonths = Object.keys(trendData)
        const trendValues = Object.values(trendData)

        if (chartTrendInstance) chartTrendInstance.destroy()
        if (canvasTrend.value) {
          chartTrendInstance = new Chart(canvasTrend.value.getContext('2d'), {
            type: 'line',
            data: {
              labels: trendMonths.length > 0 ? trendMonths : ['无结单计划'],
              datasets: [{
                data: trendValues.length > 0 ? trendValues : [0],
                borderColor: '#4f46e5',
                backgroundColor: 'rgba(79, 70, 229, 0.08)',
                fill: true,
                tension: 0.35,
                borderWidth: 3,
                pointRadius: 4,
                pointBackgroundColor: '#4f46e5'
              }]
            },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: {
                legend: { display: false },
                tooltip: {
                  callbacks: {
                    label: (context) => ` 预估结算: ${formatCurrency(context.raw)}`
                  }
                }
              },
              scales: {
                x: {
                  grid: { display: false },
                  ticks: { color: textThemeColor }
                },
                y: {
                  grid: { color: gridThemeColor },
                  ticks: {
                    color: textThemeColor,
                    callback: (val) => val >= 10000 ? `${(val / 10000).toFixed(0)}万` : val
                  }
                }
              }
            }
          })
        }

      } catch (err) {
        console.error("Error building dashboard charts", err)
      }
    }

    onMounted(async () => {
      await metricsStore.fetchMetrics()
      await renderCharts()
    })

    onBeforeUnmount(() => {
      if (chartFunnelInstance) chartFunnelInstance.destroy()
      if (chartStagesInstance) chartStagesInstance.destroy()
      if (chartTrendInstance) chartTrendInstance.destroy()
    })

    // Re-draw charts when theme toggles
    watch(appStore.theme, () => {
      setTimeout(renderCharts, 50)
    })

    return {
      metrics,
      canvasFunnel,
      canvasStages,
      canvasTrend,
      formatCurrency
    }
  }
}
</script>
