# Codex 开发规则

## 项目定位

- 项目名称：`business-opportunity-system`
- 项目类型：商机管理系统
- 主要目标：支撑商机提报、权限隔离、OA 同步、企业微信提醒和进度跟踪。

## 工作原则

- 修改前先检查 `docs/` 中的产品、流程、权限、接口和验收文档。
- 优先沿用当前 Spring Boot、Vue 和 Markdown 文档结构，不引入不必要的新框架。
- 业务代码、原型、数据库脚本、测试、工具和部署文件必须放入对应目录。
- 不提交本地日志、构建产物、依赖缓存、临时草稿和真实密钥。

## 代码规则

- 后端代码放在 `backend/`，按 controller、service、repository、model 分层。
- 前端代码放在 `frontend/`，页面、组件、路由和状态管理保持职责清晰。
- 数据库变更放在 `database/`，每次字段或表结构变化都要补迁移说明。
- 可复用脚本放在 `tools/`，一次性实验放在 `scratch/`。
- 多步骤表单（Stepper/Wizard）禁止使用 HTML5 原生 `required` 属性，必须使用纯 JS 逻辑进行分步校验，避免因隐藏或未渲染的 DOM 节点导致浏览器校验静默失败或控制台报错。
- 页面全局 Toast 提示采用统一的置顶居中布局（`top: 24px`），并使用 Vue `<transition>` 包裹提供平滑淡入淡出动效。
- 复杂配置大表单页面采用局部滚动结构（外层锁定 `overflow: hidden; height: 100%`，页签固定 `flex-shrink: 0`，内容区 `overflow-y: auto; flex: 1; min-height: 0` 独立滚动），且内容区底部留足 padding 缓冲，防止操作按钮被视口裁剪。

## 文档规则

- 新需求先更新或补充 `docs/PRD.md`。
- 涉及业务流程时同步更新 `docs/business-process.md` 和 `docs/status-flow.md`。
- 涉及字段时同步更新 `docs/field-dictionary.md`。
- 涉及权限时同步更新 `docs/permission-rules.md`。
- 涉及 OA 或外部系统时同步更新 `docs/oa-integration.md` 和 `docs/api-design.md`。
- 发布前同步更新 `CHANGELOG.md`。

## 验证规则

- 后端至少执行 `mvn test` 或等价编译验证。
- 前端至少执行 `npm run build`。
- 涉及权限、状态流转、OA 同步或提醒逻辑时，需要补充测试用例或手动验收记录。

## 生产环境部署排查（自动触发机制）

- **触发条件**：当用户的指令中包含“部署”、“上线”、“发布到服务器”或者询问“有没有隐患”、“检查”等意图时，必须自动触发本规则。
- **执行动作**：在给出部署建议或打包命令前，必须静默执行一次代码与生产环境的兼容性审查，重点排查：
  1. **数据库结构变更**：比对实体类（如 `@Column(columnDefinition="TEXT")`）或名称的改动，判断是否需要在 MySQL 生产库中执行手动的 `ALTER TABLE` 升级脚本（因为 Spring Boot 的 `ddl-auto=update` 机制只增不改）。
  2. **环境变量校验**：检查 `docker-compose.yml` 和 `application-prod.yml` 中的硬编码配置（尤其是回调 URL、IP、端口映射），并提示用户在服务器的 `.env` 文件中是否漏配。
  3. **中间件兼容性**：检查是否引入了需要额外 Docker 容器（如 Redis）或特定版本环境支持的新依赖。
- **输出要求**：将排查结果以“🚨 生产环境风险预警”为标题单独列出，直接提供可在 Linux 服务器上复制执行的升级/修复脚本（如 SQL、docker exec 命令）。如果没有隐患，则明确告知“环境审查通过”。
