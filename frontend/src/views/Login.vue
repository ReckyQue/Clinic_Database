<template>
  <div class="login-page">
    <div class="glow glow-tl" aria-hidden="true" />
    <div class="glow glow-br" aria-hidden="true" />

    <div class="login-card">
      <div class="brand-icon" aria-hidden="true">
        <svg viewBox="0 0 48 48" width="28" height="28" fill="none">
          <path
            d="M24 38s-12-7.4-12-16.2C12 16.2 16.2 13 20.2 13c2.3 0 3.8 1.1 3.8 1.1S25.5 13 27.8 13C31.8 13 36 16.2 36 21.8 36 30.6 24 38 24 38Z"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linejoin="round"
          />
          <path
            d="M18 23.5h12M24 17.5v12"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
          />
        </svg>
      </div>

      <h1 class="brand-title">乡村慢性病管理系统</h1>
      <p class="brand-desc">登录以管理辖区慢性病患者数据</p>

      <el-form
        ref="loginFormRef"
        class="login-form"
        :model="loginForm"
        :rules="rules"
        label-width="0"
        @submit.prevent
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            class="apple-input"
            placeholder="请输入用户名"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon :size="18"><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            class="apple-input"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon :size="18"><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn pressable"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="demo-area">
        <div class="demo-hint">点击标签可快捷填充测试账号</div>
        <div class="demo-pills">
          <button
            v-for="item in demoAccounts"
            :key="item.username"
            type="button"
            class="demo-pill pressable"
            @click="fillDemo(item)"
          >
            {{ item.label }}：{{ item.username }}
          </button>
        </div>
      </div>
    </div>

    <footer class="login-footer">© 2026 乡村慢性病管理系统</footer>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import { Lock, User } from '@element-plus/icons-vue';
import request from '@/utils/request';

const router = useRouter();
const store = useStore();
const loginFormRef = ref(null);
const loading = ref(false);

const loginForm = reactive({
  username: '',
  password: '',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const demoAccounts = [
  { label: '管理员', username: 'admin', password: 'admin123' },
  { label: '成员', username: 'member', password: 'member123' },
  { label: '游客', username: 'guest', password: 'guest123' },
];

const fillDemo = (item) => {
  loginForm.username = item.username;
  loginForm.password = item.password;
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const res = await request.post('/auth/login', loginForm);
      await store.dispatch('login', res.data);
      ElMessage.success('登录成功');
      router.push('/dashboard');
    } catch (error) {
      console.error('登录失败:', error);
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped>
.login-page {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px 24px;
  background: linear-gradient(135deg, #e8f5e9 0%, #e3f2fd 52%, #b3e5fc 100%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
  z-index: 0;
}

.glow-tl {
  width: 420px;
  height: 420px;
  top: -120px;
  left: -80px;
  background: rgba(52, 199, 89, 0.28);
}

.glow-br {
  width: 480px;
  height: 480px;
  right: -140px;
  bottom: -160px;
  background: rgba(0, 122, 255, 0.22);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  padding: 36px 28px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px) saturate(160%);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.7);
  opacity: 0;
  transform: translateY(12px) scale(0.98);
  animation: card-enter 280ms var(--ease-out, cubic-bezier(0.23, 1, 0.32, 1)) forwards;
}

@keyframes card-enter {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.brand-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #007aff;
  background: #e8f2ff;
}

.brand-title {
  margin: 0;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.25;
  color: #1c1c1e;
}

.brand-desc {
  margin: 8px 0 28px;
  text-align: center;
  font-size: 14px;
  line-height: 1.5;
  color: #8e8e93;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.apple-input :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 12px !important;
  background: #f5f5f7 !important;
  box-shadow: none !important;
  transition:
    box-shadow 180ms cubic-bezier(0.23, 1, 0.32, 1),
    background-color 180ms ease;
}

.apple-input :deep(.el-input__wrapper.is-focus) {
  background: #fff !important;
  box-shadow:
    0 0 0 1px #007aff,
    0 0 0 4px rgba(0, 122, 255, 0.15) !important;
}

.apple-input :deep(.el-input__inner) {
  font-size: 16px;
  color: #1c1c1e;
}

.apple-input :deep(.el-input__prefix) {
  color: #8e8e93;
}

.login-btn {
  width: 100%;
  height: 48px !important;
  border-radius: 12px !important;
  font-size: 17px !important;
  font-weight: 650 !important;
  letter-spacing: 0.28em;
  text-indent: 0.28em;
  background: #007aff !important;
  border-color: #007aff !important;
}

.login-btn:hover,
.login-btn:focus {
  background: #0066d6 !important;
  border-color: #0066d6 !important;
}

.demo-area {
  margin-top: 8px;
}

.demo-hint {
  text-align: center;
  font-size: 12px;
  color: #8e8e93;
  margin-bottom: 10px;
}

.demo-pills {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.demo-pill {
  border: none;
  cursor: pointer;
  background: #f2f2f7;
  color: #8e8e93;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  line-height: 1.4;
  transition:
    background-color 160ms ease,
    color 160ms ease,
    transform 140ms cubic-bezier(0.23, 1, 0.32, 1);
}

@media (hover: hover) and (pointer: fine) {
  .demo-pill:hover {
    background: #e8f2ff;
    color: #007aff;
  }
}

.login-footer {
  position: relative;
  z-index: 1;
  margin-top: 28px;
  font-size: 12px;
  color: rgba(28, 28, 30, 0.45);
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .login-card {
    animation: none;
    opacity: 1;
    transform: none;
  }
}

@media (prefers-reduced-transparency: reduce) {
  .login-card {
    background: #ffffff;
    backdrop-filter: none;
  }

  .glow {
    display: none;
  }
}
</style>
