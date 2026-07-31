# 宝塔部署步骤

用宝塔装：前端静态 + 后端 jar（JDK 17）+ 本机 MySQL / Redis。端口一般是 9090（前端）和 9091（后端）。细节与 [README.md](./README.md) 一致。

真实账号密码放服务器本地，这里只写占位符。

---

## 0. 面板信息（自行填写）

| 项 | 值 |
|----|-----|
| 外网面板 | https://<your_server_ip>:<panel_port>/<panel_path> |
| 内网面板 | https://<your_intranet_ip>:<panel_port>/<panel_path> |
| 面板用户 | `<panel_user>` |
| 服务器公网 IP | `<your_server_ip>` |
| 域名 | `<your_domain>` |
| 前端端口 | `9090`（域名可走 `80`） |
| 后端端口 | `9091` |
| 站点目录 | `/www/wwwroot/<your_domain>` |

弱密码尽快改，并同步改启动脚本里的环境变量。

登录后在「软件商店」确认有：Nginx、MySQL 5.7、Redis、JDK **17**（不要用 8）。进程可用 `start.sh` + `nohup`，不一定要 pm2。

---

## 1. 域名与安全组

1. DNS：给 `<your_domain>` 加 **A 记录** → `<your_server_ip>`（已解析可跳过）。
2. 云厂商安全组 / 宝塔「安全」放行端口：
   - `80`（HTTP 域名）
   - `9090`（前端直连）
   - `9091`（后端，可不对公网开放，仅本机反代更安全）
   - `<panel_port>`（面板，已开）
3. 宝塔「安全」里同样放行上述端口。

---

## 2. 数据库（宝塔 → 数据库）

### 2.1 建库 / 建用户

1. 左侧 **数据库** → **添加数据库**  
   - 数据库名：`<your_db_name>`  
   - 用户名：`<your_db_name>`  
   - 密码：自行设置（请自行设置强密码）  
   - 访问权限：本地服务器（若 Docker 连库再开「所有人」）  
2. 字符集选 **utf8mb4**。

若库已存在，可在 phpMyAdmin 中确认有表：`patients`、`users`、`diagnosis_records` 等；没有也没关系，后端 `prod` 首次启动会按 JPA `update` 建表。

### 2.2 Redis

1. **软件商店** → Redis → 设置  
2. 确认已启动，记录：
   - 端口：`6379`
   - 密码：当前环境 `requirepass` 请自行设置（以你面板里实际为准）
3. 后端启动时必须带上 Redis 密码，否则首页/统计会 500。

---

## 3. 添加网站（前端）

1. **网站** → **添加站点**  
   - 域名：`<your_domain>`  
   - 根目录：`/www/wwwroot/<your_domain>`（或面板默认）  
   - PHP：纯静态可选「纯静态」  
2. 进入站点 → **目录**，在站点下准备目录结构：

```text
/www/wwwroot/<your_domain>/
├── frontend/          # 放前端 dist 产物（Nginx root 指向这里）
├── backend.jar        # 后端包
├── start.sh           # 启动脚本
├── app.log            # 运行日志
└── backup/            # 可选：旧 jar 备份
```

3. 若面板默认 root 是站点根目录，后面 Nginx 配置里把 `root` 改成 `.../frontend`（见第 6 节）。

---

## 4. 上传前端（本机打包 → 宝塔文件）

### 4.1 本机构建

```bash
cd frontend
npm ci --registry=https://registry.npmmirror.com
```

确认 `frontend/.env.production`：

```env
VITE_APP_API_BASE_URL=/api/v1
```

> 必须是 `/api/v1`，写成 `/api` 会接口 404。

```bash
npm run build
```

产物在 `frontend/dist/`。

### 4.2 上传

**方式 A：宝塔文件管理**

1. 打开 `/www/wwwroot/<your_domain>/frontend`  
2. 清空旧文件后，上传 `dist` 内全部内容（`index.html`、`assets/` 等）  
3. 确保存在：`frontend/index.html`

**方式 B：压缩包**

1. 本机把 `dist` 打成 `frontend.zip`  
2. 上传到站点目录 → 解压到 `frontend/`

上传后浏览器访问：http://<your_domain> （先配好 Nginx 再测）。

---

## 5. 部署后端 jar（JDK 17）

### 5.1 确认 Java 17

宝塔 → **软件商店** → **Java 环境管理器** / 已装 JDK：

- 使用路径示例：`/www/server/java/jdk-17.0.8/bin/java`
- 在 SSH 终端验证：

```bash
/www/server/java/jdk-17.0.8/bin/java -version
# 应显示 17.x
```

**不要用** 系统默认 Java 8 启动本项目（Spring Boot 3 要求 17）。

### 5.2 在服务器打包（推荐）

1. 宝塔 **终端** 或 SSH 登录服务器  
2. 安装 Maven（若无）：可参考主部署文档；或本机 `mvn package` 后只上传 jar  
3. 上传 `backend` 源码到例如 `/www/wwwroot/<your_domain>/build/backend`  
4. 执行：

```bash
export JAVA_HOME=/www/server/java/jdk-17.0.8
export PATH=/opt/maven/bin:$JAVA_HOME/bin:$PATH

cd /www/wwwroot/<your_domain>/build/backend
mvn -DskipTests clean package

# 把生成的 jar 拷到站点根（排除 *.original.jar）
cp -f target/*.jar /www/wwwroot/<your_domain>/backend.jar
```

