<template>
  <section class="content-panel active list-page">
    <div class="mobile-filter-shell">
      <div class="mobile-filter-summary">
        <div class="search-input-wrapper mobile-search">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" x2="16.65" y1="21" y2="16.65"/></svg>
          <input type="text" class="form-control" v-model="filters.search" placeholder="检索采购单位、项目名称、供应商或型号...">
        </div>

        <div class="mobile-filter-actions">
          <button class="btn-secondary mobile-filter-toggle" type="button" @click="toggleAdvancedFilters">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
            </svg>
            <span>{{ filtersExpanded ? '收起筛选' : '高级筛选' }}</span>
            <span v-if="activeFilterCount" class="filter-count-badge">{{ activeFilterCount }}</span>
          </button>
          <button class="btn-primary" type="button" @click="handleExport" style="margin-left: 8px;">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
            <span>导出</span>
          </button>
          <button v-if="activeFilterCount" class="btn-secondary mobile-reset-btn" type="button" @click="resetFilters" style="margin-left: 8px;">
            重置
          </button>
        </div>
      </div>

      <div v-if="activeFilterCount" class="active-filter-tags">
        <span v-for="item in activeFilterLabels" :key="item" class="active-filter-tag">{{ item }}</span>
      </div>

      <transition name="filter-collapse">
        <div v-if="filtersExpanded" class="mobile-filter-panel">
          <div class="mobile-filter-grid">
            <div class="form-group">
              <label class="form-label">行业</label>
              <select class="form-control" v-model="filters.industry">
                <option value="all">所有行业</option>
                <option v-for="item in optionList('industries')" :key="item" :value="item">{{ item }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">供货省区</label>
              <select class="form-control" v-model="filters.supplyRegion">
                <option value="all">所有供货省区</option>
                <option v-for="item in optionList('regions')" :key="item" :value="item">{{ item }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">业务进度</label>
              <select class="form-control" v-model="filters.businessProgressStatus">
                <option value="all">所有业务进度</option>
                <option v-for="item in optionList('businessProgressStatuses')" :key="item" :value="item">{{ item }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">创建时间 (起)</label>
              <input type="date" class="form-control" v-model="filters.startDate">
            </div>
            <div class="form-group">
              <label class="form-label">创建时间 (止)</label>
              <input type="date" class="form-control" v-model="filters.endDate">
            </div>
            <div class="form-group">
              <label class="form-label">商机阶段</label>
              <select class="form-control" v-model="filters.stage">
                <option value="all">所有阶段</option>
                <option v-for="(lbl, key) in STAGES" :key="key" :value="key">{{ lbl }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">优先级</label>
              <select class="form-control" v-model="filters.priority">
                <option value="all">所有优先级</option>
                <option value="high">高</option>
                <option value="medium">中</option>
                <option value="low">低</option>
              </select>
            </div>
            <div class="form-group" style="grid-column: 1 / -1;">
              <label class="form-label">提报人/负责人</label>
              <input type="text" class="form-control" v-model="filters.ownerOrSubmitter" placeholder="输入姓名进行模糊搜索...">
            </div>
          </div>

          <div class="mobile-filter-footer">
            <button class="btn-secondary" type="button" @click="resetFilters">重置筛选</button>
            <button class="btn-primary" type="button" @click="filtersExpanded = false">完成</button>
          </div>
        </div>
      </transition>
    </div>

    <div class="mobile-list-card">
      <div class="list-meta-row">
        <div>
          <strong>商机结果</strong>
          <p>{{ oppStore.totalElements }} 条结果</p>
        </div>
      </div>

      <div class="data-view-card">
        <div class="table-container mobile-opportunity-table">
          <table class="custom-table opportunity-table">
            <thead>
              <tr>
                <th>项目名称</th>
                <th>采购单位</th>
                <th>行业</th>
                <th>设备类型</th>
                <th>预估金额</th>
                <th>赢率</th>
                <th>业务进度</th>
                <th>供货省区</th>
                <th>投标截止</th>
                <th style="text-align: right;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="opp in opportunities" :key="opp.id">
                <td>
                  <a href="#" @click.prevent="openDetail(opp.id)" style="color: var(--primary); font-weight: 700; text-decoration: none;">
                    {{ opp.name || '未命名项目' }}
                  </a>
                  <div class="muted-line">提报人：{{ opp.submitter || opp.creator || '-' }}</div>
                </td>
                <td>{{ opp.company || '-' }}</td>
                <td>{{ opp.industry || '-' }}</td>
                <td>
                  {{ opp.deviceTypes || '-' }}
                  <div v-if="isDeviceTypeLocked(opp)" class="muted-line">{{ deviceTypeLockText(opp) }}</div>
                </td>
                <td style="font-weight: 700;">{{ formatBusinessAmount(opp) }}</td>
                <td>{{ opp.winRateLabel || `${opp.probability || 0}%` }}</td>
                <td><span class="progress-pill">{{ opp.businessProgressStatus || '新提报' }}</span></td>
                <td>{{ opp.supplyRegion || '-' }}</td>
                <td>{{ opp.bidDeadline || opp.closeDate || '-' }}</td>
                <td style="text-align: right;">
                  <div style="display: inline-flex; gap: 8px;">
                    <button class="btn-icon" @click="openEditOpp(opp.id)" title="编辑自己提报的信息" style="width: 32px; height: 32px;">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 4 4"/></svg>
                    </button>
                    <button class="btn-icon" @click="handleDelete(opp)" title="删除" style="width: 32px; height: 32px; border-color: var(--danger-light); color: var(--danger);">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="opportunities.length === 0">
                <td colspan="10" style="text-align: center; color: var(--text-muted); padding: 32px 0;">没有查找到符合条件的商机提报</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-wrapper" style="display: flex; justify-content: flex-end; align-items: center; padding: 16px; gap: 16px;">
          <span style="color: var(--text-muted); font-size: 14px;">共 {{ oppStore.totalElements }} 条数据</span>
          <div style="display: flex; gap: 8px;">
            <button class="btn-secondary" :disabled="oppStore.page === 0" @click="prevPage">上一页</button>
            <span style="display: inline-flex; align-items: center; font-weight: 500;">
              {{ oppStore.page + 1 }} / {{ Math.max(1, oppStore.totalPages) }}
            </span>
            <button class="btn-secondary" :disabled="oppStore.page >= oppStore.totalPages - 1" @click="nextPage">下一页</button>
          </div>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="modalVisible">
      <div class="modal-content opportunity-modal">
        <div class="modal-header">
          <div>
            <h3 class="modal-title">{{ isEdit ? '编辑商机提报' : '新增商机提报' }}</h3>
            <div class="muted-line">手机端与 PC 端均按 Excel 商机池全字段提报</div>
          </div>
          <button class="btn-icon" @click="modalVisible = false" style="border: none; background: transparent;">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" x2="6" y1="6" y2="18"/><line x1="6" x2="18" y1="6" y2="18"/></svg>
          </button>
        </div>

        <form @submit.prevent="handleSubmit">
          <div class="modal-body">
            <div class="form-step-tabs">
              <button type="button" v-for="(step, index) in formSteps" :key="step" class="step-tab" :class="{ active: activeStep === index }" @click="activeStep = index">
                {{ index + 1 }}. {{ step }}
              </button>
            </div>

            <div v-if="duplicateMatches.length" class="duplicate-panel">
              <strong>可能存在重复商机：</strong>
              <div v-for="match in duplicateMatches" :key="match.id" class="duplicate-row">
                {{ match.name }} / {{ match.company }} / 提报人：{{ match.submitter || match.creator || '-' }}
              </div>
            </div>

            <div class="form-grid" v-if="activeStep === 0">
              <Field label="采购单位 *"><input class="form-control" v-model="form.company" required></Field>
              <Field label="项目名称 *"><input class="form-control" v-model="form.name" required></Field>
              <Field label="行业 *"><SelectInput v-model="form.industry" :items="optionList('industries')" required /></Field>
              <Field label="供应商公司名称"><input class="form-control" v-model="form.supplierCompany"></Field>
              <Field label="提报人"><input class="form-control" v-model="form.submitter"></Field>
              <Field label="政企市场部经理"><input class="form-control" v-model="form.govMarketManager"></Field>
              <Field label="提报人省区"><input class="form-control" v-model="form.submitterRegion"></Field>
              <Field label="供货省区"><SelectInput v-model="form.supplyRegion" :items="optionList('regions')" /></Field>
              <Field label="销售部门"><input class="form-control" v-model="form.salesDepartment"></Field>
              <Field label="销售"><input class="form-control" v-model="form.sales"></Field>
            </div>

            <div class="form-grid" v-if="activeStep === 1">
              <Field label="需求设备类型 *">
                <MultiSelect v-model="form.deviceTypes" :items="optionList('deviceTypes')" :disabled="isDeviceTypeInputDisabled(form)" required />
                <div v-if="isDeviceTypeLocked(form)" class="form-help warning">{{ deviceTypeEditHelpText(form) }}</div>
              </Field>
              <Field label="需求设备品类型号 *"><input class="form-control" v-model="form.deviceModels" required></Field>
              <Field label="需求数量（台）"><input type="number" class="form-control" v-model.number="form.demandQuantity" min="0"></Field>
              <Field label="预估采购金额 *"><input type="number" class="form-control" v-model.number="form.estimatedPurchaseAmount" min="0" step="0.01" required></Field>
              <Field label="金额单位 *"><SelectInput v-model="form.estimatedPurchaseAmountUnit" :items="optionList('amountUnits')" required /></Field>
            </div>

            <div class="form-grid" v-if="activeStep === 2">
              <Field label="赢率 *"><SelectInput v-model="form.winRateLabel" :items="optionList('winRates')" required /></Field>
              <Field label="业务进度 *"><SelectInput v-model="form.businessProgressStatus" :items="optionList('businessProgressStatuses')" required /></Field>
              <Field label="是否可以提前写参数"><BooleanSelect v-model="form.canPrepareParams" /></Field>
              <Field label="是否需要唯一授权"><BooleanSelect v-model="form.requiresExclusiveAuthorization" /></Field>
              <Field label="需授权品类"><MultiSelect v-model="form.authorizedCategories" :items="optionList('authorizedCategories')" /></Field>
              <Field label="是否报备成功"><BooleanSelect v-model="form.reportedSuccessfully" /></Field>
            </div>

            <div class="form-grid" v-if="activeStep === 3">
              <Field label="预计投标截止时间"><input type="date" class="form-control" v-model="form.bidDeadline"></Field>
              <Field label="预计交付时间"><input type="date" class="form-control" v-model="form.expectedDeliveryDate"></Field>
              <Field label="是否中标"><BooleanSelect v-model="form.bidWon" /></Field>
              <Field label="采购形式"><SelectInput v-model="form.purchaseType" :items="optionList('purchaseTypes')" /></Field>
              <Field label="项目中标金额"><input type="number" class="form-control" v-model.number="form.winningAmount" min="0" step="0.01"></Field>
              <Field label="中标金额单位"><SelectInput v-model="form.winningAmountUnit" :items="optionList('amountUnits')" /></Field>
              <Field label="得力设备BG交付金额"><input type="number" class="form-control" v-model.number="form.deliBgDeliveryAmount" min="0" step="0.01"></Field>
              <Field label="交付金额单位"><SelectInput v-model="form.deliBgDeliveryAmountUnit" :items="optionList('amountUnits')" /></Field>
            </div>

            <div class="form-grid" v-if="activeStep === 4">
              <Field label="省总"><input class="form-control" v-model="form.provinceGeneralManager"></Field>
              <Field label="A4业务经理"><input class="form-control" v-model="form.a4BusinessManager"></Field>
              <Field label="A3业务经理"><input class="form-control" v-model="form.a3BusinessManager"></Field>
              <Field label="商机阶段"><select class="form-control" v-model="form.stage"><option v-for="(lbl, key) in STAGES" :key="key" :value="key">{{ lbl }}</option></select></Field>
              <Field label="优先级"><select class="form-control" v-model="form.priority"><option value="high">高</option><option value="medium">中</option><option value="low">低</option></select></Field>
              <Field label="负责人"><input class="form-control" v-model="form.owner"></Field>
            </div>

            <div class="form-grid" v-if="activeStep === 5">
              <Field label="本周项目进展/备注" full><textarea class="form-control form-textarea" v-model="form.weeklyProgress" required></textarea></Field>
              <Field label="商机描述" full><textarea class="form-control form-textarea" v-model="form.description"></textarea></Field>
              <Field label="附件记录" full>
                <div class="attachment-input-row">
                  <input class="form-control" v-model="attachmentDraft.fileName" placeholder="附件名称，如 招标公告.pdf">
                  <select class="form-control" v-model="attachmentDraft.fileType">
                    <option value="tender_notice">招标公告</option>
                    <option value="params">参数文件</option>
                    <option value="authorization">授权材料</option>
                    <option value="reporting">报备材料</option>
                    <option value="quote">报价文件</option>
                    <option value="other">其他附件</option>
                  </select>
                  <button class="btn-secondary" type="button" @click="addLocalAttachment">添加</button>
                </div>
                <div class="attachment-list">
                  <span v-for="(file, index) in form.attachments" :key="index" class="attachment-chip">
                    {{ file.fileName }}
                    <button type="button" @click="form.attachments.splice(index, 1)">x</button>
                  </span>
                </div>
              </Field>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="prevStep" :disabled="activeStep === 0">上一步</button>
            <button type="button" class="btn-secondary" @click="nextStep" v-if="activeStep < formSteps.length - 1">下一步</button>
            <button type="submit" class="btn-primary" v-else>{{ isEdit ? '保存提报' : '提交提报' }}</button>
          </div>
        </form>
      </div>
    </div>

    <OpportunityDrawer :id="selectedOppId" :visible="drawerVisible" @close="closeDrawer" @updated="onOppUpdated" />
  </section>
</template>

<script>
import { computed, defineComponent, h, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useAuthStore } from '../stores/useAuthStore'
import { useOppStore } from '../stores/useOppStore'
import { useAppStore } from '../stores/useAppStore'
import { useMetricsStore } from '../stores/useMetricsStore'
import OpportunityDrawer from '../components/OpportunityDrawer.vue'

const Field = defineComponent({
  props: { label: String, full: Boolean },
  setup(props, { slots }) {
    return () => h('div', { class: ['form-group', props.full ? 'form-group-full' : ''] }, [
      h('label', { class: 'form-label' }, props.label),
      slots.default ? slots.default() : null
    ])
  }
})

const SelectInput = defineComponent({
  props: { modelValue: [String, Number], items: Array, required: Boolean },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('select', {
      class: 'form-control',
      value: props.modelValue || '',
      required: props.required,
      onChange: e => emit('update:modelValue', e.target.value)
    }, [
      h('option', { value: '' }, '请选择'),
      ...(props.items || []).map(item => h('option', { value: item }, item))
    ])
  }
})

const BooleanSelect = defineComponent({
  props: { modelValue: [Boolean, String] },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const toValue = (val) => val === true ? 'true' : val === false ? 'false' : ''
    return () => h('select', {
      class: 'form-control',
      value: toValue(props.modelValue),
      onChange: e => emit('update:modelValue', e.target.value === '' ? null : e.target.value === 'true')
    }, [
      h('option', { value: '' }, '未确定'),
      h('option', { value: 'true' }, '是'),
      h('option', { value: 'false' }, '否')
    ])
  }
})

const MultiSelect = defineComponent({
  props: { modelValue: String, items: Array, required: Boolean, disabled: Boolean },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const toggle = (item) => {
      if (props.disabled) return
      const values = (props.modelValue || '').split(',').map(v => v.trim()).filter(Boolean)
      const next = values.includes(item) ? values.filter(v => v !== item) : [...values, item]
      emit('update:modelValue', next.join(', '))
    }
    return () => h('div', { class: 'multi-select-box' }, (props.items || []).map(item => {
      const values = (props.modelValue || '').split(',').map(v => v.trim()).filter(Boolean)
      return h('button', {
        type: 'button',
        disabled: props.disabled,
        class: ['multi-option', values.includes(item) ? 'active' : '', props.disabled ? 'disabled' : ''],
        onClick: () => toggle(item)
      }, item)
    }))
  }
})

export default {
  components: { OpportunityDrawer, Field, SelectInput, BooleanSelect, MultiSelect },
  setup() {
        const authStore = useAuthStore()
    const oppStore = useOppStore()
    const appStore = useAppStore()
    const metricsStore = useMetricsStore()
const readUiPrefs = () => {
      try {
        return JSON.parse(localStorage.getItem('crm_admin_ui_prefs') || '{}')
      } catch (e) {
        return {}
      }
    }
    const STAGES = {
      prospecting: "发现商机",
      qualification: "资质评估",
      proposal: "方案报价",
      negotiation: "谈判协商",
      won: "赢得商机",
      lost: "流失商机"
    }

    const filters = reactive({
      search: '',
      industry: 'all',
      supplyRegion: 'all',
      businessProgressStatus: 'all',
      startDate: '',
      endDate: '',
      stage: 'all',
      priority: 'all',
      ownerOrSubmitter: ''
    })

    const filtersExpanded = ref(false)
    const modalVisible = ref(false)
    const isEdit = ref(false)
    const form = ref({})
    const activeStep = ref(0)
    const drawerVisible = ref(false)
    const selectedOppId = ref(null)
    const duplicateMatches = ref([])
    const attachmentDraft = reactive({ fileName: '', fileType: 'other' })
    const formSteps = ['基础信息', '设备需求', '授权报备', '投标交付', '组织归属', '进展附件']

    const optionList = (key) => oppStore.opportunityOptions?.[key] || []

    const activeFilterLabels = computed(() => {
      const labels = []
      if (filters.industry !== 'all') labels.push(`行业：${filters.industry}`)
      if (filters.supplyRegion !== 'all') labels.push(`供货省区：${filters.supplyRegion}`)
      if (filters.businessProgressStatus !== 'all') labels.push(`业务进度：${filters.businessProgressStatus}`)
      if (filters.startDate) labels.push(`开始时间：${filters.startDate}`)
      if (filters.endDate) labels.push(`结束时间：${filters.endDate}`)
      if (filters.stage !== 'all') labels.push(`阶段：${STAGES[filters.stage] || filters.stage}`)
      if (filters.priority !== 'all') labels.push(`优先级：${filters.priority === 'high' ? '高' : filters.priority === 'medium' ? '中' : '低'}`)
      if (filters.ownerOrSubmitter) labels.push(`人员：${filters.ownerOrSubmitter}`)
      return labels
    })

    const activeFilterCount = computed(() => activeFilterLabels.value.length)

    const formatBusinessAmount = (opp) => {
      if (opp.estimatedPurchaseAmount !== null && opp.estimatedPurchaseAmount !== undefined) {
        return `${opp.estimatedPurchaseAmount}${opp.estimatedPurchaseAmountUnit || ''}`
      }
      return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', minimumFractionDigits: 0 }).format(opp.value || 0)
    }

    const isDeviceTypeLocked = (opp) => {
      return ['SOFT_LOCKED', 'HARD_LOCKED'].includes(opp?.deviceRequirementLockStatus)
    }

    const canOverrideLockedDeviceType = computed(() => {
      return authStore.user?.role === 'ADMIN' || authStore.user?.canViewAll
    })

    const isDeviceTypeInputDisabled = (opp) => {
      return isDeviceTypeLocked(opp) && !canOverrideLockedDeviceType.value
    }

    const deviceTypeLockText = (opp) => {
      if (opp?.deviceRequirementLockStatus === 'HARD_LOCKED') {
        return '06节点后已硬锁定'
      }
      if (opp?.deviceRequirementLockStatus === 'SOFT_LOCKED') {
        return '投标流程中临时冻结'
      }
      return ''
    }

    const deviceTypeEditHelpText = (opp) => {
      const text = deviceTypeLockText(opp)
      if (!text) return ''
      if (canOverrideLockedDeviceType.value) {
        return `${text}，管理员特殊修正会记录高风险操作`
      }
      return text
    }

    const newForm = () => ({
      name: '',
      company: '',
      stage: 'prospecting',
      priority: 'medium',
      owner: authStore.user?.name || '张经理',
      submitter: authStore.user?.name || '张经理',
      creator: authStore.user?.name || '张经理',
      industry: '',
      supplierCompany: '',
      govMarketManager: '',
      submitterRegion: '',
      supplyRegion: '',
      salesDepartment: '设备省公司',
      sales: authStore.user?.name || '张经理',
      deviceTypes: '',
      deviceModels: '',
      demandQuantity: null,
      estimatedPurchaseAmount: null,
      estimatedPurchaseAmountUnit: '万元',
      winRateLabel: '50%-可以参与',
      businessProgressStatus: '新提报',
      canPrepareParams: null,
      requiresExclusiveAuthorization: null,
      authorizedCategories: '',
      reportedSuccessfully: null,
      bidDeadline: '',
      expectedDeliveryDate: '',
      bidWon: null,
      purchaseType: '',
      winningAmount: null,
      winningAmountUnit: '万元',
      deliBgDeliveryAmount: null,
      deliBgDeliveryAmountUnit: '万元',
      provinceGeneralManager: '',
      a4BusinessManager: '',
      a3BusinessManager: '',
      weeklyProgress: '',
      description: '',
      attachments: []
    })

    const resetFilters = () => {
      filters.search = ''
      filters.industry = 'all'
      filters.supplyRegion = 'all'
      filters.businessProgressStatus = 'all'
      filters.startDate = ''
      filters.endDate = ''
      filters.stage = 'all'
      filters.priority = 'all'
      filters.ownerOrSubmitter = ''
    }

    const toggleAdvancedFilters = () => {
      filtersExpanded.value = !filtersExpanded.value
    }

    const initNewOpp = () => {
      isEdit.value = false
      form.value = newForm()
      activeStep.value = 0
      duplicateMatches.value = []
      modalVisible.value = true
    }

    const openEditOpp = (oppId) => {
      const opp = oppStore.opportunities.find(o => o.id === oppId)
      if (opp) {
        isEdit.value = true
        form.value = { ...newForm(), ...opp, attachments: [...(opp.attachments || [])] }
        activeStep.value = 0
        duplicateMatches.value = []
        modalVisible.value = true
      }
    }

    const nextStep = async () => {
      if (activeStep.value === 0 || activeStep.value === 1) {
        const result = await oppStore.checkDuplicates(form.value)
        duplicateMatches.value = result.matches || []
      }
      if (activeStep.value < formSteps.length - 1) activeStep.value += 1
    }

    const prevStep = () => {
      if (activeStep.value > 0) activeStep.value -= 1
    }

    const addLocalAttachment = () => {
      if (!attachmentDraft.fileName.trim()) return
      form.value.attachments.push({
        fileName: attachmentDraft.fileName.trim(),
        fileType: attachmentDraft.fileType,
        fileUrl: `/mock-files/${attachmentDraft.fileName.trim()}`
      })
      attachmentDraft.fileName = ''
      attachmentDraft.fileType = 'other'
    }

    const handleSubmit = async () => {
      const payload = { ...form.value }
      const attachments = [...(payload.attachments || [])]
      delete payload.attachments
      if (isEdit.value) {
        await oppStore.updateOpp(payload.id, payload)
        for (const file of attachments.filter(a => !a.id)) {
          await oppStore.addAttachment(payload.id, file)
        }
      } else {
        await oppStore.submitOpportunity({ ...payload, attachments })
      }
      modalVisible.value = false
    }

    const handleDelete = async (opp) => {
      if (confirm(`确定要删除该商机提报 [${opp.name}] 吗？`)) {
        await oppStore.deleteOpp(opp.id)
      }
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
      await oppStore.fetchOpportunities(filters)
    }

    const prevPage = () => {
      console.log('prevPage clicked, current store page:', oppStore.page)
      if (oppStore.page > 0) {
        oppStore.fetchOpportunities(filters, oppStore.page - 1)
      }
    }

    const nextPage = () => {
      console.log('nextPage clicked, current store page:', oppStore.page, 'totalPages:', oppStore.totalPages)
      if (oppStore.page < oppStore.totalPages - 1) {
        oppStore.fetchOpportunities(filters, oppStore.page + 1)
      }
    }

    watch(filters, (newFilters) => {
      // Reset page to 0 when filters change
      oppStore.fetchOpportunities(newFilters, 0)
    }, { deep: true })

    onMounted(async () => {
      await oppStore.fetchOpportunityOptions()
      await oppStore.fetchOpportunities(filters)
      window.addEventListener('open-new-opp-modal', initNewOpp)
    })

    onUnmounted(() => {
      window.removeEventListener('open-new-opp-modal', initNewOpp)
    })

    const handleExport = async () => {
      try {
        // Build query string
        const query = new URLSearchParams()
        if (filters.search) query.append('search', filters.search)
        if (filters.industry !== 'all') query.append('industry', filters.industry)
        if (filters.supplyRegion !== 'all') query.append('supplyRegion', filters.supplyRegion)
        if (filters.businessProgressStatus !== 'all') query.append('businessProgressStatus', filters.businessProgressStatus)
        if (filters.startDate) query.append('startDate', filters.startDate)
        if (filters.endDate) query.append('endDate', filters.endDate)
        if (filters.stage !== 'all') query.append('stage', filters.stage)
        if (filters.priority !== 'all') query.append('priority', filters.priority)
        if (filters.ownerOrSubmitter) query.append('ownerOrSubmitter', filters.ownerOrSubmitter)

        // Add authorization header
        const token = authStore.user?.token
        if (!token) {
          throw new Error('当前登录状态已失效，请重新登录')
        }
        const response = await fetch(`/api/opportunities/export-excel?${query.toString()}`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })

        if (!response.ok) {
          throw new Error('导出失败')
        }

        const blob = await response.blob()
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `商机导出_${new Date().getTime()}.xlsx`
        document.body.appendChild(a)
        a.click()
        a.remove()
        window.URL.revokeObjectURL(url)
      } catch (err) {
        alert('导出失败: ' + err.message)
      }
    }

    return {
      oppStore,
      STAGES,
      opportunities: computed(() => oppStore.opportunities),
      filters,
      filtersExpanded,
      activeFilterCount,
      activeFilterLabels,
      modalVisible,
      isEdit,
      form,
      activeStep,
      formSteps,
      duplicateMatches,
      attachmentDraft,
      drawerVisible,
      selectedOppId,
      optionList,
      formatBusinessAmount,
      isDeviceTypeLocked,
      isDeviceTypeInputDisabled,
      deviceTypeLockText,
      deviceTypeEditHelpText,
      resetFilters,
      toggleAdvancedFilters,
      openEditOpp,
      handleExport,
      handleSubmit,
      handleDelete,
      openDetail,
      closeDrawer,
      onOppUpdated,
      nextStep,
      prevStep,
      addLocalAttachment,
      prevPage,
      nextPage
    }
  }
}
</script>
