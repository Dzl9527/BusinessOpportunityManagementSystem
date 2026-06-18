# BOMS Docker Production Deployment Guide

本文档面向服务器部署，覆盖当前项目是否已经包含 Nginx、是否需要额外安装 Nginx、Docker Compose 一键部署、生产环境变量、端口开放、数据持久化、更新回滚和常见问题排查。

## 1. 当前项目里有没有 Nginx

有，但分两种情况：

1. 前端 Docker 镜像内置 Nginx。
   - 文件：`frontend/Dockerfile`
   - 构建方式：先用 `node:20-alpine` 编译 Vue 前端，再用 `nginx:alpine` 托管 `dist` 静态文件。
   - 容器内监听端口：`80`
   - `docker-compose.yml` 当前映射：服务器 `5173` -> 前端容器 `80`

2. `deploy/nginx.conf` 提供了宿主机 Nginx 示例配置。
   - 这是手动部署或域名部署时使用的 Nginx 反向代理示例。
   - 它不是当前 `docker-compose.yml` 自动启动的服务。

结论：

- 只按当前 `docker-compose.yml` 部署：不需要在宿主机额外安装 Nginx，前端容器里已经有 Nginx。
- 想用域名、`http://域名/`、`https://域名/`、只开放 `80/443`：建议在宿主机安装 Nginx，或额外增加一个 Nginx 反向代理容器。

## 2. 当前版本的一个重要限制

当前前端 API 地址写在：

```ts
frontend/src/stores/useAppStore.ts
```

当前代码：

```ts
export const API_HOST = window.location.hostname || 'localhost'
export const API_BASE = `http://${API_HOST}:8080/api`
```

这意味着浏览器访问前端页面后，会直接请求：

```text
http://服务器IP:8080/api
```

所以在不改代码的情况下：

- 服务器必须开放 `8080` 给浏览器访问。
- 前端页面访问地址是 `http://服务器IP:5173/`。
- 后端 API 地址是 `http://服务器IP:8080/api`。

如果希望只开放 `80/443`，并通过 Nginx 代理 `/api` 到后端，则建议把前端 API 改为同源：

```ts
export const API_BASE = '/api'
```

改完后重新构建前端，再使用 Nginx 代理：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
}
```

本文后面会分别给出“当前代码直接部署”和“推荐生产部署”两套方案。

## 3. 服务器最低环境要求

推荐服务器：

- CPU：2 核及以上
- 内存：4 GB 及以上，推荐 8 GB
- 磁盘：40 GB 及以上
- 操作系统：Ubuntu 22.04 / Ubuntu 24.04 / CentOS 7+ / Rocky Linux / AlmaLinux

必须安装：

- Git
- Docker Engine
- Docker Compose 插件，也就是支持 `docker compose`

可选安装：

- Nginx：仅在使用域名、HTTPS、统一入口 `80/443` 时需要
- Certbot：仅在申请 Let's Encrypt HTTPS 证书时需要

## 4. 端口规划

### 4.1 当前 Docker Compose 默认端口

| 服务 | 容器 | 宿主机端口 | 容器端口 | 是否建议公网开放 |
| --- | --- | --- | --- | --- |
| 前端 | `boms-frontend` | `5173` | `80` | 可以开放 |
| 后端 | `boms-backend` | `8080` | `8080` | 当前代码需要开放 |
| MySQL | `boms-mysql` | `3306` | `3306` | 不建议公网开放 |
| Redis | `boms-redis` | `6379` | `6379` | 不建议公网开放 |

### 4.2 推荐生产端口

如果使用宿主机 Nginx：

| 用途 | 端口 | 说明 |
| --- | --- | --- |
| HTTP | `80` | Nginx 对外入口 |
| HTTPS | `443` | Nginx HTTPS 入口 |
| 前端容器 | `5173` | 可只监听本机或内网 |
| 后端容器 | `8080` | 可只监听本机或内网 |
| MySQL | `3306` | 不开放公网 |
| Redis | `6379` | 不开放公网 |

## 5. Docker Compose 直接部署

这是当前仓库最省事的部署方式。

### 5.1 安装 Docker

Ubuntu 示例：

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg git

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

docker --version
docker compose version
```

CentOS / Rocky / AlmaLinux 示例：

```bash
sudo yum install -y yum-utils git
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker

docker --version
docker compose version
```

### 5.2 拉取代码

```bash
cd /opt
sudo git clone -b dev https://github.com/Dzl9527/BusinessOpportunityManagementSystem.git boms
cd /opt/boms
```

如果服务器已经有旧代码：

