# 乡村慢性病管理系统

基层卫生场景用的慢性病管理：患者、诊断、统计、导出。后端 Spring Boot 3，前端 Vue 3。

本地默认 H2，上线用 MySQL + Redis。首页有缓存、导出走流式、列表有分页上限。提交规范、测试和上线检查见下文。

## 版本

| 版本 | 时间 | 说明 |
|------|------|------|
| v5.0 | 2026-07 | 工程化门禁、H2/MySQL 分流、canary；生产 jar + Nginx |
| v4.0 | 2026-07 | 界面与移动端、首页/统计、筛选导出 |
| v3.0 | 2026-07 | 诊断、Excel、图表 |
| v2.x | 2026-07 | Redis、低配机优化 |
| v1.0 | 2026-07 | 患者管理基础 |

## 常用命令

| 做什么 | 命令 |
|--------|------|
| 约定式提交 | `cd frontend && npm run cz` |
| 启用 hooks | `git config core.hooksPath frontend/.husky` |
| 前端测试 | `cd frontend && npm run test:run` |
| 后端单测 | `cd backend && mvn test` |
| 后端全量校验 | `cd backend && mvn verify` |
| 上线清单 | [CHECKLIST.md](./CHECKLIST.md) |
| 部署说明 | [docs/deployment/README.md](./docs/deployment/README.md) |
| 宝塔步骤 | [docs/deployment/BAOTA.md](./docs/deployment/BAOTA.md) |
| 冒烟 | `pwsh ./scripts/smoke-test.ps1` 或 `bash scripts/smoke-test.sh` |
| 回滚 | `bash scripts/rollback.sh` |
| 巡检 | `bash scripts/canary-check.sh` |

```bash
cd frontend
npm install --registry=https://registry.npmmirror.com
git config core.hooksPath frontend/.husky   # 在仓库根目录执行
npm run test:run

cd ../backend
mvn test
# 需要时再跑：mvn verify
```

## 技术栈

**后端：** Java 17、Spring Boot 3.2.5、JPA、Security + JWT、Redis 缓存、POI（SXSSF）、H2（dev/test）/ MySQL（prod）、JUnit / Mockito / Testcontainers、Checkstyle、JaCoCo、springdoc（非 prod）、Actuator + Prometheus。

**前端：** Vue 3、Element Plus、ECharts 6（按需）、Vue Router、Vuex、Axios、Vite、ESLint / Prettier、Husky、Vitest。窄屏用 `useMobile`（≤430px）。

**上线：** Nginx + `backend.jar`（JDK 17）+ 本机 MySQL/Redis。Docker Compose 可选；监控栈见 `docker/docker-compose.monitoring.yml`。

## 目录

```
├── backend/                 # Spring Boot（包名 com.sjk.clinic）
├── frontend/                # Vue 3
├── docker/                  # compose、Dockerfile、nginx 示例
├── scripts/                 # 冒烟 / 回滚 / canary
├── performance/jmeter/
├── docs/                    # API、部署、故障模板等
├── CHECKLIST.md
└── nginx.conf
```

## 本地开发

需要 JDK 17+、Node 18+、Maven 3.8+。

**后端**（默认 `dev` + H2，可不启 Redis）：

```bash
cd backend
mvn spring-boot:run
```

- API：`http://localhost:8081/api/v1`
- H2：`http://localhost:8081/api/v1/h2-console`（JDBC `jdbc:h2:mem:devdb`，用户 `sa`，密码空）

**前端：**

```bash
cd frontend
npm install --registry=https://registry.npmmirror.com
npm run dev
```

- 页面：`http://localhost:5173`
- API 基址见 `.env.development`（默认指向本机 8081）

## 生产部署

步骤、目录、Nginx、`start.sh`、排障见 [docs/deployment/README.md](./docs/deployment/README.md)。宝塔面板操作见 [BAOTA.md](./docs/deployment/BAOTA.md)。

| 项 | 说明 |
|----|------|
| 域名 | `http://<your_domain>` |
| 前端 | Nginx `80` / `9090` 静态目录 |
| 后端 | `9091`，`context-path=/api/v1` |
| 前端构建 | `.env.production` 里 `VITE_APP_API_BASE_URL=/api/v1`，再 `npm run build` |
| 后端运行 | JDK 17 跑 jar，`SPRING_PROFILES_ACTIVE=prod` |
| 数据 | 本机 MySQL + Redis（有密码则配 `SPRING_DATA_REDIS_PASSWORD`） |

摘要：

