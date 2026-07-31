<template>
  <div v-loading="loading" class="user-detail">
    <template v-if="user">
      <!-- 只读头 -->
      <section v-if="!editing" class="head-card">
        <div class="head-row">
          <span class="avatar">👤</span>
          <div class="head-main">
            <div class="name-row">
              <span class="name">{{ user.realName || user.username }}</span>
              <span v-if="isMe" class="me-tag">（我）</span>
              <span class="role" :class="roleTagClass(user.role)">
                {{ roleLabel(user.role) }}
              </span>
            </div>
          </div>
          <button v-if="perms.canEdit" type="button" class="edit-btn pressable" @click="startEdit">
            编辑
          </button>
        </div>
        <div class="divider" />
        <div class="info-row">
          <span>手机号</span><span>{{ user.phone || '—' }}</span>
        </div>
        <div class="info-row">
          <span>邮箱</span><span>{{ user.email || '—' }}</span>
        </div>
        <div class="info-row">
          <span>角色</span><span>{{ roleLabel(user.role) }}</span>
        </div>
        <div class="info-row">
          <span>状态</span>
          <span>
            <span class="dot" :class="user.status === 'ACTIVE' ? 'on' : 'off'" />
            {{ statusLabel(user.status) }}
          </span>
        </div>
        <div class="info-row">
          <span>创建时间</span><span>{{ formatDateTime(user.createTime) }}</span>
        </div>
        <div class="info-row">
          <span>最后登录</span><span>{{ formatDateTime(user.updateTime || user.createTime) }}</span>
        </div>
      </section>

      <!-- 编辑模式 -->
      <section v-else class="edit-block">
        <div class="group-title">基本信息</div>
        <div class="group-card">
          <label class="field">
            <span class="label">姓名</span>
            <input v-model.trim="form.realName" class="input" maxlength="20" />
          </label>
          <div class="line" />
          <label class="field">
            <span class="label">手机号</span>
            <input v-model.trim="form.phone" class="input" inputmode="numeric" maxlength="11" />
          </label>
          <div class="line" />
          <label class="field">
            <span class="label">邮箱</span>
            <input v-model.trim="form.email" class="input" />
          </label>
          <div class="line" />
          <div class="field">
            <span class="label">角色</span>
            <span class="locked">{{ roleLabel(user.role) }}（不可在此修改）</span>
          </div>
        </div>

        <div class="edit-actions">
          <button type="button" class="btn ghost pressable" @click="editing = false">取消</button>
          <button
            type="button"
            class="btn primary pressable"
            :disabled="submitting"
            @click="saveEdit"
          >
            {{ submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </section>

      <!-- 操作区 -->
      <section v-if="!editing && hasAnyAction" class="ops">
        <div class="group-title">操作</div>
        <button
          v-if="perms.canResetPassword"
          type="button"
          class="op-btn blue pressable"
          @click="pwdVisible = true"
        >
          重置密码
        </button>
        <button
          v-if="perms.canToggleStatus"
          type="button"
          class="op-btn orange pressable"
          @click="toggleStatus"
        >
          {{ user.status === 'ACTIVE' ? '禁用账号' : '启用账号' }}
        </button>
        <button
          v-if="perms.canDelete"
          type="button"
          class="op-btn red pressable"
          @click="removeUser"
        >
          删除用户
        </button>
      </section>
    </template>

    <div v-if="pwdVisible" class="sheet-mask" @click="pwdVisible = false">
      <div class="pwd-panel" @click.stop>
        <div class="pwd-title">重置密码</div>
        <el-input
          v-model="pwdForm.password"
          type="password"
          show-password
          placeholder="请输入新密码（至少6位）"
        />
        <el-input
          v-model="pwdForm.confirm"
          type="password"
          show-password
          placeholder="请再次输入密码"
          style="margin-top: 10px"
        />
        <button
          type="button"
          class="pwd-submit pressable"
          :disabled="pwdSubmitting || !pwdValid"
          @click="confirmResetPwd"
        >
          {{ pwdSubmitting ? '处理中…' : '确认重置' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUserById, updateUser, updateUserStatus, deleteUser } from '@/api/user';
import {
  formatDateTime,
  isCurrentUser,
  roleLabel,
  roleTagClass,
  statusLabel,
  userActionPerms,
} from '@/utils/userDisplay';

const route = useRoute();
const router = useRouter();
const store = useStore();

const loading = ref(false);
const submitting = ref(false);
const editing = ref(false);
const user = ref(null);
const pwdVisible = ref(false);
const pwdSubmitting = ref(false);
const pwdForm = reactive({ password: '', confirm: '' });
const form = reactive({ realName: '', phone: '', email: '' });

const me = computed(() => store.state.user);
const isMe = computed(() => isCurrentUser(user.value, me.value));
const perms = computed(() => userActionPerms(me.value, user.value));
const hasAnyAction = computed(
  () => perms.value.canResetPassword || perms.value.canToggleStatus || perms.value.canDelete
);
const pwdValid = computed(
  () => pwdForm.password.length >= 6 && pwdForm.password === pwdForm.confirm
);

const load = async () => {
  loading.value = true;
  try {
    const res = await getUserById(route.params.id);
    user.value = res.data;
    if (route.query.edit === '1' && userActionPerms(me.value, user.value).canEdit) {
      startEdit();
    }
  } catch (error) {
    console.error('加载用户失败:', error);
    ElMessage.error('用户不存在');
    router.replace('/users');
  } finally {
    loading.value = false;
  }
};

const startEdit = () => {
  form.realName = user.value.realName || '';
  form.phone = user.value.phone || '';
  form.email = user.value.email || '';
  editing.value = true;
};

const saveEdit = async () => {
  if (!form.realName || form.realName.length < 2) {
    ElMessage.warning('请输入有效姓名');
    return;
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('手机号格式不正确');
    return;
  }
  submitting.value = true;
  try {
    await updateUser(user.value.id, {
      username: user.value.username,
      realName: form.realName,
      role: user.value.role,
      phone: form.phone,
      email: form.email,
      status: user.value.status,
    });
    ElMessage.success('保存成功');
    editing.value = false;
    await load();
  } catch (error) {
    console.error('保存失败:', error);
  } finally {
    submitting.value = false;
  }
};

const toggleStatus = async () => {
  const next = user.value.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  const text = next === 'ACTIVE' ? '启用' : '禁用';
  try {
    await ElMessageBox.confirm(`确定要${text}该账号吗？`, '提示', { type: 'warning' });
    await updateUserStatus(user.value.id, next);
    ElMessage.success(`${text}成功`);
    await load();
  } catch (error) {
    if (error !== 'cancel') console.error(error);
  }
};

const removeUser = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 ${user.value.realName || user.value.username} 吗？此操作不可恢复。`,
      '提示',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    );
    await deleteUser(user.value.id);
    ElMessage.success('删除成功');
    router.replace('/users');
  } catch (error) {
    if (error !== 'cancel') console.error(error);
  }
};

const confirmResetPwd = async () => {
  if (!pwdValid.value) return;
  pwdSubmitting.value = true;
  try {
    await updateUser(user.value.id, {
      username: user.value.username,
      realName: user.value.realName,
      role: user.value.role,
      phone: user.value.phone,
      email: user.value.email,
      password: pwdForm.password,
    });
    ElMessage.success('密码已重置');
    pwdVisible.value = false;
    pwdForm.password = '';
    pwdForm.confirm = '';
  } catch (error) {
    console.error(error);
  } finally {
    pwdSubmitting.value = false;
  }
};

onMounted(load);
</script>

<style scoped>
.user-detail {
  padding-bottom: 24px;
}

.head-card,
.group-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
}

.head-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.avatar {
  font-size: 28px;
  line-height: 1;
}

.head-main {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.name {
  font-size: 17px;
  font-weight: 700;
  color: #1c1c1e;
}

.me-tag {
  font-size: 13px;
  color: #007aff;
}

.role {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  border-radius: 12px;
  padding: 4px 10px;
}

.tag-admin {
  background: #007aff;
}
.tag-member {
  background: #34c759;
}
.tag-guest {
  background: #8e8e93;
}

.edit-btn {
  border: none;
  background: transparent;
  color: #007aff;
  font-size: 15px;
  min-height: 36px;
  cursor: pointer;
}

.divider {
  height: 0.5px;
  background: #e5e5ea;
  margin: 12px 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  align-items: center;
  font-size: 15px;
  color: #1c1c1e;
}

.info-row span:first-child {
  color: #8e8e93;
  flex-shrink: 0;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.dot.on {
  background: #34c759;
}
.dot.off {
  background: #ff3b30;
}

.group-title {
  font-size: 13px;
  color: #8e8e93;
  padding: 0 4px 8px;
  margin-top: 18px;
}

.edit-block .group-title {
  margin-top: 0;
}

.field {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 48px;
  padding: 8px 16px;
}

.label {
  width: 64px;
  font-size: 17px;
  color: #1c1c1e;
}

.input {
  flex: 1;
  border: none;
  outline: none;
  text-align: right;
  font-size: 17px;
  background: transparent;
}

.locked {
  flex: 1;
  text-align: right;
  font-size: 13px;
  color: #8e8e93;
}

.line {
  height: 0.5px;
  background: #e5e5ea;
  margin-left: 16px;
}

.edit-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.btn {
  flex: 1;
  min-height: 48px;
  border: none;
  border-radius: 12px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
}

.btn.ghost {
  background: #fff;
  color: #1c1c1e;
}

.btn.primary {
  background: #007aff;
  color: #fff;
}

.ops {
  margin-top: 8px;
}

.op-btn {
  width: 100%;
  min-height: 48px;
  margin-bottom: 10px;
  border: none;
  border-radius: 12px;
  background: #fff;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
}

.op-btn.blue {
  color: #007aff;
}
.op-btn.orange {
  color: #ff9500;
}
.op-btn.red {
  color: #ff3b30;
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

.pwd-panel {
  width: 100%;
  max-width: 430px;
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
}

.pwd-title {
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 14px;
}

.pwd-submit {
  width: 100%;
  margin-top: 16px;
  min-height: 48px;
  border: none;
  border-radius: 12px;
  background: #007aff;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
}

.pwd-submit:disabled {
  opacity: 0.4;
}
</style>
