<template>
  <section class="content-panel active" style="overflow: hidden;">
    <div class="kanban-container">
      <!-- Stage columns -->
      <div 
        v-for="(stageLabel, stageKey) in STAGES" 
        :key="stageKey" 
        class="kanban-column"
        :class="{ 'drag-over': activeDragStage === stageKey }"
        @dragover.prevent
        @dragenter="activeDragStage = stageKey"
        @dragleave="activeDragStage = null"
        @drop="handleDrop($event, stageKey)"
      >
        <!-- Column Header -->
        <div class="kanban-column-header">
          <div class="kanban-column-title-group">
            <span class="kanban-indicator" :style="{ backgroundColor: STAGE_COLORS[stageKey] }"></span>
            <span class="kanban-column-title">{{ stageLabel }}</span>
            <span class="kanban-count-badge">{{ getStageOpps(stageKey).length }}</span>
          </div>
          <div style="font-size: 11px; font-weight: 700; color: var(--text-muted);">
            {{ formatCurrencyAbbr(getStageValueTotal(stageKey)) }}
          </div>
        </div>

        <!-- Column Cards Wrapper -->
        <div class="kanban-cards-wrapper">
          <div 
            v-for="opp in getStageOpps(stageKey)" 
            :key="opp.id" 
            class="kanban-card" 
            draggable="true" 
            @dragstart="handleDragStart($event, opp.id)"
            @click="openDetail(opp.id)"
          >
            <div class="kanban-card-company">{{ opp.company }}</div>
            <div class="kanban-card-title">{{ opp.name }}</div>
            <div class="kanban-card-value">{{ formatCurrency(opp.value) }}</div>
            <div class="kanban-card-footer">
              <div class="kanban-card-owner">
                <span class="kanban-card-avatar">{{ opp.owner.charAt(0) }}</span>
                <span>{{ opp.owner }}</span>
              </div>
              <span :class="`priority-badge priority-${opp.priority}`">
                {{ opp.priority === 'high' ? '高' : opp.priority === 'medium' ? '中' : '低' }}
              </span>
            </div>
          </div>

          <!-- Empty placeholder -->
          <div 
            v-if="getStageOpps(stageKey).length === 0" 
            style="display: flex; align-items: center; justify-content: center; height: 100px; border: 1px dashed var(--border-color); border-radius: var(--radius-md); color: var(--text-muted); font-size: 12px;"
          >
            无当前阶段商机
          </div>
        </div>
      </div>
    </div>

    <!-- Creation Modal -->
    <div class="modal-overlay" v-if="createModalVisible">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">新增销售商机</h3>
          <button class="btn-icon" @click="createModalVisible = false" style="border: none; background: transparent;">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-x"><line x1="18" x2="6" y1="6" y2="18"/><line x1="6" x2="18" y1="6" y2="18"/></svg>
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
            <button type="submit" class="btn-primary">保存并指派企微</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Opportunity detail sidebar drawer -->
    <OpportunityDrawer 
      :id="selectedOppId" 
      :visible="drawerVisible" 
      @close="closeDrawer" 
      @updated="onOppUpdated"
    />
  </section>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import { useStore } from '../store'
import OpportunityDrawer from '../components/OpportunityDrawer.vue'

export default {
  components: { OpportunityDrawer },
  setup() {
    const store = useStore()

    const STAGES = {
      prospecting: "发现商机",
      qualification: "资质评估",
      proposal: "方案报价",
      negotiation: "谈判协商",
      won: "赢得商机",
      lost: "流失商机"
    }

    const STAGE_COLORS = {
      prospecting: "#3b82f6",
      qualification: "#8b5cf6",
      proposal: "#f59e0b",
      negotiation: "#06b6d4",
      won: "#10b981",
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

    // Modal forms state
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

    // Drawer state
    const drawerVisible = ref(false)
    const selectedOppId = ref(null)

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

    const getStageOpps = (stageKey) => {
      return store.opportunities.value.filter(o => o.stage === stageKey)
    }

    const getStageValueTotal = (stageKey) => {
      return getStageOpps(stageKey).reduce((sum, o) => sum + (o.value || 0), 0)
    }

    // Drag-Drop
    let draggedOppId = null
    const handleDragStart = (e, oppId) => {
      draggedOppId = oppId
      e.dataTransfer.effectAllowed = 'move'
    }

    const handleDrop = async (e, targetStage) => {
      activeDragStage.value = null
      if (!draggedOppId) return

      const opp = store.opportunities.value.find(o => o.id === draggedOppId)
      if (opp && opp.stage !== targetStage) {
        // Prepare modifications
        const updated = {
          ...opp,
          stage: targetStage
        }
        await store.updateOpp(opp.id, updated)
      }
      draggedOppId = null
    }

    // Drawer actions
    const openDetail = (oppId) => {
      selectedOppId.value = oppId
      drawerVisible.value = true
    }

    const closeDrawer = () => {
      drawerVisible.value = false
      selectedOppId.value = null
    }

    const onOppUpdated = async () => {
      await store.fetchMyOpportunities()
    }

    // Creation logic
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
        closeDate: new Date(Date.now() + 30*24*3600*1000).toISOString().split('T')[0], // 30 days later
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

    const handleCreateSubmit = async () => {
      await store.createOpp(newOpp.value)
      createModalVisible.value = false
    }

    onMounted(async () => {
      await store.fetchMyOpportunities()
      await store.fetchContacts()

      // Bind listener to header trigger event
      window.addEventListener('open-new-opp-modal', initNewOppForm)
    })

    onUnmounted(() => {
      window.removeEventListener('open-new-opp-modal', initNewOppForm)
    })

    return {
      STAGES,
      STAGE_COLORS,
      contacts: store.contacts,
      createModalVisible,
      activeDragStage,
      newOpp,
      drawerVisible,
      selectedOppId,
      formatCurrency,
      formatCurrencyAbbr,
      getStageOpps,
      getStageValueTotal,
      handleDragStart,
      handleDrop,
      openDetail,
      closeDrawer,
      onOppUpdated,
      handleStageChange,
      handleCreateSubmit
    }
  }
}
</script>
