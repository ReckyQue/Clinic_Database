<template>
  <div class="user-form-page">
    <section class="group">
      <div class="group-title">基本信息</div>
      <div class="group-card">
        <label class="field">
          <span class="label">姓名</span>
          <input
            v-model.trim="form.realName"
            class="input"
            maxlength="20"
            placeholder="请输入姓名"
          />
          <span v-if="errors.realName" class="err">❗</span>
        </label>
        <div class="line" />
        <label class="field">
          <span class="label">手机号</span>
          <input
            v-model.trim="form.phone"
            class="input"
            inputmode="numeric"
            maxlength="11"
            placeholder="请输入手机号"
          />
          <span v-if="errors.phone" class="err">❗</span>
        </label>
        <div class="line" />
        <label class="field">
          <span class="label">邮箱</span>
          <input v-model.trim="form.email" class="input" placeholder="请输入邮箱（选填）" />
          <span v-if="errors.email" class="err">❗</span>
        </label>
        <div class="line" />
        <label class="field">
          <span class="label">用户名</span>
          <input v-model.trim="form.username" class="input" placeholder="登录账号" />
          <span v-if="errors.username" class="err">❗</span>
        </label>
      </div>
    </section>

    <section class="group">
      <div class="group-title">账号设置</div>
      <div class="group-card">
        <button type="button" class="field pressable" @click="roleSheet = true">
          <span class="label">角色</span>
          <span class="value" :class="{ placeholder: !form.role }">
            {{ roleDisplay || '请选择角色' }}
          </span>
          <span class="chev">▼</span>
        </button>
        <div class="line" />
        <label class="field">
          <span class="label">初始密码</span>
          <input
            v-model="form.password"
            class="input"
            type="password"
            placeholder="请输入初始密码"
          />
          <span v-if="errors.password" class="err">❗</span>
        </label>
        <div class="line" />
        <label class="field">
          <span class="label">确认密码</span>
          <input
            v-model="form.confirm"
            class="input"
            type="password"
            placeholder="请再次输入密码"
          />
          <span v-if="errors.confirm" class="err">❗</span>
        </label>
      </div>
    </section>

    <section class="group">
      <div class="group-title">状态设置</div>
      <div class="group-card">
        <div class="field">
          <span class="label">账号状态</span>
          <div class="radios">
            <label class="radio">
              <input v-model="form.status" type="radio" value="ACTIVE" />
              <span>启用</span>
            </label>
            <label class="radio">
              <input v-model="form.status" type="radio" value="INACTIVE" />
              <span>禁用</span>
            </label>
          </div>
        </div>
      </div>
    </section>

    <button
      type="button"
      class="save-btn pressable"
      :disabled="!canSave || submitting"
      @click="handleSave"
    >
      {{ submitting ? '保存中…' : '保 存' }}
    </button>

    <div v-if="roleSheet" class="sheet-mask" @click="roleSheet = false">
      <div class="sheet" @click.stop>
        <button
          v-for="opt in roleOptions"
          :key="opt.value"
          type="button"
          class="sheet-item"
          @click="pickRole(opt.value)"
        >
          {{ opt.label }}
        </button>
        <button type="button" class="sheet-cancel" @click="roleSheet = false">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { createUser, updateUserStatus } from '@/api/user';
import { ROLE_OPTIONS, roleLabel } from '@/utils/userDisplay';

const router = useRouter();
const submitting = ref(false);
const roleSheet = ref(false);

const form = reactive({
  realName: '',
  phone: '',
  email: '',
  username: '',
  role: 'MEMBER',
  password: '',
  confirm: '',
  status: 'ACTIVE',
});

const roleOptions = ROLE_OPTIONS;

const roleDisplay = computed(() => {
  if (!form.role) return '';
  return roleLabel(form.role);
});

const phoneOk = computed(() => /^1\d{10}$/.test(form.phone));
const emailOk = computed(() => !form.email || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email));
const nameOk = computed(() => form.realName.length >= 2 && form.realName.length <= 20);
const userOk = computed(() => form.username.length >= 2);
const pwdOk = computed(() => form.password.length >= 6);
const confirmOk = computed(() => form.password === form.confirm && form.confirm.length >= 6);

const errors = computed(() => ({
  realName: form.realName && !nameOk.value,
  phone: form.phone && !phoneOk.value,
  email: form.email && !emailOk.value,
  username: form.username && !userOk.value,
  password: form.password && !pwdOk.value,
  confirm: form.confirm && !confirmOk.value,
}));

const canSave = computed(
  () =>
    nameOk.value &&
    phoneOk.value &&
    emailOk.value &&
    userOk.value &&
    !!form.role &&
    pwdOk.value &&
    confirmOk.value
);

const pickRole = (value) => {
  form.role = value;
  roleSheet.value = false;
};

const handleSave = async () => {
  if (!canSave.value) return;
  submitting.value = true;
  try {
    const res = await createUser({
      username: form.username,
      password: form.password,
      realName: form.realName,
      role: form.role,
      phone: form.phone,
      email: form.email || null,
      status: form.status,
    });
    const newId = res.data?.id;
    if (newId && form.status === 'INACTIVE') {
      await updateUserStatus(newId, 'INACTIVE');
    }
    ElMessage.success('添加成功');
    router.push('/users');
  } catch (error) {
    console.error('添加用户失败:', error);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.user-form-page {
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
}

.field {
  width: 100%;
  min-height: 48px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: none;
  background: transparent;
  text-align: left;
}

.label {
  width: 72px;
  flex-shrink: 0;
  font-size: 17px;
  color: #1c1c1e;
}

.input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  font-size: 17px;
  color: #1c1c1e;
  background: transparent;
  text-align: right;
}

.input::placeholder {
  color: #c7c7cc;
}

.value {
  flex: 1;
  text-align: right;
  font-size: 17px;
  color: #1c1c1e;
}

.value.placeholder {
  color: #c7c7cc;
}

.chev {
  color: #c7c7cc;
  font-size: 12px;
}

.err {
  color: #ff3b30;
  flex-shrink: 0;
}

.line {
  height: 0.5px;
  background: #e5e5ea;
  margin-left: 16px;
}

.radios {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #1c1c1e;
}

.save-btn {
  width: 100%;
  min-height: 50px;
  border: none;
  border-radius: 12px;
  background: #007aff;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
}

.save-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.sheet {
  width: 100%;
  max-width: 430px;
  padding: 8px 12px calc(12px + env(safe-area-inset-bottom));
}

.sheet-item,
.sheet-cancel {
  width: 100%;
  min-height: 52px;
  border: none;
  background: #fff;
  font-size: 17px;
  cursor: pointer;
}

.sheet-item {
  border-bottom: 0.5px solid #e5e5ea;
}

.sheet-item:first-child {
  border-radius: 12px 12px 0 0;
}

.sheet-item:last-of-type {
  border-bottom: none;
  border-radius: 0 0 12px 12px;
  margin-bottom: 8px;
}

.sheet-cancel {
  border-radius: 12px;
  font-weight: 600;
}
</style>
