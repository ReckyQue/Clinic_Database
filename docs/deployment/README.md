# 线上部署说明

环境：CentOS + 宝塔，本机 MySQL / Redis / Nginx。  
推荐做法：本机打前端包，服务器用 JDK 17 打 jar，Nginx 反代。不必上完整 Docker Compose。

宝塔逐步操作见 [BAOTA.md](./BAOTA.md)。账号密码等只放服务器本地，公开文档里用占位符。

---

## 1. 线上信息

| 项 | 值 |
|----|-----|
| 域名 | `<your_domain>`（A 记录 → 你的服务器公网 IP） |
| 站点目录 | `/www/wwwroot/<your_domain>` |
| 前端静态 | `/www/wwwroot/<your_domain>/frontend` |
| 后端 jar | `/www/wwwroot/<your_domain>/backend.jar` |
| 启动脚本 | `/www/wwwroot/<your_domain>/start.sh` |
| 运行日志 | `/www/wwwroot/<your_domain>/app.log` |
| Nginx 站点 | `/www/server/panel/vhost/nginx/<your_domain>.conf` |
| 前端入口 | `80`（域名）、`9090` |
| 后端端口 | `9091`（仅本机 / 已放行时直连） |
| JDK | `/www/server/java/jdk-17.0.8`（**必须 Java 17**，勿用系统默认 Java 8） |
| Maven | `/opt/maven`（3.9.x） |

### 数据库 / Redis

| 项 | 值 |
|----|-----|
| MySQL | 5.7.44，本机 `127.0.0.1:3306` |
| 库名 | `<your_db_name>` |
| 用户 / 密码 | `<your_db_name>` / `<your_db_password>` |
| Redis | 本机 `127.0.0.1:6379`，密码 `<your_redis_password>`（`requirepass`） |

### 访问地址

- 站点：http://<your_domain>  
- 端口：http://\<IP\>:9090  
- 巡检：http://<your_domain>/api/v1/canary/probe  
- 默认账号：`admin` / `admin123`（另有 member / guest，见根 README）

---

## 2. 架构（当前生产）

```
浏览器
  │
  ├─ http://<your_domain>:80
  └─ http://IP:9090
        │
        ▼
   宿主机 Nginx
   ├─ /          → 静态目录 frontend/（SPA try_files）
   └─ /api/      → http://127.0.0.1:9091/api/
                        │
                        ▼
                 Spring Boot (prod)
                 context-path: /api/v1
                 ├─ MySQL <your_db_name>
                 └─ Redis（带密码）
```

前端生产环境变量（构建时写入）：

```env
# frontend/.env.production
VITE_APP_API_BASE_URL=/api/v1
```

> 注意：必须是 `/api/v1`，写成 `/api` 会导致登录与业务接口 404。

---

## 3. 首次部署步骤

### 3.1 准备数据库

```sql
CREATE DATABASE IF NOT EXISTS <your_db_name>
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 用户已存在则可跳过；需保证可从本机连接
GRANT ALL ON <your_db_name>.* TO '<your_db_name>'@'localhost' IDENTIFIED BY '<your_db_password>';
GRANT ALL ON <your_db_name>.* TO '<your_db_name>'@'%' IDENTIFIED BY '<your_db_password>';
FLUSH PRIVILEGES;
```

表结构由 JPA `ddl-auto: update` 在首次启动时自动补齐（prod）。

### 3.2 本机构建前端

本机需 Node.js 18+：

```bash
cd frontend
npm ci --registry=https://registry.npmmirror.com
# 确认 .env.production 为 VITE_APP_API_BASE_URL=/api/v1
npm run build
```

产物在 `frontend/dist/`，上传到服务器：

```text
/www/wwwroot/<your_domain>/frontend/
```

### 3.3 服务器构建后端

服务器需 JDK 17 + Maven（阿里云镜像建议配置在 `~/.m2/settings.xml`）。

```bash
# 将 backend/ 源码放到服务器，例如：
# /www/wwwroot/<your_domain>/build/backend

export JAVA_HOME=/www/server/java/jdk-17.0.8
export PATH=/opt/maven/bin:$JAVA_HOME/bin:$PATH

cd /www/wwwroot/<your_domain>/build/backend
mvn -DskipTests clean package

cp -f target/*.jar /www/wwwroot/<your_domain>/backend.jar
# 实际 jar 名以 target 下为准（排除 *.original.jar）
```

编译前可先停掉不需要的后台服务，编完再拉起。

### 3.4 启动脚本 `start.sh`

```bash
#!/bin/bash
SITE=/www/wwwroot/<your_domain>
JAVA17=/www/server/java/jdk-17.0.8/bin/java
cd "$SITE"
pkill -f "$SITE/backend.jar" || true
sleep 2
nohup env \
  SPRING_PROFILES_ACTIVE=prod \
  SERVER_PORT=9091 \
  DB_HOST=127.0.0.1 \
  DB_PORT=3306 \
  DB_NAME=<your_db_name> \
  DB_USER=<your_db_name> \
  DB_PASSWORD=<your_db_password> \
  DB_SSL=false \
  REDIS_HOST=127.0.0.1 \
  REDIS_PORT=6379 \
  SPRING_DATA_REDIS_PASSWORD=<your_redis_password> \
  JAVA_TOOL_OPTIONS="-Xmx384m -Xms192m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=128m" \
  "$JAVA17" -jar "$SITE/backend.jar" > "$SITE/app.log" 2>&1 &
echo $! > "$SITE/backend.pid"
echo "started pid=$(cat $SITE/backend.pid)"
```