```bash
cd /opt/boms
sudo git fetch origin
sudo git checkout dev
sudo git reset --hard origin/dev
```

### 5.3 检查当前 Compose 配置

当前仓库自带：

```bash
cat docker-compose.yml
```

当前服务包括：

- `mysql`
- `redis`
- `backend`
- `frontend`

### 5.4 第一次启动

```bash
cd /opt/boms
sudo docker compose up -d --build
```

查看容器：

```bash
sudo docker compose ps
```

查看日志：

```bash
sudo docker compose logs -f backend
sudo docker compose logs -f frontend
sudo docker compose logs -f mysql
sudo docker compose logs -f redis
```

### 5.5 访问地址

当前代码不改的情况下：

```text
前端：http://服务器IP:5173/
后端：http://服务器IP:8080/
API：http://服务器IP:8080/api
```

浏览器访问：

```text
http://服务器IP:5173/
```

### 5.6 防火墙开放端口

Ubuntu ufw 示例：

```bash
sudo ufw allow 5173/tcp
sudo ufw allow 8080/tcp
sudo ufw status
```

CentOS / Rocky firewalld 示例：

```bash
sudo firewall-cmd --permanent --add-port=5173/tcp
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
sudo firewall-cmd --list-ports
```

云服务器还需要在安全组里开放：

- `5173/tcp`
- `8080/tcp`

MySQL `3306` 和 Redis `6379` 不建议开放到公网。

## 6. 生产环境必须调整的配置

当前 `docker-compose.yml` 使用演示级默认值：

```yaml
MYSQL_ROOT_PASSWORD: root
MYSQL_DATABASE: boms
SPRING_DATASOURCE_USERNAME: root
SPRING_DATASOURCE_PASSWORD: root
```

生产环境不要使用 `root/root`。

### 6.1 推荐新增 `.env`

在项目根目录创建：

```bash
cd /opt/boms
sudo vi .env
```

示例：

```env
MYSQL_ROOT_PASSWORD=请替换为强密码
MYSQL_DATABASE=boms
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=请替换为强密码
JWT_SECRET=请替换为至少32位随机字符串
JWT_EXPIRATION_MS=86400000
FEISHU_APP_ID=你的飞书AppID
FEISHU_APP_SECRET=你的飞书AppSecret
FEISHU_REDIRECT_URI=http://服务器IP:5173/login-callback
FEISHU_SANDBOX_MODE=false
```

生成随机 JWT 密钥示例：

```bash
openssl rand -base64 48
```

### 6.2 推荐调整 `docker-compose.yml`

把敏感配置改成读取 `.env`：

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: boms-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
    volumes:
      - mysql-data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7.0-alpine
    container_name: boms-redis

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: boms-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MS: ${JWT_EXPIRATION_MS}
      FEISHU_APP_ID: ${FEISHU_APP_ID}
      FEISHU_APP_SECRET: ${FEISHU_APP_SECRET}
      FEISHU_REDIRECT_URI: ${FEISHU_REDIRECT_URI}
      FEISHU_SANDBOX_MODE: ${FEISHU_SANDBOX_MODE}
    depends_on:
      - mysql
      - redis

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: boms-frontend
    ports:
      - "5173:80"
    depends_on:
      - backend

volumes:
  mysql-data:
```

重新启动：

```bash
sudo docker compose down
sudo docker compose up -d --build
```

## 7. 是否需要安装宿主机 Nginx

### 7.1 不安装宿主机 Nginx 的情况

满足以下条件时，可以不安装宿主机 Nginx：

- 你接受访问地址是 `http://服务器IP:5173/`
- 你接受后端 API 对浏览器开放 `http://服务器IP:8080/api`
- 暂时不配置 HTTPS
- 只是内网试用、演示或小范围测试

这时项目的前端容器已经使用 Nginx 托管静态文件。

### 7.2 建议安装宿主机 Nginx 的情况

满足以下任一条件时，建议安装宿主机 Nginx：

- 使用正式域名，例如 `boms.example.com`
- 希望用户访问 `http://域名/`，不带 `:5173`
- 希望启用 HTTPS
- 希望只开放 `80/443`，不开放 `5173/8080`
- 希望统一处理上传大小、日志、gzip、缓存、证书续期

注意：如果只安装 Nginx 但不改前端 API 地址，前端仍然会请求 `域名:8080`。要完全走 Nginx `/api` 反代，建议先把前端 `API_BASE` 改成 `/api`。

## 8. 推荐生产部署：宿主机 Nginx + Docker Compose

推荐目标：