编译前可先停掉不需要的后台服务，编完再开。

### 5.3 本机打包再上传

本机 JDK 17 + Maven：

```bash
cd backend
mvn -DskipTests clean package
```

用宝塔文件管理上传 `target/*.jar` 为：

`/www/wwwroot/<your_domain>/backend.jar`

### 5.4 启动脚本（宝塔文件管理创建）

路径：`/www/wwwroot/<your_domain>/start.sh`

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

在宝塔终端执行：

```bash
chmod +x /www/wwwroot/<your_domain>/start.sh
bash /www/wwwroot/<your_domain>/start.sh
tail -f /www/wwwroot/<your_domain>/app.log
```

看到 `Started ClinicApplication` 即成功。

**用宝塔「Java 项目管理器」时注意：**

- 运行端口填 `9091`
- 启动参数 / 环境变量务必带上上面的 `DB_*`、`SPRING_DATA_REDIS_PASSWORD`、`SPRING_PROFILES_ACTIVE=prod`
- JRE/JDK 选 **17**

### 5.5 自检

```bash
curl -fsS http://127.0.0.1:9091/api/v1/canary/probe
# 期望 JSON：status=UP, profile=prod
```

---

## 6. 配置 Nginx（宝塔网站设置）

1. 网站 → `<your_domain>` → **设置** → **配置文件**  
2. 替换为（或合并）以下内容：

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

3. 保存后点 **重载配置**（或终端 `nginx -t && nginx -s reload`）。

说明：

| 监听 | 用途 |
|------|------|
| `80` | http://<your_domain> |
| `9090` | http://<your_server_ip>:9090 |
| `/api/` → `9091` | 反代到 Spring Boot（完整路径仍是 `/api/v1/...`） |

若宝塔提示「端口被占用」，在「网站 → 设置 → 配置文件」确认没有第二个 `server` 抢 `9090`；或到「安全 / 防火墙」检查是否放行。

---

## 7. 访问验收

| 检查项 | 地址 / 操作 |
|--------|-------------|
| 前端页面 | http://<your_domain> |
| 端口访问 | http://<your_server_ip>:9090 |
| 巡检 | http://<your_domain>/api/v1/canary/probe |
| 登录 | `admin` / `admin123` |
| 业务 | 首页、患者、诊断、统计；诊断列表点患者名进详情 |

浏览器若仍异常：**Ctrl+F5** 强刷，避免旧前端缓存。

---

## 8. 日常运维（都在宝塔里能做）

### 更新前端

1. 本机 `npm run build`  
2. 文件管理覆盖 `frontend/`  
3. 用户端强刷

### 更新后端

1. 上传新 `backend.jar`（建议先复制到 `backup/backend.jar.日期`）  
2. 终端执行 `bash /www/wwwroot/<your_domain>/start.sh`  
3. **文件** → 查看 `app.log`，或终端 `tail -f app.log`

### 看日志

- 后端：`/www/wwwroot/<your_domain>/app.log`  
- Nginx：网站设置 → 日志，或 `/www/wwwlogs/<your_domain>.*.log`

### 开机自启（可选）

宝塔 → **计划任务** → 添加 Shell 脚本，类型选「系统启动时」：

```bash
bash /www/wwwroot/<your_domain>/start.sh
```

或使用「Java 项目管理器」勾选开机启动。

---

## 9. 常见问题（宝塔场景）

| 现象 | 处理 |
|------|------|
| 面板打不开 | 查安全组 `<panel_port>`、本机防火墙；用外网面板地址 |
| 网站 404 / 空白 | `frontend` 是否上传了 `index.html`；Nginx `root` 是否指向 `frontend` |
| 接口 404 | 前端是否 `/api/v1`；Nginx 是否有 `location /api/` |
| 登录后首页 500 | Redis 密码是否写入启动环境；面板 Redis 是否在运行 |
| 患者详情 500 | 使用当前仓库已修复版本的 jar（勿对 Patient 实体 Redis 缓存） |
| 后端起不来 | 是否 Java 17；`app.log` 是否 OOM；内存是否被 ES 占满 |
| 改配置不生效 | Nginx 要重载；前端要重新 build；后端要重新 `start.sh` |

更完整的环境变量与排障见：[README.md](./README.md)。

---

## 10. 操作清单（勾选）

- [ ] DNS A 记录指向 `<your_server_ip>`
- [ ] 安全组放行 `80` / `9090`（及按需 `9091`）
- [ ] MySQL 库 `<your_db_name>` 可用
- [ ] Redis 已启动并记下密码
- [ ] 安装 / 选用 JDK 17
- [ ] 前端 `.env.production` = `/api/v1` 并上传 `frontend/`
- [ ] `backend.jar` + `start.sh` 启动成功，canary 返回 UP
- [ ] Nginx `80`+`9090`，`/api/` 反代到 `9091`
- [ ] 登录与诊断→患者详情冒烟通过
- [ ] （建议）修改面板 / 数据库 / Redis 默认弱密码

---

## 11. HTTPS（可选，后续）

宝塔网站 → **SSL** → Let's Encrypt 申请证书并强制 HTTPS。  
申请前确保 `80` 可从外网访问且域名已解析。当前环境可先用 HTTP 验收。