```bash
chmod +x /www/wwwroot/<your_domain>/start.sh
bash /www/wwwroot/<your_domain>/start.sh
```

### 3.5 Nginx 配置

文件：`/www/server/panel/vhost/nginx/<your_domain>.conf`

```nginx
server {
    listen 80;
    listen 9090;
    server_name <your_domain>;

    client_max_body_size 50m;

    location / {
        root /www/wwwroot/<your_domain>/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;

        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9091/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
        proxy_http_version 1.1;
    }

    access_log /www/wwwlogs/<your_domain>.access.log;
    error_log /www/wwwlogs/<your_domain>.error.log;
}
```

```bash
nginx -t && nginx -s reload
```

防火墙 / 安全组放行：`80`、`9090`、`9091`（9091 也可仅内网）。

---

## 4. 日常更新

### 只更新前端

```bash
cd frontend
npm run build
# 上传 dist/* 覆盖服务器 frontend/
```

浏览器请 **Ctrl+F5** 强刷，避免旧 JS 缓存。

### 只更新后端

```bash
# 上传新 backend 源码 → mvn package → 覆盖 backend.jar
bash /www/wwwroot/<your_domain>/start.sh
tail -f /www/wwwroot/<your_domain>/app.log
```

### 健康检查

```bash
curl -fsS http://127.0.0.1:9091/api/v1/canary/probe
curl -fsS http://<your_domain>/api/v1/canary/probe
ss -lntp | grep -E '9090|9091'
```

上线清单与冒烟脚本见：[CHECKLIST.md](../../CHECKLIST.md)、`scripts/smoke-test.ps1` / `scripts/smoke-test.sh`。

---

## 5. 环境变量一览（后端 prod）

| 变量 | 说明 | 生产示例 |
|------|------|----------|
| `SPRING_PROFILES_ACTIVE` | 激活配置 | `prod` |
| `SERVER_PORT` | 监听端口 | `9091` |
| `DB_HOST` / `DB_PORT` | MySQL | `127.0.0.1` / `3306` |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | 库账号 | 见上文 |
| `DB_SSL` | 是否 SSL | `false` |
| `REDIS_HOST` / `REDIS_PORT` | Redis | `127.0.0.1` / `6379` |
| `SPRING_DATA_REDIS_PASSWORD` 或 `REDIS_PASSWORD` | Redis 密码 | `<your_redis_password>` |
| `JAVA_TOOL_OPTIONS` | JVM 参数 | 堆约 384m |

`application-prod.yml` 中 Redis 密码项为：`spring.data.redis.password: ${REDIS_PASSWORD:}`，也可用 `SPRING_DATA_REDIS_PASSWORD` 注入。

---

## 6. 常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| `/api/auth/login` 等 404 | 前端 baseURL 不是 `/api/v1` | 改 `.env.production` 后重新 `npm run build` 并覆盖静态资源 |
| 首页 / 统计 500，日志 `NOAUTH` / Redis | Redis 有密码未配置 | `start.sh` 增加 `SPRING_DATA_REDIS_PASSWORD` |
| `/api/v1/patients/{id}` 500，日志 `LazyInitializationException` | 缓存实体时序列化懒加载集合 | 勿对带懒加载关联的 JPA 实体做 Redis `@Cacheable`（已修复：详情接口不缓存实体） |
| 进程起不来 / OOM | 内存被其它进程占满 | 编译或启动前腾内存；保持 JVM `-Xmx384m` |
| 用 `java -jar` 启动失败 | 系统默认 Java 8 | 必须用 JDK 17 绝对路径 |
| 域名 80 正常、IP 裸访问「没有找到站点」 | 未带 `Host: <your_domain>` | 请用域名或 `:9090` |

---

## 7. Docker Compose（可选，非当前生产）

仓库仍保留 `docker/docker-compose.yml`（前端映射 `9090`、后端 `9091`，连宿主机 MySQL）。  
适合内存更充裕、且本机/服务器能拉 Docker 镜像的环境：

```bash
cd docker
# 配置 .env：DB_* 、必要时 REDIS
docker compose up -d --build
```

容器内 MySQL 主机一般为 `host.docker.internal`；若用宿主机 Redis，需额外配置密码与网络，**不要与现有 jar 进程抢 9091 端口**。

监控栈（可选，占额外内存）：

```bash
docker compose -f docker/docker-compose.monitoring.yml up -d
```

---

## 8. 回滚

1. 后端：用站点目录 `backup/` 中旧 `backend.jar.*` 覆盖 `backend.jar`，再执行 `start.sh`。  
2. 前端：保留上一版 `frontend` 目录备份，整目录换回后 `nginx -s reload`（一般不必）。  
3. 脚本回滚示例：`PREV_TAG=previous bash scripts/rollback.sh`（需按镜像/标签体系使用时再启用）。

---

## 9. 安全建议

- root / 数据库 / Redis 密码过弱时请尽快更换，并同步改 `start.sh` 与 MySQL 授权。  
- 生产建议后续上 HTTPS（宝塔申请证书即可），HTTP 仅作内测过渡。  
- `9091` 尽量不对公网开放，只经 Nginx 反代。
