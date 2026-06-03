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

