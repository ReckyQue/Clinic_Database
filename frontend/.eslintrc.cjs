module.exports = {
  root: true,
  env: {
    node: true,
    browser: true,
    es2022: true,
    'vue/setup-compiler-macros': true,
  },
  // 渐进接入 Airbnb：已安装 @vue/eslint-config-airbnb，可按模块打开
  extends: [
    'plugin:vue/vue3-recommended',
    'eslint:recommended',
    'plugin:prettier/recommended',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  rules: {
    'prettier/prettier': 'error',
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off',
  },
  overrides: [
    {
      files: ['**/*.{test,spec}.{js,ts}'],
      env: { jest: true },
    },
  ],
};
