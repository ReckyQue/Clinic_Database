/** 用户展示与权限辅助 — 三角色：管理员 · 成员 · 游客 */

export const ROLE_ORDER = { ADMIN: 0, MEMBER: 1, GUEST: 2 };

export const ROLE_OPTIONS = [
  { label: '管理员', value: 'ADMIN' },
  { label: '成员', value: 'MEMBER' },
  { label: '游客', value: 'GUEST' },
];

export function roleLabel(role) {
  return (
    {
      ADMIN: '管理员',
      MEMBER: '成员',
      GUEST: '游客',
    }[role] || role
  );
}

export function roleTagClass(role) {
  return (
    {
      ADMIN: 'tag-admin',
      MEMBER: 'tag-member',
      GUEST: 'tag-guest',
    }[role] || 'tag-guest'
  );
}

export function statusLabel(status) {
  return status === 'ACTIVE' ? '正常' : '已禁用';
}

export function formatDateTime(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleString('zh-CN');
}

/** 系统内置管理员账号（不可删除/禁用） */
export function isSystemAdminAccount(user) {
  return user?.username === 'admin';
}

export function isCurrentUser(row, me) {
  if (!row || !me) return false;
  return row.id === me.id || row.username === me.username;
}

/** 排序：当前用户置顶 → 角色优先级 → 姓名 */
export function sortUsers(list, me) {
  const copy = [...(list || [])];
  copy.sort((a, b) => {
    const aMe = isCurrentUser(a, me) ? 0 : 1;
    const bMe = isCurrentUser(b, me) ? 0 : 1;
    if (aMe !== bMe) return aMe - bMe;
    const ra = ROLE_ORDER[a.role] ?? 9;
    const rb = ROLE_ORDER[b.role] ?? 9;
    if (ra !== rb) return ra - rb;
    return String(a.realName || a.username || '').localeCompare(
      String(b.realName || b.username || ''),
      'zh-CN'
    );
  });
  return copy;
}

/**
 * 管理员拥有全部用户管理权限。
 * 约束：不可操作自己的禁用/删除/改角色；内置 admin 账号不可删/禁。
 */
export function userActionPerms(me, target) {
  const amAdmin = me?.role === 'ADMIN';
  const isSelf = isCurrentUser(target, me);
  const isSeed = isSystemAdminAccount(target);

  return {
    canView: amAdmin,
    canEdit: amAdmin && !isSeed,
    canResetPassword: amAdmin && !isSelf,
    canToggleStatus: amAdmin && !isSelf && !isSeed,
    canDelete: amAdmin && !isSelf && !isSeed,
    canChangeRole: amAdmin && !isSelf && !isSeed,
    canAddAdmin: amAdmin,
  };
}
