<template>
  <section class="content-panel kanban-page">
    <div class="kanban-shell">
      <div class="kanban-toolbar-card">
        <div class="kanban-toolbar-left">
          <select class="form-control kanban-view-select">
            <option>全部商机</option>
            <option>我的商机</option>
            <option>白名单范围商机</option>
          </select>
        </div>
        <div class="kanban-toolbar-right">
          <button class="btn-secondary kanban-action-btn" type="button">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
            <span>筛选</span>
          </button>
          <button class="btn-primary kanban-action-btn" type="button" @click="openCreateModal">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14"/><path d="M12 5v14"/></svg>
            <span>新建商机</span>
          </button>
        </div>
      </div>

      <div class="crm-board-grid">
        <div
          v-for="(stageLabel, stageKey) in boardStages"
          :key="stageKey"
          class="crm-board-column"
          :class="{ 'drag-over': activeDragStage === stageKey }"
          @dragover.prevent
          @dragenter="activeDragStage = stageKey"
          @dragleave="activeDragStage = null"
          @drop="handleDrop($event, stageKey)"
        >
          <div class="crm-board-column-header" :style="{ '--stage-accent': STAGE_COLORS[stageKey] }">
            <div class="crm-board-column-top">
              <div>
                <strong>{{ stageLabel }}</strong>
                <p>{{ getStageOpps(stageKey).length }} 个商机</p>
              </div>
              <button class="crm-column-more" type="button" aria-label="更多操作">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg>
              </button>
            </div>
            <span class="crm-column-amount">{{ formatCurrencyAbbr(getStageValueTotal(stageKey)) }}</span>
          </div>

          <div class="crm-board-card-list">
            <button
              v-for="opp in getStageOpps(stageKey)"
              :key="opp.id"
              type="button"
              class="crm-opp-card"
              draggable="true"
              @dragstart="handleDragStart($event, opp.id)"
              @click="openDetail(opp.id)"
            >
              <div class="crm-opp-card-title">{{ opp.name || '未命名商机' }}</div>
              <div class="crm-opp-card-company">{{ opp.company || '未填写采购单位' }}</div>
              <div class="crm-opp-card-meta">
                <span>负责人：{{ opp.owner || opp.submitter || opp.creator || '-' }}</span>
                <span>预估金额：{{ formatKanbanAmount(opp) }}</span>
                <span>预计成交日期：{{ opp.bidDeadline || opp.closeDate || '-' }}</span>
              </div>
              <div class="crm-opp-card-footer">
                <span :class="['crm-priority-chip', `crm-priority-${opp.priority || 'medium'}`]">
                  {{ priorityLabel(opp.priority) }}
                </span>
                <span class="crm-stage-inline">{{ opp.businessProgressStatus || stageLabel }}</span>
              </div>
            </button>

            <div v-if="getStageOpps(stageKey).length === 0" class="crm-empty-column">
              暂无当前阶段商机
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="createModalVisible">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">新增销售商机</h3>
          <button class="btn-icon" @click="createModalVisible = false" style="border: none; background: transparent;">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" x2="6" y1="6" y2="18"/><line x1="6" x2="18" y1="6" y2="18"/></svg>
          </button>
        </div>
        <form @submit.prevent="handleCreateSubmit">
          <div class="modal-body">
            <div class="form-grid">
              <div class="form-group form-group-full">
                <label class="form-label">商机名称 *</label>
                <input type="text" class="form-control" v-model="newOpp.name" required placeholder="如：极客科技客服采购项目">
              </div>
              <div class="form-group">
                <label class="form-label">客户名称 / 公司 *</label>
                <input type="text" class="form-control" v-model="newOpp.company" required placeholder="如：极客科技股份有限公司">
              </div>
              <div class="form-group">
                <label class="form-label">预估金额 (元) *</label>
                <input type="number" class="form-control" v-model="newOpp.value" required min="0" placeholder="数字金额，如 100000">
              </div>
              <div class="form-group">
                <label class="form-label">当前阶段 *</label>
                <select class="form-control" v-model="newOpp.stage" @change="handleStageChange" required>
                  <option v-for="(lbl, key) in STAGES" :key="key" :value="key">{{ lbl }}</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">赢单概率 (%) *</label>
                <input type="number" class="form-control" v-model="newOpp.probability" required min="0" max="100" placeholder="0-100">
              </div>
              <div class="form-group">
                <label class="form-label">预计结单日期 *</label>
                <input type="date" class="form-control" v-model="newOpp.closeDate" required>
              </div>
              <div class="form-group">
                <label class="form-label">优先级 *</label>
                <select class="form-control" v-model="newOpp.priority" required>
                  <option value="high">高 (High)</option>
                  <option value="medium">中 (Medium)</option>
                  <option value="low">低 (Low)</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">负责人 *</label>
                <select class="form-control" v-model="newOpp.owner" required>
                  <option v-for="c in contacts" :key="c.userId" :value="c.name">{{ c.name }}</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">商机来源</label>
                <select class="form-control" v-model="newOpp.source">
                  <option value="线上注册">线上注册</option>
                  <option value="转介绍">转介绍</option>
                  <option value="展会活动">展会活动</option>
                  <option value="冷拓拜访">冷拓拜访</option>
                  <option value="电话咨询">电话咨询</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">联系人姓名</label>
                <input type="text" class="form-control" v-model="newOpp.contactName" placeholder="联系人">
              </div>
              <div class="form-group">
                <label class="form-label">联系电话</label>
                <input type="text" class="form-control" v-model="newOpp.contactPhone" placeholder="手机号">
              </div>
              <div class="form-group form-group-full">
                <label class="form-label">电子邮箱</label>
                <input type="email" class="form-control" v-model="newOpp.contactEmail" placeholder="example@domain.com">
              </div>
              <div class="form-group form-group-full">
                <label class="form-label">商机描述</label>
                <textarea class="form-control form-textarea" v-model="newOpp.description" placeholder="输入需求描述..."></textarea>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="createModalVisible = false">取消</button>
            <button type="submit" class="btn-primary">保存并指派飞书</button>
          </div>
        </form>
      </div>
    </div>

    <OpportunityDrawer
      :id="selectedOppId"
      :visible="drawerVisible"
      @close="closeDrawer"
      @updated="onOppUpdated"
    />
  </section>
