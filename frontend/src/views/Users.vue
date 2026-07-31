<template>
  <div class="users" :class="{ 'is-mobile': isMobile }">
    <!-- ========== PC：原表格（不变） ========== -->
    <el-card v-if="!isMobile" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon> 添加用户
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="searchForm.role"
            placeholder="全部角色"
            clearable
            style="width: 140px"
          >
            <el-option label="管理员" value="ADMIN" />
            <el-option label="成员" value="MEMBER" />
            <el-option label="游客" value="GUEST" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
          >
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="users" style="width: 100%">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            {{ pcRoleLabel(row.role) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button
              type="warning"
              link
              :disabled="row.username === 'admin'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button
              type="danger"
              link
              :disabled="row.username === 'admin'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="fetchUsers"
        @current-change="fetchUsers"
      />
    </el-card>

    <!-- ========== 移动端：卡片列表 ========== -->
    <div v-else v-loading="loading" class="m-users">
      <div v-if="!loading && sortedUsers.length <= 1 && onlyMe" class="m-empty">
        <div class="empty-icon">👥</div>
        <div class="empty-title">暂无其他用户</div>
        <div class="empty-desc">点击下方按钮添加用户</div>
        <button type="button" class="empty-btn pressable" @click="$router.push('/users/add')">
          ＋ 添加用户
        </button>
        <div class="empty-me">
          （当前登录：{{ me?.realName || me?.username }} · {{ roleLabel(me?.role) }}）
        </div>
      </div>

      <template v-else>
        <article
          v-for="row in sortedUsers"
          :key="row.id"
          class="user-card pressable"
          :class="{ disabled: row.status !== 'ACTIVE' }"
          @click="$router.push(`/users/${row.id}`)"
        >
          <div class="uc-top">
            <div class="uc-name-row">
              <span class="uc-avatar">👤</span>
              <span class="uc-name">
                {{ row.realName || row.username }}
                <span v-if="isMe(row)" class="uc-me">（我）</span>
              </span>
              <span class="uc-role" :class="roleTagClass(row.role)">
                {{ roleLabel(row.role) }}
              </span>
            </div>
          </div>
          <div v-if="row.email" class="uc-line">📧 {{ row.email }}</div>
          <div v-if="row.phone" class="uc-line">📱 {{ row.phone }}</div>
          <div class="uc-sub">最后登录：{{ formatDateTime(row.updateTime || row.createTime) }}</div>
          <div class="uc-footer" @click.stop>
            <span class="uc-status">
              <span class="dot" :class="row.status === 'ACTIVE' ? 'on' : 'off'" />
              {{ statusLabel(row.status) }}
            </span>
            <div class="uc-actions">
              <button
                v-if="perms(row).canEdit"
                type="button"
                class="link-btn"
                @click="$router.push({ path: `/users/${row.id}`, query: { edit: '1' } })"
              >
                编辑
              </button>
              <button type="button" class="more-btn" aria-label="更多" @click="openSheet(row)">
                ···
              </button>
            </div>
          </div>
        </article>

        <el-pagination
          v-if="total > searchForm.size"
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :total="total"
          layout="prev, pager, next"
          class="m-pager"
          @current-change="fetchUsers"
        />
      </template>
    </div>

    <!-- 更多操作 Action Sheet -->
    <div v-if="sheetVisible" class="sheet-mask" @click="sheetVisible = false">
      <div class="sheet" @click.stop>
        <button v-if="sheetPerms.canEdit" type="button" class="sheet-item" @click="sheetGoEdit">
          ✏️ 编辑用户
        </button>
        <button
          v-if="sheetPerms.canResetPassword"
          type="button"
          class="sheet-item"
          @click="sheetResetPwd"
        >
          🔑 重置密码
        </button>
        <button
          v-if="sheetPerms.canToggleStatus"
          type="button"
          class="sheet-item"
          @click="sheetToggle"
        >
          {{ sheetRow?.status === 'ACTIVE' ? '🚫 禁用账号' : '✅ 启用账号' }}
        </button>
        <button
          v-if="sheetPerms.canDelete"
          type="button"
          class="sheet-item danger"
          @click="sheetDelete"
        >
          🗑️ 删除用户
        </button>
        <button type="button" class="sheet-cancel" @click="sheetVisible = false">取消</button>
      </div>
    </div>

    <!-- 重置密码面板 -->
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

    <!-- PC 弹窗 -->
    <el-dialog
      v-if="!isMobile"
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item :label="isEdit ? '新密码' : '密码'" :prop="isEdit ? '' : 'password'">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="成员" value="MEMBER" />
            <el-option label="游客" value="GUEST" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { getUsers, createUser, updateUser, deleteUser, updateUserStatus } from '@/api/user';
import { useMobile } from '@/composables/useMobile';
import {
  formatDateTime,
  isCurrentUser,
  roleLabel,
  roleTagClass,
  sortUsers,
  statusLabel,
  userActionPerms,
} from '@/utils/userDisplay';

const router = useRouter();
const store = useStore();
const { isMobile } = useMobile();
const mobileActions = inject('mobileActions', null);

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const users = ref([]);
const total = ref(0);
const formRef = ref(null);

const sheetVisible = ref(false);
const sheetRow = ref(null);
const pwdVisible = ref(false);
const pwdSubmitting = ref(false);
const pwdForm = reactive({ password: '', confirm: '' });

const me = computed(() => store.state.user);
const sortedUsers = computed(() => sortUsers(users.value, me.value));
const onlyMe = computed(() => {
  if (!users.value.length) return true;
  return users.value.every((u) => isCurrentUser(u, me.value));
});

const searchForm = reactive({
  page: 1,
  size: 20,
  username: '',
  realName: '',
  role: '',
  status: '',
});

const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  role: 'MEMBER',
  phone: '',
  email: '',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
};

const pcRoleLabel = (role) => ({ ADMIN: '管理员', MEMBER: '成员', GUEST: '游客' })[role] || role;

const isMe = (row) => isCurrentUser(row, me.value);
const perms = (row) => userActionPerms(me.value, row);
const sheetPerms = computed(() => userActionPerms(me.value, sheetRow.value));
const pwdValid = computed(
  () => pwdForm.password.length >= 6 && pwdForm.password === pwdForm.confirm
);

const resetForm = () => {
  form.id = null;
  form.username = '';
  form.password = '';
  form.realName = '';
  form.role = 'MEMBER';
  form.phone = '';
  form.email = '';
};

const fetchUsers = async () => {
  loading.value = true;
  try {
    const res = await getUsers(searchForm);
    users.value = res.data.records;
    total.value = res.data.total;
  } catch (error) {
    console.error('获取用户列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  searchForm.page = 1;
  fetchUsers();
};

const handleReset = () => {
  searchForm.username = '';
  searchForm.realName = '';
  searchForm.role = '';
  searchForm.status = '';
  handleSearch();
};

const openDialog = (row) => {
  resetForm();
  isEdit.value = !!row;
  if (row) {
    form.id = row.id;
    form.username = row.username;
    form.realName = row.realName;
    form.role = row.role;
    form.phone = row.phone || '';
    form.email = row.email || '';
  }
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const payload = {
        username: form.username,
        realName: form.realName,
        role: form.role,
        phone: form.phone,
        email: form.email,
        status: 'ACTIVE',
      };
      if (form.password) payload.password = form.password;
      if (isEdit.value) {
        await updateUser(form.id, payload);
        ElMessage.success('用户更新成功');
      } else {
        await createUser(payload);
        ElMessage.success('用户创建成功');
      }
      dialogVisible.value = false;
      fetchUsers();
    } catch (error) {
      console.error('保存用户失败:', error);
    } finally {
      submitting.value = false;
    }
  });
};

