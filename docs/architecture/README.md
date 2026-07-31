# 架构备忘

## API

- 前缀 `/api/v1`，资源名复数、小写
- 统一 `Result<T>`，异常见 `GlobalExceptionHandler`

## 数据访问

- 列表 `size` 上限 100
- 统计尽量投影 / 查最新诊断，少 N+1、少整表实体
- 需要关联时用 `JOIN FETCH`（见 `DiagnosisRecordRepository`）

## 日志

- Logback（`logback-spring.xml`）
- 生产 INFO，开发 DEBUG；ERROR 单独滚文件
- 出错要打上下文和堆栈，别吞

## 环境

| Profile | 库 | 缓存 |
|---------|----|------|
| dev | H2 | 内存 |
| test | H2 | 内存（可混沌） |
| staging | MySQL | Redis 可选 |
| prod | MySQL | Redis |
