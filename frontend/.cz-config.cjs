module.exports = {
  types: [
    { value: 'feat', name: 'feat:     新增功能' },
    { value: 'fix', name: 'fix:      修复缺陷' },
    { value: 'docs', name: 'docs:     文档变更' },
    { value: 'style', name: 'style:    代码格式（不影响功能）' },
    { value: 'refactor', name: 'refactor: 代码重构' },
    { value: 'perf', name: 'perf:     性能优化' },
    { value: 'test', name: 'test:     测试相关' },
    { value: 'chore', name: 'chore:    构建/工具变更' },
    { value: 'ci', name: 'ci:       CI 配置' },
    { value: 'build', name: 'build:    构建系统' },
    { value: 'revert', name: 'revert:   回滚提交' },
  ],
  scopes: ['frontend', 'backend', 'config', 'db', 'docker', 'docs'],
  allowCustomScopes: true,
  allowBreakingChanges: ['feat', 'fix'],
};
