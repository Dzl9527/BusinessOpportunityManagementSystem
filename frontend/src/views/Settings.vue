<template>
  <section class="content-panel active admin-tools-page settings-page">
    <nav class="settings-tabs" style="margin-top: 16px;">
      <button 
        type="button" 
        class="tab-btn" 
        :class="{ active: activeTab === 'wecom' }" 
        @click="activeTab = 'wecom'"
      >
        基础对接
      </button>
      <button 
        type="button" 
        class="tab-btn" 
        :class="{ active: activeTab === 'reminder' }" 
        @click="activeTab = 'reminder'"
      >
        智能提醒配置
      </button>
      <button 
        type="button" 
        class="tab-btn" 
        :class="{ active: activeTab === 'auth' }" 
        @click="activeTab = 'auth'"
      >
        组织权限管理
      </button>
      <button 
        type="button" 
        class="tab-btn" 
        :class="{ active: activeTab === 'data' }" 
        @click="activeTab = 'data'"
      >
        数据与联调
      </button>
    </nav>

    <div class="settings-tab-content">
      <!-- TAB: WeCom -->
      <div v-if="activeTab === 'wecom'" class="settings-single-grid">
        <section class="admin-mobile-panel">
          <div class="panel-heading">
            <h3>企业微信配置</h3>
            <p>维护本地调试用的企微参数，并执行通讯录同步。</p>
          </div>

          <div class="admin-form-stack">
            <div class="admin-setting-card">
              <span>企业 ID (CorpID)</span>
              <input type="text" class="form-control" v-model="wecomConfig.corpId">
            </div>
            <div class="admin-setting-card">
              <span>自建应用 AgentID</span>
              <input type="text" class="form-control" v-model="wecomConfig.agentId">
            </div>
            <div class="admin-setting-card">
              <span>应用凭证 Secret</span>
              <input type="password" class="form-control" v-model="wecomConfig.secret">
            </div>
            <div class="admin-setting-card">
              <span>最近通讯录同步</span>
              <p class="settings-card-text">{{ syncSummary }}</p>
            </div>
          </div>

          <div class="stacked-actions">
            <button class="btn-primary full-width" @click="saveWecomSettings">保存企微配置</button>
            <button class="btn-secondary full-width" @click="syncWecomContacts" :disabled="isSyncing">
              {{ isSyncing ? '同步中...' : '同步企微通讯录' }}
            </button>
          </div>
        </section>
      </div>

      <!-- TAB: Reminder -->
      <div v-if="activeTab === 'reminder'" class="settings-single-grid">
        <section class="admin-mobile-panel">
          <div class="panel-heading">
            <h3>商机提醒配置</h3>
            <p>配置自动提醒场景触发规则，包括投标截止、预计交付提前天数与开关状态。</p>
          </div>

          <div class="admin-form-stack">
            <div class="admin-setting-card">
              <span>投标截止提前提醒天数</span>
              <input type="number" class="form-control" v-model="reminderConfig['reminder.bid_deadline_days']" min="1" max="30">
            </div>
            <div class="admin-setting-card">
              <span>预计交付提前提醒天数</span>
              <input type="number" class="form-control" v-model="reminderConfig['reminder.expected_delivery_days']" min="1" max="30">
            </div>
            <div class="admin-setting-card">
              <span>周跟进未更新周期（天数）</span>
              <input type="number" class="form-control" v-model="reminderConfig['reminder.weekly_update_days']" min="1" max="30">
            </div>

            <div class="admin-setting-card">
              <span>启用提醒规则</span>
              <div class="settings-checkbox-list">
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="reminderSwitches.bid_deadline">
                  <div class="settings-checkbox-copy">
                    <strong>投标截止提醒</strong>
                    <p>在投标截止日期前 N 天提醒负责人跟进。</p>
                  </div>
                </label>
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="reminderSwitches.expected_delivery">
                  <div class="settings-checkbox-copy">
                    <strong>预计交付提醒</strong>
                    <p>在预计交付日期前 N 天提醒负责人跟进。</p>
                  </div>
                </label>
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="reminderSwitches.weekly_update">
                  <div class="settings-checkbox-copy">
                    <strong>周跟进更新提醒</strong>
                    <p>超期未更新进展时发送周更新提醒。</p>
                  </div>
                </label>
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="reminderSwitches.auth_followup">
                  <div class="settings-checkbox-copy">
                    <strong>唯一授权跟进提醒</strong>
                    <p>需唯一授权但无品类或无授权附件时发送提醒。</p>
                  </div>
                </label>
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="reminderSwitches.report_followup">
                  <div class="settings-checkbox-copy">
                    <strong>商机报备催办提醒</strong>
                    <p>投标临近但未成功报备时发送提醒。</p>
                  </div>
                </label>
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="reminderSwitches.bid_result">
                  <div class="settings-checkbox-copy">
                    <strong>中标结果补录提醒</strong>
                    <p>投标已截止但未录入中标状态时发送提醒。</p>
                  </div>
                </label>
              </div>
            </div>
          </div>

          <div class="stacked-actions">
            <button class="btn-primary full-width" @click="saveReminderSettings" :disabled="isSavingConfig">
              {{ isSavingConfig ? '保存中...' : '保存提醒配置' }}
            </button>
            <button class="btn-secondary full-width" @click="triggerReminderScan" :disabled="isScanning">
              {{ isScanning ? '扫描提醒中...' : '立即执行提醒扫描' }}
            </button>
          </div>
        </section>
      </div>

      <!-- TAB: Auth -->
      <div v-if="activeTab === 'auth'" class="settings-single-grid">
        <section class="admin-mobile-panel">
          <div class="panel-heading">
            <h3>组织与权限</h3>
            <p>处理角色、启用状态和白名单范围，保持查看边界清晰。</p>
          </div>

          <div class="settings-overview-grid cols-3">
            <article class="settings-overview-card">
              <span>启用账号</span>
              <strong>{{ enabledUsersCount }}</strong>
              <p>当前允许登录系统的企业微信账号数量</p>
            </article>
            <article class="settings-overview-card">
              <span>管理员账号</span>
              <strong>{{ adminUsersCount }}</strong>
              <p>具备系统设置与用户管理权限的账号</p>
            </article>
            <article class="settings-overview-card">
              <span>通讯录人数</span>
              <strong>{{ contactCount }}</strong>
              <p>最近一次同步到前端的企业微信联系人</p>
            </article>
          </div>

          <div class="selected-user-summary">
            <div>
              <strong>{{ currentAdminName }}</strong>
              <span>{{ currentAdminId }}</span>
            </div>
            <span class="role-chip">系统管理员</span>
          </div>

          <div class="tool-card-list">
            <button class="tool-card-button" type="button" @click="goUsers">
              <strong>进入用户管理</strong>
              <p>维护普通用户、领导、管理员以及白名单可见范围。</p>
            </button>

            <div class="admin-setting-card">
              <span>当前权限策略</span>
              <p class="settings-card-text">
                普通用户默认查看本人商机；领导查看本人及白名单范围商机；管理员可查看和维护全部商机。
              </p>
            </div>
          </div>
        </section>
      </div>

      <!-- TAB: Data & Testing -->
      <div v-if="activeTab === 'data'" class="admin-tools-grid settings-tools-grid">
        <section class="admin-mobile-panel">
          <div class="panel-heading">
            <h3>通知与联调</h3>
            <p>测试当前账号的企微消息推送能力，并保存当前浏览器的管理偏好。</p>
          </div>

          <div class="admin-form-stack">
            <div class="admin-setting-card">
              <span>测试内容</span>
              <textarea class="form-control form-textarea admin-textarea" v-model="testMsg" placeholder="输入推送测试文本"></textarea>
            </div>

            <div class="admin-setting-card">
              <span>当前端偏好（仅当前浏览器）</span>
              <div class="settings-checkbox-list">
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="uiPrefs.showSubmitToast">
                  <div class="settings-checkbox-copy">
                    <strong>保留提报成功提示</strong>
                    <p>提交商机后继续显示本地成功提示和状态回馈。</p>
                  </div>
                </label>
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="uiPrefs.highlightRiskActions">
                  <div class="settings-checkbox-copy">
                    <strong>强调高风险操作</strong>
                    <p>在初始化、覆盖导入等操作前保留明显提醒。</p>
                  </div>
                </label>
                <label class="settings-checkbox-row">
                  <input type="checkbox" v-model="uiPrefs.keepFilterCollapsed">
                  <div class="settings-checkbox-copy">
                    <strong>默认使用折叠筛选</strong>
                    <p>列表页保持轻量化入口，减少大筛选区占用空间。</p>
                  </div>
                </label>
              </div>
            </div>
          </div>

          <div class="stacked-actions">
            <button class="btn-primary full-width" @click="sendTestPush" :disabled="isPushing">
              {{ isPushing ? '发送中...' : '测试发送企微消息' }}
            </button>
            <button class="btn-secondary full-width" @click="saveUiPrefs">
              保存当前端偏好
            </button>
          </div>
        </section>

        <section class="admin-mobile-panel">
          <div class="panel-heading">
            <h3>数据管理</h3>
            <p>导出商机数据，或导入已有备份进行恢复。</p>
          </div>

          <div class="tool-card-list">
            <button class="tool-card-button" type="button" @click="handleExport">
              <strong>导出商机数据</strong>
              <p>下载当前系统数据的 JSON 备份文件。</p>
            </button>

            <label class="tool-card-button file-card">
              <strong>导入备份还原</strong>
              <p>选择 JSON 备份文件并覆盖当前库。</p>
              <input type="file" class="file-upload-input" @change="handleImport" accept=".json">
            </label>

            <div class="admin-setting-card">
              <span>最近数据操作</span>
              <p class="settings-card-text">{{ lastDataAction }}</p>
            </div>
          </div>
        </section>

        <section class="admin-mobile-panel danger-panel">
          <div class="panel-heading">
            <h3>演示环境</h3>
            <p>仅在演示或重置环境时使用，操作前请再次确认。</p>
          </div>

          <div class="admin-form-stack">
            <div class="admin-setting-card">
              <span>环境说明</span>
              <p class="settings-card-text">
                当前版本只调整前后端 UI 表现，不新增后端逻辑。初始化会覆盖当前数据库中的商机记录并恢复演示样例。
              </p>
            </div>
          </div>

          <div class="stacked-actions">
            <button class="btn-primary full-width danger-button" @click="handleResetDb">
              初始化恢复演示数据
            </button>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<script>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useAuthStore } from '../stores/useAuthStore'
