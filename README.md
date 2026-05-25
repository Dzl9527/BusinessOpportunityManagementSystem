# Business Opportunity Management System

企业微信账户体系下的商机提报、商机看板、用户权限和白名单管理系统。

## 项目结构

- `backend/`：Spring Boot + JPA + H2 后端服务，默认端口 `8080`
- `frontend/`：Vue 3 + Vite 前端应用，默认端口 `5173`
- `docs/`：需求、技术方案、变更记录和 SQL 设计文档
- `.github/`：Issue 模板和 PR 模板

## 本地启动

### 后端

```powershell
cd D:\antigravityproject\BusinessOpportunityManagementSystem\backend
.\.maven\apache-maven-3.9.6\bin\mvn.cmd "-Dmaven.repo.local=.m2repo" "-DskipTests" compile
.\.maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
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

版本计划查看：`docs/product/版本规划.md`

发布记录查看：`CHANGELOG.md`
