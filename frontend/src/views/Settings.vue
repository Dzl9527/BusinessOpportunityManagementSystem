<template>
  <section class="content-panel active">
    <!-- Section 1: WeCom Integration -->
    <div class="settings-section">
      <h3 class="settings-section-title" style="display: flex; align-items: center; gap: 8px;">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-message-circle"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>
        <span>企业微信 (WeCom) API 集成设置</span>
      </h3>
      <p class="settings-section-desc">配置企业微信开发凭证，支持扫码免密登录、商机指派自动下发企微工作通知消息。</p>
      
      <div class="form-grid" style="margin-bottom: 24px; max-width: 800px;">
        <div class="form-group">
          <label class="form-label">企业 ID (CorpID)</label>
          <input type="text" class="form-control" v-model="wecomConfig.corpId">
        </div>
        <div class="form-group">
          <label class="form-label">自建应用 AgentID</label>
          <input type="text" class="form-control" v-model="wecomConfig.agentId">
        </div>
        <div class="form-group form-group-full">
          <label class="form-label">应用凭证 Secret</label>
          <input type="password" class="form-control" v-model="wecomConfig.secret">
        </div>
      </div>
      
      <div class="settings-btn-group">
        <button class="btn-primary" @click="saveWecomSettings">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-save"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          <span>保存企微配置</span>
        </button>
        <button class="btn-secondary" @click="syncWecomContacts" :disabled="isSyncing">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-refresh-cw" :class="{ 'spin': isSyncing }"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><polyline points="3 3 3 8 8 8"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><polyline points="16 16 21 16 21 21"/></svg>
          <span>同步企微通讯录</span>
        </button>
      </div>
    </div>

    <!-- Section 2: WeChat Message Push Test -->
    <div class="settings-section">
      <h3 class="settings-section-title">消息推送功能联调</h3>
      <p class="settings-section-desc">测试后端主动向指定的企微账号推送通知消息。默认向您当前登录的测试账号推送。</p>
      
      <div style="display: flex; gap: 12px; max-width: 600px; margin-bottom: 16px;">
        <input type="text" class="form-control" v-model="testMsg" placeholder="输入推送测试文本内容，如：新客户来访提醒...">
        <button class="btn-primary" @click="sendTestPush" :disabled="isPushing" style="flex-shrink: 0;">
          <span>测试发送</span>
        </button>
      </div>
    </div>

    <!-- Section 3: Data Backup -->
    <div class="settings-section">
      <h3 class="settings-section-title">系统数据备份与还原</h3>
      <p class="settings-section-desc">导出系统商机数据库为 JSON 备份文件，或者导入已备份的 JSON 数据覆盖当前库。</p>
      <div class="settings-btn-group">
        <button class="btn-primary" @click="handleExport">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-download"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
          <span>导出商机数据</span>
        </button>
        
        <div class="file-upload-wrapper">
          <button class="btn-secondary">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-upload"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
            <span>导入备份还原</span>
          </button>
          <input type="file" class="file-upload-input" @change="handleImport" accept=".json">
        </div>
      </div>
    </div>

    <!-- Section 4: Factory Reset -->
    <div class="settings-section" style="border-color: var(--danger-light);">
      <h3 class="settings-section-title" style="color: var(--danger);">系统危险区</h3>
      <p class="settings-section-desc">清空所有数据库中的更改，并将系统数据重新加载为出厂演示数据状态。</p>
      <div class="settings-btn-group">
        <button class="btn-primary" @click="handleResetDb" style="background-color: var(--danger); box-shadow: 0 4px 12px rgba(239, 68, 68, 0.2);">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-trash-2"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/></svg>
          <span>初始化恢复演示数据</span>
        </button>
      </div>
    </div>
  </section>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { API_BASE, useStore, showToast } from '../store'

export default {
  setup() {
    const store = useStore()
    const isSyncing = ref(false)
    const isPushing = ref(false)
    const testMsg = ref('提示：有一笔价值 280,000 元的“智能客服系统采购项目”商机状态已更新！')

    const wecomConfig = reactive({
      corpId: localStorage.getItem('wecom_corp_id') || 'wwdemo1234567890',
      agentId: localStorage.getItem('wecom_agent_id') || '1000002',
      secret: localStorage.getItem('wecom_secret') || '••••••••••••••••••••••••••••••••'
    })

    const saveWecomSettings = () => {
      localStorage.setItem('wecom_corp_id', wecomConfig.corpId)
      localStorage.setItem('wecom_agent_id', wecomConfig.agentId)
      localStorage.setItem('wecom_secret', wecomConfig.secret)
      showToast('企业微信配置保存成功！(生产部署需要在后端 application.yml 重新载入生效)', 'success')
    }

    const syncWecomContacts = async () => {
      isSyncing.value = true
      try {
        await store.fetchContacts()
        showToast('企微通讯录同步成功！已成功加载 3 名销售人员。', 'success')
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
      // Direct redirection to download file from backend
      const params = new URLSearchParams({
        userId: store.user.value?.userId || '',
        userName: store.user.value?.name || ''
      })
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
            params: { userId: store.user.value?.userId, userName: store.user.value?.name }
          })
          if (response.data.success) {
            showToast(response.data.message, 'success')
            await store.fetchOpportunities()
            await store.fetchMetrics()
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
      if (confirm('确定要清除所有本地录入的商机，并将系统初始化为预设的演示数据吗？\n\n此操作会清空当前数据库中所有的记录。')) {
        try {
          // Default mock data to restore
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
            params: { userId: store.user.value?.userId, userName: store.user.value?.name }
          })
          if (response.data.success) {
            showToast('数据库初始化重置成功！', 'success')
            await store.fetchOpportunities()
            await store.fetchMetrics()
          }
        } catch (e) {
          showToast('恢复演示数据异常，请确保后端正常运行。', 'error')
        }
      }
    }

    onMounted(() => {
      store.fetchContacts()
    })

    return {
      wecomConfig,
      testMsg,
      isSyncing,
      isPushing,
      saveWecomSettings,
      syncWecomContacts,
      sendTestPush,
      handleExport,
      handleImport,
      handleResetDb
    }
  }
}
</script>

<style scoped>
@keyframes spin {
  to { transform: rotate(360deg); }
}
.spin {
  animation: spin 1s linear infinite;
}
</style>