</template>

<script>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/useAuthStore'
import { useOppStore } from '../stores/useOppStore'
import { useAppStore } from '../stores/useAppStore'
import { useUserStore } from '../stores/useUserStore'
import { useMetricsStore } from '../stores/useMetricsStore'
import OpportunityDrawer from '../components/OpportunityDrawer.vue'

export default {
  components: { OpportunityDrawer },
  setup() {
        const authStore = useAuthStore()
    const oppStore = useOppStore()
    const appStore = useAppStore()
    const userStore = useUserStore()
    const metricsStore = useMetricsStore()
const STAGES = {
      prospecting: "潜在线索",
      qualification: "需求确认",
      proposal: "方案报价",
      negotiation: "商务谈判",
      won: "成交赢单",
      lost: "商机流失"
    }

    const STAGE_COLORS = {
      prospecting: "#2684ff",
      qualification: "#16a3b5",
      proposal: "#2b9ed6",
      negotiation: "#2974e8",
      won: "#1ea97c",
      lost: "#ef4444"
    }

    const DEFAULT_PROBABILITIES = {
      prospecting: 20,
      qualification: 40,
      proposal: 60,
      negotiation: 80,
      won: 100,
      lost: 0
    }

    const createModalVisible = ref(false)
    const activeDragStage = ref(null)
    const newOpp = ref({
      name: '',
      company: '',
      value: 0,
      stage: 'prospecting',
      probability: 20,
      closeDate: '',
      priority: 'medium',
      owner: '张经理',
      source: '线上注册',
      contactName: '',
      contactPhone: '',
      contactEmail: '',
      description: ''
    })

    const drawerVisible = ref(false)
    const selectedOppId = ref(null)

    const boardStages = computed(() => {
      const allowed = ['prospecting', 'qualification', 'proposal', 'negotiation', 'won']
      return Object.fromEntries(Object.entries(STAGES).filter(([key]) => allowed.includes(key)))
    })

    const formatCurrency = (num) => {
      return new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: 'CNY',
        minimumFractionDigits: 0
      }).format(num || 0)
    }

    const formatCurrencyAbbr = (num) => {
      if (num >= 10000) {
        return `￥${(num / 10000).toFixed(1)}万`
      }
      return `￥${(num || 0).toFixed(0)}`
    }

    const formatKanbanAmount = (opp) => {
      if (opp.estimatedPurchaseAmount !== null && opp.estimatedPurchaseAmount !== undefined) {
        return `￥${opp.estimatedPurchaseAmount}${opp.estimatedPurchaseAmountUnit || ''}`
      }
      return formatCurrency(opp.value)
    }

    const priorityLabel = (priority) => {
      if (priority === 'high') return '高优先级'
      if (priority === 'low') return '低优先级'
      return '普通'
    }

    const getStageOpps = (stageKey) => {
      return oppStore.opportunities.filter(o => o.stage === stageKey)
    }

    const getStageValueTotal = (stageKey) => {
      return getStageOpps(stageKey).reduce((sum, o) => sum + (o.value || 0), 0)
    }

    let draggedOppId = null
    const handleDragStart = (e, oppId) => {
      draggedOppId = oppId
      e.dataTransfer.effectAllowed = 'move'
    }

    const handleDrop = async (e, targetStage) => {
      activeDragStage.value = null
      if (!draggedOppId) return

      const opp = oppStore.opportunities.find(o => o.id === draggedOppId)
      if (opp && opp.stage !== targetStage) {
        const updated = {
          ...opp,
          stage: targetStage
        }
        await oppStore.updateOpp(opp.id, updated)
      }
      draggedOppId = null
    }

    const openDetail = (oppId) => {
      selectedOppId.value = oppId
      drawerVisible.value = true
    }

    const closeDrawer = () => {
      drawerVisible.value = false
      selectedOppId.value = null
    }

    const onOppUpdated = async () => {
      await oppStore.fetchMyOpportunities()
    }

    const handleStageChange = () => {
      newOpp.value.probability = DEFAULT_PROBABILITIES[newOpp.value.stage] || 0
    }

    const initNewOppForm = () => {
      newOpp.value = {
        name: '',
        company: '',
        value: 0,
        stage: 'prospecting',
        probability: 20,
        closeDate: new Date(Date.now() + 30 * 24 * 3600 * 1000).toISOString().split('T')[0],
        priority: 'medium',
        owner: '张经理',
        source: '线上注册',
        contactName: '',
        contactPhone: '',
        contactEmail: '',
        description: ''
      }
      createModalVisible.value = true
    }

    const openCreateModal = () => {
      initNewOppForm()
    }

    const handleCreateSubmit = async () => {
      await oppStore.createOpp(newOpp.value)
      createModalVisible.value = false
    }

    onMounted(async () => {
      await oppStore.fetchMyOpportunities()
      await userStore.fetchContacts()
      window.addEventListener('open-new-opp-modal', initNewOppForm)
    })

    onUnmounted(() => {
      window.removeEventListener('open-new-opp-modal', initNewOppForm)
    })

    return {
      STAGES,
      STAGE_COLORS,
      boardStages,
      contacts: userStore.contacts,
      createModalVisible,
      activeDragStage,
      newOpp,
      drawerVisible,
      selectedOppId,
      formatCurrencyAbbr,
      formatKanbanAmount,
      priorityLabel,
      getStageOpps,
      getStageValueTotal,
      handleDragStart,
      handleDrop,
      openCreateModal,
      openDetail,
      closeDrawer,
      onOppUpdated,
      handleStageChange,
      handleCreateSubmit
    }
  }
}
</script>
