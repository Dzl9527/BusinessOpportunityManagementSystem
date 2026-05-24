<template>
  <section class="content-panel submit-panel">
    <div class="submit-shell">
      <div class="submit-summary">
        <div>
          <p class="submit-kicker">移动端提报</p>
          <h2>商机提报</h2>
          <p>按商机池字段分步填写，提交后自动生成提醒并进入商机列表。</p>
        </div>
        <div class="submit-progress">
          <span>{{ activeStep + 1 }}</span>
          <small>/ {{ formSteps.length }}</small>
        </div>
      </div>

      <div class="submit-steps">
        <button
          v-for="(step, index) in formSteps"
          :key="step"
          type="button"
          class="submit-step"
          :class="{ active: activeStep === index, done: index < activeStep }"
          @click="activeStep = index"
        >
          <span>{{ index + 1 }}</span>
          {{ step }}
        </button>
      </div>

      <form class="submit-form" @submit.prevent="handleSubmit">
        <div v-if="duplicateMatches.length" class="duplicate-panel">
          <strong>可能存在重复商机</strong>
          <div v-for="match in duplicateMatches" :key="match.id" class="duplicate-row">
            {{ match.name }} / {{ match.company }} / 提报人：{{ match.submitter || match.creator || '-' }}
          </div>
        </div>

        <div class="submit-section" v-if="activeStep === 0">
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

        <div class="submit-section" v-if="activeStep === 1">
          <Field label="需求设备类型 *" full><MultiSelect v-model="form.deviceTypes" :items="optionList('deviceTypes')" /></Field>
          <Field label="需求设备品类型号 *"><input class="form-control" v-model="form.deviceModels" required></Field>
          <Field label="需求数量（台）"><input type="number" class="form-control" v-model.number="form.demandQuantity" min="0"></Field>
          <Field label="预估采购金额 *"><input type="number" class="form-control" v-model.number="form.estimatedPurchaseAmount" min="0" step="0.01" required></Field>
          <Field label="金额单位 *"><SelectInput v-model="form.estimatedPurchaseAmountUnit" :items="optionList('amountUnits')" required /></Field>
        </div>

        <div class="submit-section" v-if="activeStep === 2">
          <Field label="是否可以提前写参数"><BooleanSelect v-model="form.canPrepareParams" /></Field>
          <Field label="是否需要唯一授权"><BooleanSelect v-model="form.requiresExclusiveAuthorization" /></Field>
          <Field label="需授权品类" full><MultiSelect v-model="form.authorizedCategories" :items="optionList('authorizedCategories')" /></Field>
          <Field label="是否报备成功"><BooleanSelect v-model="form.reportedSuccessfully" /></Field>
        </div>

        <div class="submit-section" v-if="activeStep === 3">
          <Field label="赢率 *"><SelectInput v-model="form.winRateLabel" :items="optionList('winRates')" required /></Field>
          <Field label="预计交付时间"><input type="date" class="form-control" v-model="form.expectedDeliveryDate"></Field>
          <Field label="预计投标截止时间"><input type="date" class="form-control" v-model="form.bidDeadline"></Field>
          <Field label="是否中标"><BooleanSelect v-model="form.bidWon" /></Field>
          <Field label="采购形式"><SelectInput v-model="form.purchaseType" :items="optionList('purchaseTypes')" /></Field>
          <Field label="项目中标金额"><input type="number" class="form-control" v-model.number="form.winningAmount" min="0" step="0.01"></Field>
          <Field label="中标金额单位"><SelectInput v-model="form.winningAmountUnit" :items="optionList('amountUnits')" /></Field>
          <Field label="得力设备BG交付金额"><input type="number" class="form-control" v-model.number="form.deliBgDeliveryAmount" min="0" step="0.01"></Field>
          <Field label="交付金额单位"><SelectInput v-model="form.deliBgDeliveryAmountUnit" :items="optionList('amountUnits')" /></Field>
        </div>

        <div class="submit-section" v-if="activeStep === 4">
          <Field label="省总"><input class="form-control" v-model="form.provinceGeneralManager"></Field>
          <Field label="A4业务经理"><input class="form-control" v-model="form.a4BusinessManager"></Field>
          <Field label="A3业务经理"><input class="form-control" v-model="form.a3BusinessManager"></Field>
          <Field label="业务进度"><SelectInput v-model="form.businessProgressStatus" :items="optionList('businessProgressStatuses')" /></Field>
        </div>

        <div class="submit-section" v-if="activeStep === 5">
          <Field label="本周项目进展/备注" full><textarea class="form-control form-textarea" v-model="form.weeklyProgress"></textarea></Field>
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

        <div class="submit-actions">
          <button type="button" class="btn-secondary" @click="prevStep" :disabled="activeStep === 0">上一步</button>
          <button type="button" class="btn-secondary" @click="nextStep" v-if="activeStep < formSteps.length - 1">下一步</button>
          <button type="submit" class="btn-primary" v-else :disabled="submitting">
            {{ submitting ? '提交中...' : '提交商机' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script>
import { defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from '../store'

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
  props: { modelValue: String, items: Array },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const toggle = (item) => {
      const values = (props.modelValue || '').split(',').map(v => v.trim()).filter(Boolean)
      const next = values.includes(item) ? values.filter(v => v !== item) : [...values, item]
      emit('update:modelValue', next.join(', '))
    }
    return () => h('div', { class: 'multi-select-box' }, (props.items || []).map(item => {
      const values = (props.modelValue || '').split(',').map(v => v.trim()).filter(Boolean)
      return h('button', {
        type: 'button',
        class: ['multi-option', values.includes(item) ? 'active' : ''],
        onClick: () => toggle(item)
      }, item)
    }))
  }
})

