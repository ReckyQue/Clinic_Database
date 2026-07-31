# API 说明

基础路径：`/api/v1`  
本地（dev / staging）可开 Swagger：`http://localhost:8081/api/v1/swagger-ui.html`

## 约定

- 路径小写，资源用复数：`/patients`、`/diagnoses`、`/users`
- 方法：GET / POST / PUT / DELETE
- 响应：`{ "code": 200, "message": "...", "data": ... }`
- 异常走全局处理，不要空 catch

## 主要接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/auth/login` | 登录 | 无 |
| GET | `/home/dashboard` | 首页数据 | 可匿名 |
| GET | `/patients` | 患者分页 | 可匿名 |
| POST | `/patients` | 新建患者 | ADMIN / MEMBER |
| GET | `/diagnoses` | 诊断列表 | ADMIN / MEMBER |
| GET | `/statistics/dashboard` | 统计 | 可匿名 |
| POST | `/export/filtered` | 按条件导出 | ADMIN / MEMBER |
| GET | `/canary/probe` | 巡检 | 无 |
| GET | `/actuator/health` | 健康检查 | 无 |

更全的列表见根目录 README。OpenAPI 配置在 `OpenApiConfig`（prod 不启用页面）。