```text
用户 -> http://域名/ 或 https://域名/
Nginx -> 前端容器 5173
Nginx /api -> 后端容器 8080
MySQL / Redis 仅容器内部访问
```

### 8.1 修改前端 API 地址

编辑：

```bash
frontend/src/stores/useAppStore.ts
```

修改为：

```ts
export const API_BASE = '/api'
```

重新构建前端容器：

```bash
sudo docker compose up -d --build frontend
```

### 8.2 安装宿主机 Nginx

Ubuntu：

```bash
sudo apt update
sudo apt install -y nginx
sudo systemctl enable --now nginx
```

CentOS / Rocky：

```bash
sudo yum install -y nginx
sudo systemctl enable --now nginx
```

### 8.3 Nginx 配置

创建：

```bash
sudo vi /etc/nginx/conf.d/boms.conf
```

配置示例：

```nginx
server {
    listen 80;
    server_name 你的域名或服务器IP;

    client_max_body_size 50m;

    location / {
        proxy_pass http://127.0.0.1:5173;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

检查配置：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

访问：

```text
http://你的域名/
```

### 8.4 使用 HTTPS

域名解析到服务器后，安装 Certbot：

Ubuntu：

```bash
sudo apt install -y certbot python3-certbot-nginx
```

申请证书：

```bash
sudo certbot --nginx -d 你的域名
```

检查自动续期：

```bash
sudo certbot renew --dry-run
```

如果使用 HTTPS，飞书回调地址也应改成：

```text
https://你的域名/login-callback
```

并同步更新：

```env
FEISHU_REDIRECT_URI=https://你的域名/login-callback
```

## 9. 数据库说明

### 9.1 当前生产数据库

生产 profile 使用 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/boms?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

容器内数据库名默认：

```text
boms
```

数据持久化 volume：

```text
mysql-data
```

### 9.2 表结构

后端配置：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

应用启动时会根据实体自动创建或更新表结构。

仓库里还有迁移草稿：

```text
deploy/sql/2026-05-24-opportunity-field-migration.sql
```

该 SQL 注释中说明需要按实际数据库方言调整，生产执行前必须先备份数据库并在测试库验证。

### 9.3 进入 MySQL

```bash
sudo docker exec -it boms-mysql mysql -uroot -p
```

查看数据库：

```sql
SHOW DATABASES;
USE boms;
SHOW TABLES;
```

## 10. Redis 说明

生产 profile 使用 Redis 缓存：

```yaml
spring:
  redis:
    host: redis
    port: 6379
  cache:
    type: redis
```

缓存 TTL 在代码中配置为 5 分钟：

```text
backend/src/main/java/com/boms/config/CacheConfig.java
```

Redis 不应开放公网。

查看 Redis 容器日志：

```bash
sudo docker compose logs -f redis
```

进入 Redis：

```bash
sudo docker exec -it boms-redis redis-cli
```

## 11. 飞书配置

当前默认配置是演示值：

```yaml
feishu:
  app-id: cli_aa930a532a78dcbb
  app-secret: ApUwfVXA5fAvi2FSvleGLgq2sICGCgaf
  redirect-uri: http://localhost:5173/login-callback
  sandbox-mode: true
