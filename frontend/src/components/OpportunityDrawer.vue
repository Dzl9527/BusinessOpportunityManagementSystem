<template>
  <div class="drawer-overlay" v-if="visible" @click.self="handleClose">
    <div class="drawer-content">
      <!-- Drawer Header -->
      <div class="drawer-header" v-if="opp">
        <div>
          <h3 class="modal-title" style="margin-bottom: 4px;">{{ opp.name }}</h3>
          <span style="font-size: 13px; color: var(--text-muted); font-weight: 500;">{{ opp.company }}</span>
        </div>
        <button class="btn-icon" @click="handleClose" style="border: none; background: transparent;">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-x"><line x1="18" x2="6" y1="6" y2="18"/><line x1="6" x2="18" y1="6" y2="18"/></svg>
        </button>
      </div>

      <!-- Drawer Body -->
      <div class="drawer-body" v-if="opp">
        <!-- Quick stats summary card -->
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">商机预估金额</span>
            <span class="detail-value" style="color: var(--primary); font-size: 16px;">{{ formatCurrency(opp.value) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">当前商机阶段</span>
            <div>
              <span :class="`stage-badge stage-${opp.stage}-badge`">{{ STAGES[opp.stage] }}</span>
            </div>
          </div>
          <div class="detail-item">
            <span class="detail-label">预计结单日期</span>
            <span class="detail-value">{{ opp.closeDate }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">指派负责人</span>
            <span class="detail-value">{{ opp.owner }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">设备需求版本</span>
            <span class="detail-value">V{{ opp.deviceRequirementVersion || 1 }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">需求设备类型状态</span>
            <span :class="['oa-status-pill', lockStatusClass(opp.deviceRequirementLockStatus)]">
              {{ lockStatusLabel(opp.deviceRequirementLockStatus) }}
            </span>
          </div>
        </div>

        <!-- Detail tabs switcher -->
        <div class="drawer-tabs">
          <div class="drawer-tab" :class="{ 'active': activeTab === 'summary' }" @click="activeTab = 'summary'">基本信息</div>
          <div class="drawer-tab" :class="{ 'active': activeTab === 'tasks' }" @click="activeTab = 'tasks'">
            任务清单 ({{ opp.tasks ? opp.tasks.length : 0 }})
          </div>
          <div class="drawer-tab" :class="{ 'active': activeTab === 'activities' }" @click="activeTab = 'activities'">跟进日志</div>
          <div class="drawer-tab" :class="{ 'active': activeTab === 'changes' }" @click="activeTab = 'changes'">
            修订记录 ({{ changeLogs.length }})
          </div>
        </div>

        <!-- Tab 1: Summary -->
        <div class="tab-pane" v-if="activeTab === 'summary'">
          <div style="display: flex; flex-direction: column; gap: 16px;">
            <div class="detail-item">
              <span class="detail-label">赢单胜率 (预测)</span>
              <span class="detail-value" style="color: var(--warning); font-size: 15px; font-weight: 700;">{{ opp.probability }}%</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">商机获取渠道</span>
              <span class="detail-value">{{ opp.source || '暂未录入' }}</span>
            </div>

            <div style="border-top: 1px solid var(--border-color); padding-top: 16px;">
              <div class="oa-section-header">
                <h4 style="font-size: 14px; font-weight: 600; color: var(--text-secondary);">OA流程关联</h4>
                <button
                  v-if="canStartReportFlow(opp)"
                  class="btn-secondary"
                  type="button"
                  @click="handleStartReportFlow"
                  style="padding: 8px 12px; font-size: 12px;"
                >
                  发起OA商机报备
                </button>
              </div>
              <div class="oa-info-grid">
                <div>
                  <span>商机报备流程号</span>
                  <strong>{{ opp.reportFlowNo || '未生成' }}</strong>
                </div>
                <div>
                  <span>商机报备状态</span>
                  <strong>{{ reportStatusLabel(opp.reportFlowStatus) }}</strong>
                </div>
                <div>
                  <span>报备归档时间</span>
                  <strong>{{ opp.reportArchivedAt || '-' }}</strong>
                </div>
                <div>
                  <span>投标制作流程号</span>
                  <strong>{{ opp.bidDocumentFlowNo || '未发起' }}</strong>
                </div>
                <div>
                  <span>投标制作状态</span>
                  <strong>{{ bidStatusLabel(opp.bidDocumentFlowStatus) }}</strong>
                </div>
                <div>
                  <span>锁定流程号</span>
                  <strong>{{ opp.deviceRequirementLockedByFlowNo || '-' }}</strong>
                </div>
                <div>
                  <span>锁定节点</span>
                  <strong>{{ opp.deviceRequirementLockedAtNode || '-' }}</strong>
                </div>
                <div>
                  <span>锁定时间</span>
                  <strong>{{ opp.deviceRequirementLockedAt || '-' }}</strong>
                </div>
              </div>
            </div>
            
            <div style="border-top: 1px solid var(--border-color); padding-top: 16px;">
              <h4 style="font-size: 14px; font-weight: 600; margin-bottom: 12px; color: var(--text-secondary);">主要客户联系方式</h4>
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                <div class="detail-item">
                  <span class="detail-label">联系人姓名</span>
                  <span class="detail-value">{{ opp.contactName || '无' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">移动电话 / 座机</span>
                  <span class="detail-value">{{ opp.contactPhone || '无' }}</span>
                </div>
                <div class="detail-item" style="grid-column: span 2;">
                  <span class="detail-label">电子邮箱</span>
                  <span class="detail-value">{{ opp.contactEmail || '无' }}</span>
                </div>
              </div>
            </div>
            
            <div style="border-top: 1px solid var(--border-color); padding-top: 16px;">
              <h4 style="font-size: 14px; font-weight: 600; margin-bottom: 8px; color: var(--text-secondary);">需求背景及业务描述</h4>
              <p style="font-size: 13.5px; line-height: 1.6; color: var(--text-secondary); background: var(--bg-primary); padding: 16px; border-radius: var(--radius-md); white-space: pre-wrap;">{{ opp.description || '暂无描述。' }}</p>
            </div>
          </div>
        </div>

        <!-- Tab 2: Tasks Checklist -->
        <div class="tab-pane" v-if="activeTab === 'tasks'">
          <!-- Add task header -->
          <div style="display: flex; gap: 12px;">
            <input type="text" class="form-control" v-model="newTaskText" @keyup.enter="handleAddTask" placeholder="录入新的待办任务，如：约定技术演示...">
            <button class="btn-primary" @click="handleAddTask" style="padding: 10px 16px; flex-shrink: 0;">添加</button>
          </div>
          <!-- Task items list container -->
          <div style="display: flex; flex-direction: column; gap: 10px; margin-top: 10px;">
            <div 
              v-for="task in opp.tasks" 
              :key="task.id" 
              class="task-item" 
              :class="{ 'completed': task.done }"
              @click="handleToggleTask(task)"
            >
              <div class="task-checkbox">
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-check"><polyline points="20 6 9 17 4 12"/></svg>
              </div>
              <span class="task-text">{{ task.text }}</span>
            </div>
            
            <div v-if="!opp.tasks || opp.tasks.length === 0" style="text-align: center; color: var(--text-muted); font-size: 13px; padding: 20px;">
              暂无待办任务
            </div>
          </div>
        </div>

        <!-- Tab 3: Activities Timeline -->
        <div class="tab-pane" v-if="activeTab === 'activities'">
          <!-- Activity Entry Form -->
          <div class="activity-log-form">
            <span class="form-label">录入最新的跟进纪要</span>
            <div class="timeline-type-selector">
              <button 
                v-for="type in activityTypes" 
                :key="type.key" 
                class="type-btn" 
                :class="{ 'active': activeActivityType === type.key }"
                @click="activeActivityType = type.key"
              >
                {{ type.label }}
              </button>
            </div>
            <textarea class="form-control" v-model="newActivityContent" style="min-height: 70px;" placeholder="记录跟进反馈、备忘事项..."></textarea>
            <button class="btn-primary" @click="handleAddActivity" style="align-self: flex-end; padding: 8px 16px; font-size: 13px;">提交活动日志</button>
          </div>

          <!-- Timeline content wrapper -->
          <div class="timeline" style="margin-top: 10px;">
            <div v-for="act in opp.activities" :key="act.id" class="timeline-item">
              <div :class="`timeline-node ${act.type}`">
                <!-- Select proper SVGs for timeline nodes -->
                <svg v-if="act.type === 'call'" xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-phone"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                <svg v-else-if="act.type === 'meeting'" xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-users"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                <svg v-else-if="act.type === 'email'" xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-mail"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-shield-alert"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
              </div>
              <div class="timeline-date">{{ act.date }}</div>
              <div class="timeline-content">{{ act.content }}</div>
            </div>
            
            <div v-if="!opp.activities || opp.activities.length === 0" style="text-align: center; color: var(--text-muted); font-size: 13px; padding: 20px;">
              暂无跟进活动记录
            </div>
          </div>
        </div>

        <div class="tab-pane" v-if="activeTab === 'changes'">
          <div class="change-log-list">
            <div v-for="log in changeLogs" :key="log.id" class="change-log-item">
              <div class="change-log-header">
                <strong>{{ log.fieldLabel || log.fieldName }}</strong>
                <span>{{ log.editedAt }}</span>
              </div>
              <div class="change-log-meta">
                {{ log.editorName || log.editorUserId || '-' }} / {{ permissionLabel(log.permissionSource) }}
              </div>
              <div class="change-log-values">
                <div><span>修改前</span><p>{{ log.oldValue || '-' }}</p></div>
                <div><span>修改后</span><p>{{ log.newValue || '-' }}</p></div>
              </div>
            </div>
            <div v-if="changeLogs.length === 0" style="text-align: center; color: var(--text-muted); font-size: 13px; padding: 20px;">
              暂无字段级修订记录
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, watch } from 'vue'
import axios from 'axios'
import { useAuthStore } from '../stores/useAuthStore'
import { useOppStore } from '../stores/useOppStore'
import { useAppStore } from '../stores/useAppStore'
import { useUserStore } from '../stores/useUserStore'
import { useMetricsStore } from '../stores/useMetricsStore'

export default {
  props: {
    id: {
      type: [Number, String],
      default: null
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close', 'updated'],
  setup(props, { emit }) {
        const authStore = useAuthStore()
    const oppStore = useOppStore()
    const appStore = useAppStore()
    const userStore = useUserStore()
    const metricsStore = useMetricsStore()
const opp = ref(null)
    const activeTab = ref('summary')
    const changeLogs = ref([])

    // Subtask input
    const newTaskText = ref('')

    // Activity input
    const newActivityContent = ref('')
    const activeActivityType = ref('call')
    const activityTypes = [
      { key: 'call', label: '电话沟通' },
      { key: 'meeting', label: '线下会议' },
      { key: 'email', label: '电子邮件' },
      { key: 'system', label: '商务备忘' }
    ]

    const STAGES = {
      prospecting: "发现商机",
      qualification: "资质评估",
      proposal: "方案报价",
      negotiation: "谈判协商",
      won: "赢得商机",
      lost: "流失商机"
    }

    const formatCurrency = (val) => {
      return new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: 'CNY',
        minimumFractionDigits: 2
      }).format(val || 0)
    }

    const fetchDetail = async (oppId) => {
      if (!oppId) return
      try {
        const response = await axios.get(`${API_BASE}/opportunities/${oppId}`, {
          params: { userId: authStore.user?.userId, userName: authStore.user?.name }
        })
        opp.value = response.data
        changeLogs.value = await oppStore.fetchOpportunityChangeLogs(oppId)
      } catch (err) {
        console.error("Failed to load opportunity drawer details", err)
      }
    }

    const permissionLabel = (source) => {
      if (source === 'ADMIN') return '管理员'
      if (source === 'WHITELIST') return '白名单授权'
      if (source === 'SYSTEM') return '系统/OA回写'
      return '本人'
    }

    const lockStatusLabel = (status) => {
      if (status === 'SOFT_LOCKED') return '临时冻结'
      if (status === 'HARD_LOCKED') return '06节点硬锁定'
      return '未锁定'
    }

    const lockStatusClass = (status) => {
      if (status === 'SOFT_LOCKED') return 'soft'
      if (status === 'HARD_LOCKED') return 'hard'
      return 'open'
    }

    const reportStatusLabel = (status) => {
      const labels = {
        NOT_STARTED: '未发起',
        IN_PROGRESS: '审批中',
        ARCHIVED: '已归档',
        REJECTED: '已驳回',
        CANCELED: '已撤回/作废'
      }
      return labels[status] || '未发起'
    }

    const bidStatusLabel = (status) => {
      const labels = {
        NOT_STARTED: '未发起',
        IN_PROGRESS: '审批中',
        NODE_06_REACHED: '已到06节点',
        APPROVED: '已通过',
        REJECTED: '已驳回',
        CANCELED: '已撤回/作废'
      }
      return labels[status] || '未发起'
    }

    const canStartReportFlow = (target) => {
      return target && ['NOT_STARTED', 'REJECTED', 'CANCELED', null, undefined, ''].includes(target.reportFlowStatus)
    }

    const handleClose = () => {
      emit('close')
      opp.value = null
    }

    const handleAddTask = async () => {
      const text = newTaskText.value.trim()
      if (!text || !opp.value) return

      const added = await oppStore.addTask(opp.value.id, text)
      if (added) {
        newTaskText.value = ''
        await fetchDetail(opp.value.id)
        emit('updated')
      }
    }

    const handleToggleTask = async (task) => {
      if (!opp.value) return
      const updated = await oppStore.toggleTask(opp.value.id, task.id)
      if (updated) {
        await fetchDetail(opp.value.id)
        emit('updated')
      }
    }

    const handleAddActivity = async () => {
      const content = newActivityContent.value.trim()
      if (!content || !opp.value) return

      const added = await oppStore.addActivity(opp.value.id, activeActivityType.value, content)
      if (added) {
        newActivityContent.value = ''
        await fetchDetail(opp.value.id)
        emit('updated')
      }
    }

    const handleStartReportFlow = async () => {
      if (!opp.value) return
      const updated = await oppStore.startOaReportFlow(opp.value.id)
      if (updated) {
        await fetchDetail(opp.value.id)
        emit('updated')
      }
    }

    // Watch drawer visibility to trigger query
    watch(() => props.visible, async (newVal) => {
      if (newVal && props.id) {
        activeTab.value = 'summary'
        await fetchDetail(props.id)
      }
    })

    // Watch opportunity id directly in case it switches
    watch(() => props.id, async (newId) => {
      if (props.visible && newId) {
        await fetchDetail(newId)
      }
    })

    return {
      opp,
      activeTab,
      changeLogs,
      newTaskText,
      newActivityContent,
      activeActivityType,
      activityTypes,
      STAGES,
      formatCurrency,
      permissionLabel,
      lockStatusLabel,
      lockStatusClass,
      reportStatusLabel,
      bidStatusLabel,
      canStartReportFlow,
      handleClose,
      handleAddTask,
      handleToggleTask,
      handleAddActivity,
      handleStartReportFlow
    }
  }
}
</script>