```bash
cd frontend && npm run build
# 把 dist 覆盖到服务器 /www/wwwroot/<your_domain>/frontend

# 服务器上 JDK17 + Maven 打 jar，覆盖 backend.jar 后：
bash /www/wwwroot/<your_domain>/start.sh
tail -f /www/wwwroot/<your_domain>/app.log
```

上线前勾 [CHECKLIST.md](./CHECKLIST.md)，上线后跑冒烟和 `canary-check`。

### Docker（可选）

内存够、镜像拉得下再用：

```bash
cd docker
# .env 里配 DB_HOST / DB_NAME / DB_USER / DB_PASSWORD 等
docker compose up -d --build
```

前端 `9090`，后端 `9091/api/v1`，探针 `GET /api/v1/canary/probe`。

## 配置

| Profile | 库 | 缓存 |
|---------|----|------|
| `dev`（默认） | H2 | 内存 |
| `test` | H2 | 内存（可开混沌延迟） |
| `staging` | MySQL | 可选 Redis |
| `prod` | MySQL | Redis |

常用环境变量：`SPRING_PROFILES_ACTIVE`、`SERVER_PORT`、`DB_*`、`REDIS_*` / `SPRING_DATA_REDIS_PASSWORD`、`VITE_APP_API_BASE_URL`（生产务必 `/api/v1`，不要写成 `/api`）。

生产 JVM 参考：

```
-Xmx384m -Xms192m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=128m
```

Compose 资源上限大致：Redis 96MB、后端 640MB、前端 96MB。首页/统计缓存 TTL 约 5 分钟；写操作会失效相关缓存。不要缓存带懒加载关联的 JPA 实体，否则 Redis 序列化容易 500。

## 默认账号

| 用户 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 全权限 |
| member | member123 | 患者/诊断读写 |
| guest | guest123 | 只读 |

## 功能概要

- 首页工作台：待随访、高危、近一周趋势、待办与最近患者
- 患者：分页搜索、移动端卡片、按筛选导出
- 诊断：列表与 CRUD、摘要统计
- 统计：指标与图表、病情分级名单
- 导出：Excel/CSV、字段可选、流式写出
- 用户管理；运维侧有 canary、冒烟/回滚脚本

## API

基础路径 `/api/v1`。摘要：

- 认证：`POST /auth/login`、`/auth/logout`，`GET /auth/profile`
- 首页：`GET /home/dashboard`；巡检：`GET /canary/probe`
- 患者 / 诊断：标准 REST
- 统计：`GET /statistics/dashboard`；导出：`POST /export/filtered` 等
- 用户：`/users` CRUD 与启停

更细的约定见 [docs/API.md](./docs/API.md)。本地非 prod 可开 Swagger：`/api/v1/swagger-ui.html`。

## 移动端

断点 `max-width: 430px`。底部 Tab：首页、患者、分析、诊断、更多。列表用卡片；输入框字号 ≥16px，减少 iOS 自动放大。

## 监控

```bash
curl -fsS http://127.0.0.1:9091/api/v1/canary/probe
curl -fsS http://<your_domain>/api/v1/canary/probe

# 可选监控栈
cd docker && docker compose -f docker-compose.monitoring.yml up -d
```

## 升级

**v4 → v5：** 备份库 → 拉代码 → `frontend` 装依赖并设 hooks → `mvn verify` → 按部署文档重发 → 看 canary、首页、导出。

**v3 → v4：** 备份后重建部署，重点看首页指标、统计、导出和移动 Tab。

## 常见问题

**接口 404（写成了 `/api/...`）：** 生产前端必须是 `/api/v1`。改 `.env.production` 后重新 build 并覆盖静态文件，浏览器强刷。

**首页/统计 500，日志 `NOAUTH`：** Redis 有密码时启动要带 `SPRING_DATA_REDIS_PASSWORD`。

**患者详情 500（懒加载）：** 不要对含懒加载集合的 `Patient` 做 Redis `@Cacheable`；详情直接查库。

**根目录 `npm install` 失败：** 依赖在 `frontend` 目录装；可用 npmmirror。

**手机仍像桌面：** 视口压到 ≤430px，或用真机；宽屏侧栏是预期。

**导出和列表对不上：** 导出跟当前筛选/勾选走，先筛再导。

**`java` 启动失败：** 需要 Java 17，别用系统自带的 Java 8。

## 开发时注意

后端：业务放 Service；别空 catch；列表注意分页和 N+1；统计尽量用投影。  
前端：样式优先改 `tokens.css`；移动逻辑走 `useMobile`；ECharts 用完 dispose；提交走 hooks，类型用 `feat|fix|docs|...`。

## 许可证

仅供学习与研究使用。
