# business-opportunity-system

企业微信账户体系下的商机提报、商机看板、用户权限、白名单管理、OA 同步和进度提醒系统。

## 项目背景

本项目用于统一管理设备事业群商机从提报、跟进、报备、授权、投标到交付的全流程数据。系统以商机主数据为核心，支持移动端提报、权限隔离、字段级修订记录、企业微信提醒和后续 OA 流程集成。

## 技术栈

- 后端：Java 17、Spring Boot、Spring Data JPA
- 前端：Vue 3、Vite
- 数据库：MySQL 目标环境，当前本地演示仍使用 H2
- 原型：HTML/Vue 静态原型
- 文档：Markdown
- CI：GitHub Actions

## 项目结构

- `backend/`：Spring Boot + JPA + H2 后端服务，默认端口 `8080`
- `frontend/`：Vue 3 + Vite 前端应用，默认端口 `5173`
- `database/`：MySQL 数据库建表、迁移和初始化脚本
- `deploy/`：部署配置、环境变量示例和发布脚本
- `docs/`：产品、流程、权限、接口、验收和技术方案文档
- `prototype/`：HTML/Vue 静态原型文件
- `tests/`：测试用例、测试数据和验收脚本
- `tools/`：可复用的本地联调、数据同步和验证脚本
- `scratch/`：一次性实验脚本和临时草稿，不作为稳定接口依赖
- `.github/`：Issue 模板和 PR 模板

## 本地启动

要求：

- JDK 17
- Maven 3.9+（推荐安装到系统 `PATH`；如果本机已有 `backend/.maven/` 缓存，也可临时使用其中的 `mvn.cmd`）
- Node.js 18+
- MySQL 8.x（生产或试点环境）

### 后端

```powershell
cd D:\antigravityproject\BusinessOpportunityManagementSystem\backend
mvn "-DskipTests" compile
mvn spring-boot:run
```

### 前端

```powershell
cd D:\antigravityproject\BusinessOpportunityManagementSystem\frontend
npm install
npm run dev -- --host 0.0.0.0
```

浏览器访问：

- 本机：`http://localhost:5173`
- 手机同网段测试：`http://电脑IPv4:5173`

## 验证

```powershell
cd D:\antigravityproject\BusinessOpportunityManagementSystem\backend
mvn test

cd D:\antigravityproject\BusinessOpportunityManagementSystem\frontend
npm ci
npm run build
```

端到端手动联调可在前后端服务启动后执行：

```powershell
cd D:\antigravityproject\BusinessOpportunityManagementSystem
.\tools\tests\test-boms.ps1
```

## 当前一期能力

- 手机端商机提报
- 我的商机/白名单范围商机看板
- 企业微信沙箱登录，预留真实企业微信接入
- 用户管理和用户白名单配置
- 管理员查看全部商机
- 历史 Excel 导入数据默认 `UNASSIGNED`，仅管理员可见
- 商机字段级修订记录

## 项目管理

- 稳定分支：`main`
- 开发集成分支：`dev`
- 新功能分支：`feature/功能名称`
- 修复分支：`fix/问题名称`

版本计划查看：`docs/core/版本规划.md`

发布记录查看：`CHANGELOG.md`
