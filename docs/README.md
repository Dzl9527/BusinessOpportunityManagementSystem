# Business Opportunity Management System Docs

本目录作为项目本地需求知识库使用，替代 Notion。

## 使用约定

每次有新需求时，先检索本目录下历史文档，再分析新需求是否与既有需求、数据库设计、接口设计、前端交互冲突。

推荐检索范围：

- `docs/product/`：产品总览、版本规划和业务范围
- `docs/requirements/`：需求与 PRD
- `docs/design/`：系统设计、数据库设计、接口设计、SQL 草案
- `docs/decisions/`：关键产品/技术决策
- `docs/changelog/`：需求变更记录

涉及权限、企业微信、手机端看板、商机归属或修订记录的需求，必须同步检查并更新：

- 产品总览
- 相关需求文档
- 技术设计方案
- SQL 草案
- 需求变更记录

## 当前文档

- `docs/PRD.md`
- `docs/business-process.md`
- `docs/field-dictionary.md`
- `docs/permission-rules.md`
- `docs/status-flow.md`
- `docs/oa-integration.md`
- `docs/api-design.md`
- `docs/acceptance-criteria.md`
- `docs/product/商机管理系统-产品总览.md`
- `docs/product/版本规划.md`
- `docs/BOMS业务需求与核心流程说明书.md`
- `docs/requirements/2026-05-24-设备事业群商机字段改造需求.md`
- `docs/requirements/2026-05-24-用户端商机提报数据结构设计.md`
- `docs/requirements/2026-05-25-OA商机报备与项目授权申请同步需求.md`
- `docs/requirements/2026-05-26-手机端信息架构与核心页面需求.md`
- `docs/design/2026-05-24-设备事业群商机字段技术改造方案.md`
- `docs/design/2026-05-24-企业微信权限与移动端商机工程实现方案.md`
- `docs/design/2026-05-24-opportunity-field-migration.sql`
- `docs/design/2026-05-26-OA商机报备与项目授权申请流程图.md`
- `docs/design/2026-05-29-飞书日历查询脚本说明.md`
- `docs/decisions/2026-05-24-本地文档库替代Notion.md`
- `docs/changelog/需求变更记录.md`

## 当前已确认关键口径

- 手机端包含商机提报和 `我的商机` 看板。
- 手机端一级导航进一步明确为：工作台、商机提报、商机列表、商机看板、用户中心。
- `工作台` 作为原“仪表盘”的业务首页命名，承担摘要、提醒和快捷入口。
- `用户中心` 承担登录状态、个人信息、权限说明和退出登录。
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
