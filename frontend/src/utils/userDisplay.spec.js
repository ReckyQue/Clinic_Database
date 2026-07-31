import { describe, expect, it } from 'vitest';
import {
  formatDateTime,
  isSystemAdminAccount,
  roleLabel,
  sortUsers,
  statusLabel,
  userActionPerms,
} from '@/utils/userDisplay';

describe('userDisplay', () => {
  it('roleLabel maps known roles', () => {
    expect(roleLabel('ADMIN')).toBe('管理员');
    expect(roleLabel('MEMBER')).toBe('成员');
    expect(roleLabel('GUEST')).toBe('游客');
  });

  it('statusLabel maps active/inactive', () => {
    expect(statusLabel('ACTIVE')).toBe('正常');
    expect(statusLabel('INACTIVE')).toBe('已禁用');
  });

  it('isSystemAdminAccount only matches seed admin', () => {
    expect(isSystemAdminAccount({ username: 'admin' })).toBe(true);
    expect(isSystemAdminAccount({ username: 'member' })).toBe(false);
  });

  it('formatDateTime returns dash for empty', () => {
    expect(formatDateTime('')).toBe('—');
    expect(formatDateTime(null)).toBe('—');
  });

  it('sortUsers puts current user first then by role', () => {
    const me = { id: 2, username: 'me', role: 'MEMBER' };
    const list = [
      { id: 1, username: 'a', role: 'GUEST', realName: '张三' },
      { id: 2, username: 'me', role: 'MEMBER', realName: '李四' },
      { id: 3, username: 'b', role: 'ADMIN', realName: '王五' },
    ];
    const sorted = sortUsers(list, me);
    expect(sorted[0].username).toBe('me');
    expect(sorted[1].role).toBe('ADMIN');
  });

  it('userActionPerms protects self and seed admin', () => {
    const admin = { id: 1, username: 'boss', role: 'ADMIN' };
    const seed = { id: 9, username: 'admin', role: 'ADMIN' };
    const self = { id: 1, username: 'boss', role: 'ADMIN' };
    expect(userActionPerms(admin, seed).canDelete).toBe(false);
    expect(userActionPerms(admin, self).canToggleStatus).toBe(false);
    expect(userActionPerms(admin, { id: 3, username: 'x', role: 'MEMBER' }).canDelete).toBe(true);
  });
});
