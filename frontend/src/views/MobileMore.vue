<template>
  <div class="more-page">
    <section class="group">
      <div class="group-title">账户</div>
      <div class="group-card">
        <div class="row">
          <div class="row-left">
            <div class="avatar">{{ avatarLetter }}</div>
            <div>
              <div class="row-title">{{ user?.realName || user?.username }}</div>
              <div class="row-sub">{{ roleLabel }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="isAdmin" class="group">
      <div class="group-title">用户与系统</div>
      <div class="group-card">
        <button type="button" class="row pressable" @click="$router.push('/users')">
          <span class="row-left-text">
            <span class="row-icon">👤</span>
            <span class="row-title">用户管理</span>
          </span>
          <el-icon color="#C7C7CC"><ArrowRight /></el-icon>
        </button>
        <div class="divider" />
        <button type="button" class="row pressable" @click="$router.push('/settings')">
          <span class="row-title">系统设置</span>
          <el-icon color="#C7C7CC"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section v-else-if="isMember" class="group">
      <div class="group-title">工作</div>
      <div class="group-card">
        <button type="button" class="row pressable" @click="$router.push('/patients')">
          <span class="row-title">患者管理</span>
          <el-icon color="#C7C7CC"><ArrowRight /></el-icon>
        </button>
        <div class="divider" />
        <button type="button" class="row pressable" @click="$router.push('/diagnosis')">
          <span class="row-title">诊断记录</span>
          <el-icon color="#C7C7CC"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="group">
      <div class="group-title">关于</div>
      <div class="group-card">
        <div class="row">
          <span class="row-title">系统名称</span>
          <span class="row-value">乡村慢性病管理系统</span>
        </div>
      </div>
    </section>

    <button type="button" class="logout pressable" @click="handleLogout">退出登录</button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessageBox } from 'element-plus';
import { ArrowRight } from '@element-plus/icons-vue';

const store = useStore();
const router = useRouter();
const user = computed(() => store.state.user);
const isAdmin = computed(() => store.getters.isAdmin);
const isMember = computed(() => store.getters.isMember);
const avatarLetter = computed(() =>
  String(user.value?.realName || user.value?.username || '用').slice(0, 1)
);
const roleLabel = computed(() => {
  if (isAdmin.value) return '管理员';
  if (isMember.value) return '成员';
  return '游客';
});

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    store.dispatch('logout');
    router.push('/login');
  } catch {
    // cancelled
  }
};
</script>

<style scoped>
.more-page {
  padding-bottom: 24px;
}

.group {
  margin-bottom: 22px;
}

.group-title {
  font-size: 13px;
  color: #8e8e93;
  padding: 0 4px 8px;
}

.group-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.row {
  width: 100%;
  min-height: 44px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.row-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.row-left-text {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.row-icon {
  font-size: 18px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e8f2ff;
  color: #007aff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.row-title {
  font-size: 17px;
  color: #1c1c1e;
  font-weight: 500;
}

.row-sub,
.row-value {
  font-size: 15px;
  color: #8e8e93;
}

.divider {
  height: 0.5px;
  background: #e5e5ea;
  margin-left: 16px;
}

.logout {
  width: 100%;
  min-height: 50px;
  border: none;
  border-radius: 12px;
  background: #fff;
  color: #ff3b30;
  font-size: 17px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}
</style>