export default {
  components: { Field, SelectInput, BooleanSelect, MultiSelect },
  setup() {
    const router = useRouter()
    const store = useStore()
    const activeStep = ref(0)
    const submitting = ref(false)
    const duplicateMatches = ref([])
    const attachmentDraft = reactive({ fileName: '', fileType: 'other' })
    const formSteps = ['基础信息', '设备需求', '授权报备', '投标交付', '组织归属', '进展附件']

    const optionList = (key) => store.opportunityOptions.value?.[key] || []
    const userName = store.user.value?.name || '张经理'
    const form = reactive({
      name: '',
      company: '',
      stage: 'prospecting',
      priority: 'medium',
      owner: userName,
      submitter: userName,
      creator: userName,
      industry: '',
      supplierCompany: '',
      govMarketManager: '',
      submitterRegion: '',
      supplyRegion: '',
      salesDepartment: '设备省公司',
      sales: userName,
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
      attachments: []
    })

    const nextStep = async () => {
      if (activeStep.value <= 1 && form.company && form.name) {
        const result = await store.checkDuplicates(form)
        duplicateMatches.value = result.matches || []
      }
      if (activeStep.value < formSteps.length - 1) activeStep.value += 1
    }

    const prevStep = () => {
      if (activeStep.value > 0) activeStep.value -= 1
    }

    const addLocalAttachment = () => {
      if (!attachmentDraft.fileName.trim()) return
      form.attachments.push({
        fileName: attachmentDraft.fileName.trim(),
        fileType: attachmentDraft.fileType,
        fileUrl: `/mock-files/${attachmentDraft.fileName.trim()}`
      })
      attachmentDraft.fileName = ''
      attachmentDraft.fileType = 'other'
    }

    const handleSubmit = async () => {
      submitting.value = true
      try {
        await store.submitOpportunity({ ...form, attachments: [...form.attachments] })
        router.push({ name: 'List' })
      } finally {
        submitting.value = false
      }
    }

    onMounted(async () => {
      await store.fetchOpportunityOptions()
    })

    return {
      form,
      formSteps,
      activeStep,
      submitting,
      duplicateMatches,
      attachmentDraft,
      optionList,
      nextStep,
      prevStep,
      addLocalAttachment,
      handleSubmit
    }
  }
}
</script>