```

正式部署需要替换为真实飞书应用配置：

```env
FEISHU_APP_ID=真实AppID
FEISHU_APP_SECRET=真实AppSecret
FEISHU_REDIRECT_URI=http://服务器IP:5173/login-callback
FEISHU_SANDBOX_MODE=false
```

如果使用域名和 HTTPS：

```env
FEISHU_REDIRECT_URI=https://你的域名/login-callback
```

飞书后台也要同步配置可信域名、回调地址或授权域名。

## 12. 常用运维命令

### 12.1 查看服务状态

```bash
cd /opt/boms
sudo docker compose ps
```

### 12.2 查看全部日志

```bash
sudo docker compose logs -f
```

### 12.3 查看单个服务日志

```bash
sudo docker compose logs -f backend
sudo docker compose logs -f frontend
sudo docker compose logs -f mysql
sudo docker compose logs -f redis
```

### 12.4 重启服务

```bash
sudo docker compose restart
```

重启单个服务：

```bash
sudo docker compose restart backend
sudo docker compose restart frontend
```

### 12.5 停止服务

```bash
sudo docker compose down
```

注意：`docker compose down` 不会删除 `mysql-data` volume，数据库数据还在。

不要随便执行：

```bash
sudo docker compose down -v
```

`-v` 会删除 volume，可能导致数据库数据丢失。

## 13. 更新部署

### 13.1 拉取最新 dev 分支

```bash
cd /opt/boms
sudo git fetch origin
sudo git checkout dev
sudo git reset --hard origin/dev
```

### 13.2 重新构建并启动

```bash
sudo docker compose up -d --build
```

### 13.3 检查状态

```bash
sudo docker compose ps
sudo docker compose logs --tail=100 backend
sudo docker compose logs --tail=100 frontend
```

## 14. 备份和恢复

### 14.1 备份 MySQL

```bash
mkdir -p /opt/boms-backups
sudo docker exec boms-mysql mysqldump -uroot -p boms > /opt/boms-backups/boms-$(date +%F-%H%M%S).sql
```

执行后会提示输入 MySQL root 密码。

### 14.2 恢复 MySQL

```bash
cat /opt/boms-backups/你的备份文件.sql | sudo docker exec -i boms-mysql mysql -uroot -p boms
```

恢复前建议先停后端：

```bash
sudo docker compose stop backend
```

恢复后启动：

```bash
sudo docker compose start backend
```

## 15. 健康检查

### 15.1 前端检查

```bash
curl -I http://127.0.0.1:5173/
```

预期看到：

```text
HTTP/1.1 200 OK
```

### 15.2 后端检查

当前根路径可能因为安全配置返回 `403`，这不一定代表后端异常。

可以检查端口：

```bash
curl -I http://127.0.0.1:8080/
```

如果返回 `403`，说明 Spring Security 在工作，服务本身已经响应。

也可以看后端日志：

```bash
sudo docker compose logs --tail=200 backend
```

看到类似信息说明后端启动完成：

```text
Tomcat started on port 8080
Started Application
```

## 16. 常见问题

### 16.1 前端页面打不开

检查：

```bash
sudo docker compose ps
sudo docker compose logs --tail=100 frontend
curl -I http://127.0.0.1:5173/
```

确认防火墙和云安全组开放 `5173`。

### 16.2 页面打开了，但接口请求失败

当前代码会请求：

```text
http://服务器IP:8080/api
```

检查：

```bash
sudo docker compose ps
sudo docker compose logs --tail=200 backend
curl -I http://127.0.0.1:8080/
```

确认防火墙和云安全组开放 `8080`。

如果你用了域名和 Nginx，希望接口走 `/api`，请确认已经把前端 `API_BASE` 改成 `/api` 并重新构建。

### 16.3 后端启动失败，提示连不上 MySQL

检查 MySQL 是否启动：

```bash
sudo docker compose ps mysql
sudo docker compose logs --tail=200 mysql
```

检查后端环境变量：

```bash
sudo docker compose exec backend env | grep SPRING_DATASOURCE
```

### 16.4 后端启动失败，提示 Redis 连接异常

检查 Redis：

```bash
sudo docker compose ps redis
sudo docker compose logs --tail=100 redis
```

### 16.5 修改配置后没有生效

重新构建并启动：

```bash
sudo docker compose up -d --build
```

如果只是环境变量变更：

```bash
sudo docker compose up -d --force-recreate backend
```

### 16.6 端口被占用

查看占用：

```bash
sudo lsof -i :5173
sudo lsof -i :8080
```

修改 `docker-compose.yml` 端口映射，例如：

```yaml
ports:
  - "18080:8080"
```

修改后：

```bash
sudo docker compose up -d
```

## 17. 上线前检查清单

上线前逐项确认：

- 已安装 Docker 和 Docker Compose
- 已拉取 `dev` 分支最新代码
- 已修改 MySQL 密码
- 已修改 JWT 密钥
- 已配置飞书真实参数
- 已确认是否使用沙箱模式
- 已确认访问方式：`IP:5173` 或域名 `80/443`
- 如果使用域名和 Nginx，已确认前端 API 是否改为 `/api`
- 已确认防火墙和云安全组端口
- 已确认 MySQL volume 持久化
- 已完成一次数据库备份演练
- 已保存 `.env`，并避免提交到 Git

## 18. 当前建议

如果只是先把系统在服务器跑起来：

1. 不安装宿主机 Nginx。
2. 直接用 `docker compose up -d --build`。
3. 开放 `5173` 和 `8080`。
4. 访问 `http://服务器IP:5173/`。

如果准备正式给用户使用：

1. 安装宿主机 Nginx。
2. 配置域名和 HTTPS。
3. 修改前端 `API_BASE` 为 `/api`。
4. 只开放 `80/443`。
5. 不对公网开放 MySQL 和 Redis。
6. 使用强密码和真实飞书配置。