import { useOppStore } from '../stores/useOppStore'
import { useAppStore } from '../stores/useAppStore'
import { useUserStore } from '../stores/useUserStore'
import { useMetricsStore } from '../stores/useMetricsStore'

const UI_PREFS_KEY = 'crm_admin_ui_prefs'
const SYNC_SUMMARY_KEY = 'crm_settings_last_sync'
const DATA_ACTION_KEY = 'crm_settings_last_data_action'

const defaultUiPrefs = () => ({
  showSubmitToast: true,
  highlightRiskActions: true,
  keepFilterCollapsed: true
})

const formatNow = () => new Date().toLocaleString('zh-CN', { hour12: false })

export default {
  setup() {
    const router = useRouter()
        const authStore = useAuthStore()
    const oppStore = useOppStore()
    const appStore = useAppStore()
    const userStore = useUserStore()
    const metricsStore = useMetricsStore()
const activeTab = ref('wecom')
    const isSyncing = ref(false)
    const isPushing = ref(false)
    const isSavingConfig = ref(false)
    const isScanning = ref(false)

    const reminderConfig = reactive({
      'reminder.bid_deadline_days': '3',
      'reminder.expected_delivery_days': '3',
      'reminder.weekly_update_days': '7'
    })

    const reminderSwitches = reactive({
      bid_deadline: true,
      expected_delivery: true,
      weekly_update: true,
      auth_followup: true,
      report_followup: true,
      bid_result: true
    })

    const fetchReminderConfig = async () => {
      try {
        const response = await axios.get(`${API_BASE}/opportunities/reminders/config`, {
          params: { userId: authStore.user?.userId, userName: authStore.user?.name }
        })
        const data = response.data
        if (data) {
          reminderConfig['reminder.bid_deadline_days'] = data['reminder.bid_deadline_days'] || '3'
          reminderConfig['reminder.expected_delivery_days'] = data['reminder.expected_delivery_days'] || '3'
          reminderConfig['reminder.weekly_update_days'] = data['reminder.weekly_update_days'] || '7'
          reminderSwitches.bid_deadline = data['reminder.enable.bid_deadline'] !== 'false'
          reminderSwitches.expected_delivery = data['reminder.enable.expected_delivery'] !== 'false'
          reminderSwitches.weekly_update = data['reminder.enable.weekly_update'] !== 'false'
          reminderSwitches.auth_followup = data['reminder.enable.auth_followup'] !== 'false'
          reminderSwitches.report_followup = data['reminder.enable.report_followup'] !== 'false'
          reminderSwitches.bid_result = data['reminder.enable.bid_result'] !== 'false'
        }
      } catch (e) {
        console.error('获取提醒配置失败', e)
      }
    }

    const saveReminderSettings = async () => {
      isSavingConfig.value = true
      try {
        const payload = {
          'reminder.bid_deadline_days': String(reminderConfig['reminder.bid_deadline_days']),
          'reminder.expected_delivery_days': String(reminderConfig['reminder.expected_delivery_days']),
          'reminder.weekly_update_days': String(reminderConfig['reminder.weekly_update_days']),
          'reminder.enable.bid_deadline': String(reminderSwitches.bid_deadline),
          'reminder.enable.expected_delivery': String(reminderSwitches.expected_delivery),
          'reminder.enable.weekly_update': String(reminderSwitches.weekly_update),
          'reminder.enable.auth_followup': String(reminderSwitches.auth_followup),
          'reminder.enable.report_followup': String(reminderSwitches.report_followup),
          'reminder.enable.bid_result': String(reminderSwitches.bid_result)
        }
        const response = await axios.post(`${API_BASE}/opportunities/reminders/config`, payload, {
          params: { userId: authStore.user?.userId, userName: authStore.user?.name }
        })
        if (response.data.success) {
          showToast(response.data.message, 'success')
        } else {
          showToast('保存提醒配置失败', 'error')
        }
      } catch (e) {
        showToast('保存提醒配置异常', 'error')
      } finally {
        isSavingConfig.value = false
      }
    }

    const triggerReminderScan = async () => {
      isScanning.value = true
      try {
        const response = await axios.post(`${API_BASE}/opportunities/reminders/trigger-scan`, {}, {
          params: { userId: authStore.user?.userId, userName: authStore.user?.name }
        })
        if (response.data.success) {
          showToast(response.data.message, 'success')
        } else {
          showToast('触发提醒扫描失败', 'error')
        }
      } catch (e) {
        showToast('执行扫描异常，请检查后端运行状态。', 'error')
      } finally {
        isScanning.value = false
      }
    }
    const syncSummary = ref(localStorage.getItem(SYNC_SUMMARY_KEY) || '暂无同步记录')
    const lastDataAction = ref(localStorage.getItem(DATA_ACTION_KEY) || '暂无数据操作记录')
    const testMsg = ref('提示：有一笔价值 280,000 元的“智能客服系统采购项目”商机状态已更新！')

    const savedPrefs = (() => {
      try {
        return JSON.parse(localStorage.getItem(UI_PREFS_KEY) || '{}')
      } catch (e) {
        return {}
      }
    })()

    const uiPrefs = reactive({
      ...defaultUiPrefs(),
      ...savedPrefs
    })

    const wecomConfig = reactive({
      corpId: localStorage.getItem('wecom_corp_id') || 'wwdemo1234567890',
      agentId: localStorage.getItem('wecom_agent_id') || '1000002',
      secret: localStorage.getItem('wecom_secret') || '••••••••••••••••••••••••••••••••'
    })

    const users = computed(() => userStore.users || [])
    const enabledUsersCount = computed(() => users.value.filter(item => item.enabled).length)
    const adminUsersCount = computed(() => users.value.filter(item => item.role === 'ADMIN').length)
    const contactCount = computed(() => userStore.contacts?.length || 0)
    const activeOpportunityCount = computed(() => metricsStore.metrics?.activeCount || 0)
    const currentAdminName = computed(() => authStore.user?.name || '管理员')
    const currentAdminId = computed(() => authStore.user?.userId || '-')

    const updateSyncSummary = (text) => {
      syncSummary.value = `${formatNow()} ${text}`
      localStorage.setItem(SYNC_SUMMARY_KEY, syncSummary.value)
    }

    const updateDataAction = (text) => {
      lastDataAction.value = `${formatNow()} ${text}`
      localStorage.setItem(DATA_ACTION_KEY, lastDataAction.value)
    }

    const goUsers = () => {
      router.push({ name: 'Users' })
    }

    const saveUiPrefs = () => {
      localStorage.setItem(UI_PREFS_KEY, JSON.stringify({ ...uiPrefs }))
      showToast('当前端偏好已保存，仅对本浏览器生效', 'success')
    }

    const saveWecomSettings = () => {
      localStorage.setItem('wecom_corp_id', wecomConfig.corpId)
      localStorage.setItem('wecom_agent_id', wecomConfig.agentId)
      localStorage.setItem('wecom_secret', wecomConfig.secret)
      updateSyncSummary('已保存企业微信本地配置')
      showToast('企业微信配置保存成功！(生产部署需要在后端 application.yml 重新载入生效)', 'success')
    }

    const syncWecomContacts = async () => {
      isSyncing.value = true
      try {
        await userStore.fetchContacts()
        updateSyncSummary(`已同步 ${userStore.contacts?.length || 0} 位企业微信联系人`)
        showToast(`企微通讯录同步成功！已成功加载 ${userStore.contacts?.length || 0} 名销售人员。`, 'success')
      } catch (err) {
        showToast('同步通讯录失败', 'error')
      } finally {
        isSyncing.value = false
      }
    }

    const sendTestPush = async () => {
      if (!testMsg.value.trim()) return
      isPushing.value = true
      try {
        const response = await axios.post(`${API_BASE}/wecom/push-test`, {
          content: testMsg.value.trim()
        })
        if (response.data.success) {
          updateSyncSummary('已执行企业微信消息联调')
          showToast(response.data.message, 'success')
        } else {
          showToast(response.data.message, 'error')
        }
      } catch (e) {
        showToast('发送推送请求异常，请确认后端已正常启动。', 'error')
      } finally {
        isPushing.value = false
      }
    }

    const handleExport = () => {
      const params = new URLSearchParams({
        userId: authStore.user?.userId || '',
        userName: authStore.user?.name || ''
      })
      updateDataAction('已发起数据导出请求')
      window.location.href = `${API_BASE}/wecom/export?${params.toString()}`
      showToast('数据下载请求已发送', 'success')
    }

    const handleImport = async (e) => {
      const file = e.target.files[0]
      if (!file) return

      const reader = new FileReader()
      reader.onload = async (evt) => {
        try {
          const json = JSON.parse(evt.target.result)
          const response = await axios.post(`${API_BASE}/wecom/import`, json, {
            params: { userId: authStore.user?.userId, userName: authStore.user?.name }
          })
          if (response.data.success) {
            updateDataAction(`已导入备份文件 ${file.name}`)
            showToast(response.data.message, 'success')
            await oppStore.fetchOpportunities()
            await metricsStore.fetchMetrics()
          } else {
            showToast(response.data.message, 'error')
          }
        } catch (err) {
          showToast('JSON 备份解析失败，请上传正确的 BOMS 数据包', 'error')
        } finally {
          e.target.value = ''
        }
      }
      reader.readAsText(file)
    }

    const handleResetDb = async () => {
      const riskNotice = uiPrefs.highlightRiskActions
        ? '确定要清除所有本地录入的商机，并将系统初始化为预设的演示数据吗？\n\n此操作会清空当前数据库中所有的记录。'
        : '确定要初始化演示数据吗？'

      if (confirm(riskNotice)) {
        try {
          const defaultMocks = [
            {
              name: "智能客服系统采购项目",
              company: "极客科技股份有限公司",
              stage: "proposal",
              value: 280000,
              probability: 60,
              closeDate: "2026-06-15",
              contactName: "陈总",
              contactPhone: "13800138000",
              contactEmail: "chen.y@geektech.com",
              source: "线上注册",
              priority: "high",
              owner: "张经理",
              description: "客户正在寻找一套能够对接微信公众号、小程序和官网的智能客服系统，需要支持自然语言处理（NLP）和多轮对话。目前已完成需求对接，正在起草定制化方案和报价。",
              tasks: [
                { text: "定制化方案方案书编写", done: true },
                { text: "系统演示与技术答辩", done: true },
                { text: "提交正式报价单", done: false },
                { text: "进行第一轮商务合同条款确认", done: false }
              ],
              activities: [
                { type: "system", content: "创建商机：智能客服系统采购项目，分配给 张经理", date: "2026-05-10 10:00" },
                { type: "meeting", content: "与陈总及技术总监进行线上会议，调研核心客服指标和对接系统", date: "2026-05-12 14:30" },
                { type: "call", content: "致电陈总确认接口细节，客户表示对我们的知识库管理功能非常感兴趣", date: "2026-05-15 11:15" },
                { type: "system", content: "商机阶段由 [资质评估] 变更为 [方案报价]", date: "2026-05-18 16:45" }
              ]
            },
            {
              name: "云原生容器化改造服务",
              company: "瑞丰物流集团",
              stage: "negotiation",
              value: 650000,
              probability: 80,
              closeDate: "2026-06-05",
              contactName: "李处长",
              contactPhone: "13911022938",
              contactEmail: "li.xiang@ruifeng.cn",
              source: "转介绍",
              priority: "high",
              owner: "李主管",
              description: "物流调度系统进行微服务及容器化改造，需提供Kubernetes集群规划、CI/CD流水线建设及为期半年的运维保障。目前报价已被接受，正处于合同条款博弈阶段，重点在赔偿上限及响应时间SLA上。",
              tasks: [
                { text: "架构设计初稿设计", done: true },
                { text: "商务报价审批", done: true },
                { text: "法务审核合同模板", done: true },
                { text: "确定SLA保障细则", done: false }
              ],
              activities: [
                { type: "system", content: "创建商机，由渠道商推荐，分配给 李主管", date: "2026-04-20 09:12" },
                { type: "meeting", content: "前往瑞丰物流总部与运维负责人交流，评估迁移风险", date: "2026-04-25 15:00" },
                { type: "email", content: "发送更新版云架构方案及POC测试报告", date: "2026-05-02 18:22" },
                { type: "system", content: "商机阶段变更为 [谈判协商]，预计赢单率提升至 80%", date: "2026-05-20 14:00" }
              ]
            },
            {
              name: "企业级数据湖平台建设",
              company: "华夏零售连锁",
              stage: "qualification",
              value: 1200000,
              probability: 40,
              closeDate: "2026-08-30",
              contactName: "王总",
              contactPhone: "13588992233",
              contactEmail: "wang.jian@huaxaretail.com",
              source: "展会活动",
              priority: "medium",
              owner: "张经理",
              description: "客户希望打通线下POS、线上电商及小程序的用户行为数据，构建统一的数据湖。由于项目预算庞大，目前正处于评估我司方案与竞争对手阶段，需要协调资深数据架构师做联合售前输出。",
              tasks: [
                { text: "了解客户历史数据架构", done: true },
                { text: "准备行业零售数据湖案例PPT", done: false },
                { text: "协调内部架构师资源", done: false }
              ],
              activities: [
                { type: "system", content: "创建商机：在行业零售峰会中获得联系方式，分配给 张经理", date: "2026-05-18 11:30" },
                { type: "call", content: "与王总电话沟通，初步摸清了对方在多渠道数据融合方面的痛点", date: "2026-05-22 10:15" }
              ]
            },
            {
              name: "私有云存储扩容项目",
              company: "天图设计院",
              stage: "prospecting",
              value: 150000,
              probability: 20,
              closeDate: "2026-07-20",
              contactName: "赵工",
              contactPhone: "15677889900",
              contactEmail: "zhao.y@tiantu-design.com",
              source: "冷拓拜访",
              priority: "low",
              owner: "王销售",
              description: "设计院日常三维渲染图纸增多，NAS存储告急，打算扩容私有存储。目前处于初步接触阶段，需要带硬件产品经理上门做硬件测试及空间评估。",
              tasks: [
                { text: "致电客户约见上门时间", done: false },
                { text: "准备存储服务器硬件彩页", done: false }
              ],
              activities: [
                { type: "system", content: "王销售录入商机，归属于发现阶段", date: "2026-05-23 15:40" }
              ]
            }
          ]

          const response = await axios.post(`${API_BASE}/wecom/import`, defaultMocks, {
            params: { userId: authStore.user?.userId, userName: authStore.user?.name }
          })
          if (response.data.success) {
            updateDataAction('已恢复演示数据')
            showToast('数据库初始化重置成功！', 'success')
            await oppStore.fetchOpportunities()
            await metricsStore.fetchMetrics()
          }
        } catch (e) {
          showToast('恢复演示数据异常，请确保后端正常运行。', 'error')
        }
      }
    }

    onMounted(async () => {
      await Promise.all([
        userStore.fetchUsers(),
        userStore.fetchContacts(),
        metricsStore.fetchMetrics(),
        fetchReminderConfig()
      ])
    })

    return {
      wecomConfig,
      uiPrefs,
      testMsg,
      isSyncing,
      isPushing,
      syncSummary,
      lastDataAction,
      enabledUsersCount,
      adminUsersCount,
      contactCount,
      activeOpportunityCount,
      currentAdminName,
      currentAdminId,
      goUsers,
      saveUiPrefs,
      saveWecomSettings,
      syncWecomContacts,
      sendTestPush,
      handleExport,
      handleImport,
      handleResetDb,
      reminderConfig,
      reminderSwitches,
      isSavingConfig,
      isScanning,
      saveReminderSettings,
      triggerReminderScan,
      activeTab
    }
  }
}
</script>

<style scoped>
.settings-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 8px;
  overflow-x: auto;
}

.tab-btn {
  background: none;
  border: none;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.tab-btn:hover {
  color: #3b82f6;
  background-color: #f1f5f9;
}

.tab-btn.active {
  color: #3b82f6;
  background-color: #eff6ff;
  box-shadow: inset 0 -2px 0 #3b82f6;
}

.settings-tab-content {
  animation: fadeIn 0.25s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.settings-single-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.settings-overview-grid.cols-3 {
  margin-bottom: 20px;
}

@media (min-width: 768px) {
  .settings-single-grid {
    grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  }
  .settings-overview-grid.cols-3 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
