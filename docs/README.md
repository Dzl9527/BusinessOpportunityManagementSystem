# Business Opportunity Management System Docs

本目录作为项目本地需求知识库使用，替代 Notion。

## 目录结构设计 (自 2026-06-14 起)

经过重构，当前文档全面采取**基于功能模块（Feature Modules）**与**时间线日志**分离的归档模式。

每次有新需求时，先检索本目录下相关的模块文档，再分析新需求是否与既有需求、数据库设计、接口设计、前端交互冲突。

### 1. 全局与架构核心 (`docs/core/`)
存放产品整体规划、业务说明等非具体模块化的全局资产。
- [商机管理系统-产品总览.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/core/商机管理系统-产品总览.md)
- [版本规划.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/core/版本规划.md)
- [BOMS业务需求与核心流程说明书.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/core/BOMS业务需求与核心流程说明书.md)

### 2. 功能模块 (`docs/modules/`)
将所有具体功能相关的**需求文档**与**技术设计**归拢在同属模块内：

**商机管理 (`opportunity-management`)**
- [2026-05-24-设备事业群商机字段改造需求.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/modules/opportunity-management/2026-05-24-设备事业群商机字段改造需求.md)
- [2026-05-24-用户端商机提报数据结构设计.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/modules/opportunity-management/2026-05-24-用户端商机提报数据结构设计.md)
- [2026-05-24-设备事业群商机字段技术改造方案.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/modules/opportunity-management/2026-05-24-设备事业群商机字段技术改造方案.md)

**OA集成 (`oa-integration`)**
- [2026-05-25-OA商机报备与项目授权申请同步需求.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/modules/oa-integration/2026-05-25-OA商机报备与项目授权申请同步需求.md)
- [2026-05-26-OA商机报备与项目授权申请流程图.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/modules/oa-integration/2026-05-26-OA商机报备与项目授权申请流程图.md)

**移动端 (`mobile-client`)**
- [2026-05-26-手机端信息架构与核心页面需求.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/modules/mobile-client/2026-05-26-手机端信息架构与核心页面需求.md)

**权限与认证 (`auth-and-permissions`)**
- [2026-05-24-企业微信权限与移动端商机工程实现方案.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/modules/auth-and-permissions/2026-05-24-企业微信权限与移动端商机工程实现方案.md)

### 3. 时间线日志 (`docs/changelog/` & `docs/decisions/`)
天然带有按发生时间追溯属性的历史记录，独立于模块管理：
- [需求变更记录.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/changelog/需求变更记录.md)
- [2026-05-24-本地文档库替代Notion.md](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/docs/decisions/2026-05-24-本地文档库替代Notion.md)

### 4. 数据库部署脚本 (`deploy/sql/`)
*注意：为保持文档库纯粹，涉及数据库结构的 `.sql` 脚本已被迁移至项目根目录的部署资源文件夹中。*
- [2026-05-24-opportunity-field-migration.sql](file:///C:/Users/Administrator/.gemini/antigravity/worktrees/BusinessOpportunityManagementSystem/verify-worktree-isolation-impact/deploy/sql/2026-05-24-opportunity-field-migration.sql)

### 5. 新增标准模版与规范 (归档至 `docs/templates/`)
- `docs/templates/PRD.md`
- `docs/templates/business-process.md`
- `docs/templates/field-dictionary.md`
- `docs/templates/permission-rules.md`
- `docs/templates/status-flow.md`
- `docs/templates/oa-integration.md`
- `docs/templates/api-design.md`
- `docs/templates/acceptance-criteria.md`

---

## 当前已确认关键口径

- 手机端包含商机提报和 `我的商机` 看板。
- 手机端一级导航进一步明确为：工作台、商机提报、商机列表、商机看板、用户中心。
- `工作台` 作为原“仪表盘”的业务首页命名，承担摘要、提醒和快捷入口。
- `用户中心` 承担登录状态、个人信息、权限说明和退出登录，并在开发环境支持免登沙箱身份切换。
- `系统设置` 保留为管理员专属入口，不占用普通用户主导航。
- 企业微信 `wecomUserId` 是权限判断唯一依据。
- 第一期手机端使用沙箱模拟登录，预留真实企业微信 SSO。
- 后续通过企业微信 API 自动同步用户、部门和标签。
- 新增用户板块，管理员维护角色、启用状态和用户白名单。
- 白名单按“领导/授权用户 -> 可查看人员列表”配置，第一期只做用户白名单。
- 白名单同时允许查看和编辑指定人员商机。
- 管理员可查看和编辑全部商机。
- 历史未归属数据仅管理员可见。
- 所有商机编辑必须生成字段级修订记录。
- 商机系统是设备需求主数据源，OA 商机报备归档只保存审批快照。
- OA 项目授权申请流程发起前，通过商机报备流程号回查商机系统最新数据。
- 项目授权申请流程发起后临时冻结需求设备类型字段，到授权书创建审批 06 节点后硬锁定。