const toggleStatus = async (row) => {
  const nextStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  const actionText = nextStatus === 'ACTIVE' ? '启用' : '禁用';
  try {
    await ElMessageBox.confirm(`确定要${actionText}用户 ${row.username} 吗？`, '提示', {
      type: 'warning',
    });
    await updateUserStatus(row.id, nextStatus);
    ElMessage.success(`${actionText}成功`);
    fetchUsers();
  } catch (error) {
    if (error !== 'cancel') console.error('更新用户状态失败:', error);
  }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 ${row.realName || row.username} 吗？此操作不可恢复。`,
      '提示',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    );
    await deleteUser(row.id);
    ElMessage.success('删除成功');
    fetchUsers();
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error);
  }
};

const openSheet = (row) => {
  sheetRow.value = row;
  sheetVisible.value = true;
};

const sheetGoEdit = () => {
  const id = sheetRow.value?.id;
  sheetVisible.value = false;
  if (id) router.push({ path: `/users/${id}`, query: { edit: '1' } });
};

const sheetToggle = async () => {
  const row = sheetRow.value;
  sheetVisible.value = false;
  if (row) await toggleStatus(row);
};

const sheetDelete = async () => {
  const row = sheetRow.value;
  sheetVisible.value = false;
  if (row) await handleDelete(row);
};

const sheetResetPwd = () => {
  pwdForm.password = '';
  pwdForm.confirm = '';
  sheetVisible.value = false;
  pwdVisible.value = true;
};

const confirmResetPwd = async () => {
  if (!pwdValid.value || !sheetRow.value) return;
  pwdSubmitting.value = true;
  try {
    await updateUser(sheetRow.value.id, {
      username: sheetRow.value.username,
      realName: sheetRow.value.realName,
      role: sheetRow.value.role,
      phone: sheetRow.value.phone,
      email: sheetRow.value.email,
      password: pwdForm.password,
    });
    ElMessage.success('密码已重置');
    pwdVisible.value = false;
  } catch (error) {
    console.error('重置密码失败:', error);
  } finally {
    pwdSubmitting.value = false;
  }
};

onMounted(() => {
  if (mobileActions) {
    mobileActions.openAdd = () => router.push('/users/add');
  }
  fetchUsers();
});

onBeforeUnmount(() => {
  if (mobileActions) mobileActions.openAdd = null;
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.m-users {
  min-height: 160px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.user-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  min-height: 90px;
  cursor: pointer;
}

.user-card.disabled {
  opacity: 0.5;
}

.uc-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.uc-avatar {
  font-size: 16px;
}

.uc-name {
  font-size: 17px;
  font-weight: 700;
  color: #1c1c1e;
}

.uc-me {
  font-size: 13px;
  font-weight: 500;
  color: #007aff;
}

.uc-role {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  border-radius: 12px;
  padding: 4px 10px;
  line-height: 1.2;
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

.uc-line {
  font-size: 15px;
  color: #8e8e93;
  margin-bottom: 2px;
}

.uc-sub {
  font-size: 13px;
  color: #aeaeb2;
  margin: 4px 0 8px;
}

.uc-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.uc-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8e8e93;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot.on {
  background: #34c759;
}
.dot.off {
  background: #ff3b30;
}

.uc-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.link-btn,
.more-btn {
  border: none;
  background: transparent;
  color: #007aff;
  font-size: 15px;
  min-height: 36px;
  padding: 0 8px;
  cursor: pointer;
}

.more-btn {
  font-size: 20px;
  letter-spacing: 1px;
  color: #8e8e93;
  min-width: 44px;
}

.m-pager {
  justify-content: center;
  margin-top: 8px;
}

.m-empty {
  text-align: center;
  padding: 48px 16px 24px;
  background: #fff;
  border-radius: 12px;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 17px;
  font-weight: 650;
  color: #1c1c1e;
}

.empty-desc {
  margin-top: 6px;
  font-size: 15px;
  color: #8e8e93;
}

.empty-btn {
  margin-top: 20px;
  min-height: 44px;
  padding: 0 28px;
  border: none;
  border-radius: 12px;
  background: #007aff;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}

.empty-me {
  margin-top: 16px;
  font-size: 13px;
  color: #aeaeb2;
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

.sheet-item.danger {
  color: #ff3b30;
}

.sheet-cancel {
  border-radius: 12px;
  font-weight: 600;
}

.pwd-panel {
  width: 100%;
  max-width: 430px;
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
}

.pwd-title {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 14px;
  text-align: center;
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
  cursor: not-allowed;
}
</style>
