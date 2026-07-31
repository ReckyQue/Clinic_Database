# 上线前检查

## 构建与代码

- [ ] 前端 `npm run lint`、`npm run test:run` 通过
- [ ] 后端 `mvn -f backend/pom.xml test` 通过
- [ ] 后端 `mvn -f backend/pom.xml verify` 通过（含 IT、JaCoCo）
- [ ] Checkstyle 无错误
- [ ] 有人看过 diff
- [ ] 提交信息符合 `feat|fix|docs|...`

## 配置

- [ ] 生产环境变量已备好，密钥不进仓库
- [ ] MySQL 库已建，账号权限够用即可
- [ ] Redis 密码和内存上限按机器情况设好
- [ ] 生产关掉 Swagger

## 发布

- [ ] 镜像或 jar 版本可区分（便于回滚）
- [ ] 灰度方式清楚（可参考 `docker/nginx-canary.conf.example`）
- [ ] 回滚步骤演练过（`scripts/rollback.sh`）
- [ ] 冒烟脚本通过（`scripts/smoke-test.ps1` / `smoke-test.sh`）

## 观测

- [ ] `/api/v1/canary/probe` 通
- [ ] `/actuator/health` 通
- [ ] （可选）Prometheus / Grafana 已起
- [ ] （可选）错误率告警已配

## 上线后

- [ ] 跑一遍 `scripts/canary-check.sh`
- [ ] 手测：登录、首页、患者、诊断、导出
- [ ] 看一会儿日志和 CPU / 内存
