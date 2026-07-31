<template>
  <el-container class="layout-container" :class="{ 'is-mobile': isMobile }">
    <!-- PC 侧栏：结构与样式保持原样 -->
    <el-aside :width="'var(--sidebar-width)'" class="aside aside--desktop">
      <div class="logo">
        <div class="logo-title">乡村慢性病管理系统</div>
      </div>
      <el-menu :default-active="activeMenu" router class="side-menu">
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="topbar" height="var(--topbar-height)">
        <div class="header-left" :class="{ 'has-back': isMobile && showBack }">
          <button
            v-if="isMobile && showBack"
            type="button"
            class="nav-back pressable"
            aria-label="返回"
            @click="goBack"
          >
            <el-icon :size="22"><ArrowLeft /></el-icon>
          </button>
          <!-- PC：始终显示 page-title；移动：显示短标题 -->
          <h4 class="page-title">{{ isMobile ? mobileTitle : currentTitle }}</h4>
        </div>
        <div class="header-right">
          <!-- 移动端右侧操作 -->
          <template v-if="isMobile">
            <button
              v-if="showSearch"
              type="button"
              class="nav-icon pressable"
              aria-label="搜索"
              @click="emitSearch"
            >
              <el-icon :size="22"><Search /></el-icon>
            </button>
            <button
              v-if="showAdd"
              type="button"
              class="nav-icon pressable"
              aria-label="添加"
              @click="emitAdd"
            >
              <el-icon :size="22"><Plus /></el-icon>
            </button>
          </template>
          <!-- PC：头像 + 退出（与改动前一致） -->
          <template v-else>
            <template v-if="isLoggedIn">
              <div class="avatar" aria-hidden="true">{{ avatarLetter }}</div>
              <span class="user-name">{{ user?.realName || user?.username }}</span>
              <el-button class="logout-btn pressable" text @click="handleLogout">退出</el-button>
            </template>
            <template v-else>
              <el-button type="primary" class="pressable" @click="$router.push('/login')"
                >登录</el-button
              >
            </template>
          </template>
        </div>
      </el-header>

      <el-main class="content" :class="{ 'has-tabbar': isMobile }">
        <router-view v-slot="{ Component }">
          <keep-alive include="Dashboard">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <!-- 仅移动端：液态玻璃底部 Tab -->
    <nav v-if="isMobile" class="tabbar" aria-label="主导航">
      <div class="tabbar-glass" :style="{ '--tab-count': mobileTabs.length }">
        <div class="tab-indicator" :style="indicatorStyle" aria-hidden="true" />
        <router-link
          v-for="(tab, index) in mobileTabs"
          :key="tab.path"
          :to="tab.path"
          class="tab-item"
          :class="{ active: isTabActive(tab) }"
          @click="onTabClick(index)"
        >
          <span class="tab-pill">
            <el-icon class="tab-icon" :size="isTabActive(tab) ? 28 : 26">
              <component :is="tab.icon" />
            </el-icon>
            <span class="tab-label">{{ tab.label }}</span>
          </span>
        </router-link>
      </div>
    </nav>
  </el-container>
</template>

<script setup>
import { computed, provide, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessageBox } from 'element-plus';
import {
  ArrowLeft,
  DataAnalysis,
  Document,
  House,
  Plus,
  Search,
  Setting,
  User,
  UserFilled,
} from '@element-plus/icons-vue';
import { useMobile } from '@/composables/useMobile';

const route = useRoute();
const router = useRouter();
const store = useStore();
const { isMobile } = useMobile();

const mobileActions = {
  openSearch: null,
  openAdd: null,
};
provide('mobileActions', mobileActions);

const activeMenu = computed(() => route.path);
const currentTitle = computed(() => route.meta?.title || '首页');
const isLoggedIn = computed(() => store.getters.isLoggedIn);
const isAdmin = computed(() => store.getters.isAdmin);
const isMember = computed(() => store.getters.isMember);
const user = computed(() => store.state.user);
const avatarLetter = computed(() => {
  const name = user.value?.realName || user.value?.username || '用';
  return String(name).slice(0, 1).toUpperCase();
});

const allMenus = [
  { path: '/dashboard', label: '首页', icon: House, roles: ['all'] },
  { path: '/patients', label: '患者管理', icon: User, roles: ['all'] },
  { path: '/statistics', label: '统计分析', icon: DataAnalysis, roles: ['all'] },
  { path: '/diagnosis', label: '诊断记录', icon: Document, roles: ['admin', 'member'] },
  { path: '/users', label: '用户管理', icon: UserFilled, roles: ['admin'] },
  { path: '/settings', label: '系统设置', icon: Setting, roles: ['admin'] },
];

const visibleMenus = computed(() => {
  if (!isLoggedIn.value) {
    return allMenus.filter((item) => item.roles.includes('all'));
  }
  return allMenus.filter((item) => {
    if (item.roles.includes('all')) return true;
    if (item.roles.includes('admin') && isAdmin.value) return true;
    if (item.roles.includes('member') && (isAdmin.value || isMember.value)) return true;
    return false;
  });
});

const mobileTabs = computed(() => {
  const tabs = [
    { path: '/dashboard', label: '首页', icon: House, match: ['/dashboard'] },
    { path: '/patients', label: '患者', icon: User, match: ['/patients'] },
    { path: '/statistics', label: '分析', icon: DataAnalysis, match: ['/statistics'] },
  ];
  if (isAdmin.value || isMember.value) {
    tabs.push({ path: '/diagnosis', label: '诊断', icon: Document, match: ['/diagnosis'] });
  }
  if (isLoggedIn.value) {
    tabs.push({
      path: '/more',
      label: '设置',
      icon: Setting,
      match: ['/more', '/users', '/settings'],
    });
  }
  return tabs;
});

const leafRoutes = new Set([
  'AddPatient',
  'EditPatient',
  'PatientDetail',
  'Users',
  'AddUser',
  'UserDetail',
  'Settings',
]);
const showBack = computed(() => leafRoutes.has(route.name));
const showSearch = computed(() => route.name === 'Patients');
const showAdd = computed(() => {
  if (route.name === 'Patients' && (isAdmin.value || isMember.value)) return true;
  if (route.name === 'Diagnosis' && (isAdmin.value || isMember.value)) return true;
  if (route.name === 'Users' && isAdmin.value) return true;
  return false;
});

const mobileTitle = computed(() => {
  if (route.name === 'Patients') return '患者';
  if (route.name === 'Statistics') return '统计分析';
  if (route.name === 'Diagnosis') return '诊断记录';
  if (route.name === 'More') return '设置';
  if (route.name === 'Dashboard') return '首页';
  if (route.name === 'Users') return '用户管理';
  if (route.name === 'AddUser') return '添加用户';
  if (route.name === 'UserDetail') return '用户详情';
  return currentTitle.value;
});

const isTabActive = (tab) =>
  tab.match.some((prefix) => route.path === prefix || route.path.startsWith(`${prefix}/`));

const activeTabIndex = computed(() => {
  const idx = mobileTabs.value.findIndex((tab) => isTabActive(tab));
  return idx < 0 ? 0 : idx;
});

const indicatorStyle = computed(() => {
  const n = mobileTabs.value.length || 1;
  const i = activeTabIndex.value;
  // 与 .tabbar-glass 左右 padding 6px 对齐：胶囊在第 i 个槽位正中
  return {
    left: `calc(6px + (100% - 12px) * ${(i + 0.5) / n} - 28px)`,
  };
});

const lastTabTap = ref({ index: -1, at: 0 });

const onTabClick = (index) => {
  try {
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(8);
    }
  } catch {
    // ignore
  }
  const now = Date.now();
  if (lastTabTap.value.index === index && now - lastTabTap.value.at < 400) {
    const main = document.querySelector('.content');
    main?.scrollTo({ top: 0, behavior: 'smooth' });
  }
  lastTabTap.value = { index, at: now };
};

const goBack = () => {
  if (route.name === 'Users') {
    router.push('/more');
    return;
  }
  if (route.name === 'AddUser' || route.name === 'UserDetail') {
    router.push('/users');
    return;
  }
  if (route.name === 'Settings') {
    router.push('/more');
    return;
  }
  if (window.history.length > 1) router.back();
  else router.push('/dashboard');
};

const emitSearch = () => {
  if (typeof mobileActions.openSearch === 'function') mobileActions.openSearch();
};

const emitAdd = () => {
  if (typeof mobileActions.openAdd === 'function') {
    mobileActions.openAdd();
    return;
  }
  if (route.name === 'Patients') router.push('/patients/add');
  if (route.name === 'Diagnosis') router.push({ path: '/diagnosis', query: { create: '1' } });
  if (route.name === 'Users') router.push('/users/add');
};

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
.layout-container {
  height: 100%;
  background: var(--color-canvas);
}

.aside {
  width: var(--sidebar-width) !important;
  background: var(--color-sidebar);
  border-right: 1px solid var(--color-line);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.logo {
  min-height: var(--topbar-height);
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-bottom: 1px solid var(--color-line);
}

.logo-title {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-ink);
  line-height: 1.25;
}

.side-menu {
  border-right: none !important;
  background: transparent !important;
  padding: 12px 10px;
  flex: 1;
}

.side-menu :deep(.el-menu-item) {
  height: auto;
  min-height: 44px;
  line-height: 1.3;
  margin: 2px 0;
  padding: 12px 16px !important;
  border-radius: 12px;
  color: var(--color-ink) !important;
  font-size: 15px;
  font-weight: 500;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.side-menu :deep(.el-menu-item .el-icon) {
  color: var(--color-muted);
  margin-right: 12px;
  width: 22px;
  font-size: 22px;
}

.side-menu :deep(.el-menu-item:hover) {
  background: var(--color-hover) !important;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--color-selected) !important;
  color: var(--color-primary) !important;
  font-weight: 650;
}

.side-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--color-primary);
}

.main-wrap {
  min-width: 0;
  flex: 1;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  height: var(--topbar-height) !important;
  background: #ffffff;
  border-bottom: 1px solid var(--color-line);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.4;
  color: var(--color-ink);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-selected);
  color: var(--color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 650;
}

.user-name {
  color: var(--color-ink);
  font-size: 15px;
  font-weight: 500;
}

.logout-btn {
  color: var(--color-muted) !important;
  font-size: 15px;
  min-height: 44px;
}

.content {
  background: var(--color-canvas);
  padding: 20px 24px;
}

.nav-back,
.nav-icon,
.tabbar {
  display: none;
}

/* ===== 仅 ≤430px：移动壳（不影响 PC） ===== */
@media (max-width: 430px) {
  .aside--desktop {
    display: none !important;
  }

  .topbar {
    height: 44px !important;
    /* 导航栏左右 16pt，与内容区边距一致 */
    padding-top: env(safe-area-inset-top);
    padding-bottom: 0;
    padding-left: max(16px, env(safe-area-inset-left));
    padding-right: max(16px, env(safe-area-inset-right));
    min-height: calc(44px + env(safe-area-inset-top));
    background: rgba(255, 255, 255, 0.88);
    backdrop-filter: blur(20px) saturate(160%);
  }

  .header-left {
    gap: 0;
    flex: 1;
    min-width: 0;
  }

  /* 有返回按钮：箭头与标题间距 8pt */
  .header-left.has-back {
    gap: 8px;
  }

  .page-title {
    font-size: 17px;
    font-weight: 600;
    padding-left: 0;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .content {
    padding: 12px 16px;
  }

  .content.has-tabbar {
    /* 悬浮 Tab：玻璃高度 + 底部悬空 8pt + 安全区 */
    padding-bottom: calc(12px + 66px + 8px + env(safe-area-inset-bottom));
  }

  .nav-back {
    display: inline-flex;
    width: 28px;
    min-width: 28px;
    height: 44px;
    margin: 0;
    padding: 0;
    border: none;
    background: transparent;
    color: #007aff;
    align-items: center;
    justify-content: flex-start;
    border-radius: 10px;
    cursor: pointer;
    flex-shrink: 0;
  }

  .nav-icon {
    display: inline-flex;
    width: 44px;
    height: 44px;
    border: none;
    background: transparent;
    color: #007aff;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    cursor: pointer;
    flex-shrink: 0;
  }

  .header-right {
    gap: 0;
    margin-right: -8px;
  }

  /* —— 液态玻璃悬浮 Tab Bar —— */
  .tabbar {
    display: block;
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: calc(8px + env(safe-area-inset-bottom));
    z-index: 50;
    height: auto;
    padding: 0;
    background: transparent;
    border: none;
    pointer-events: none;
  }

  .tabbar-glass {
    pointer-events: auto;
    position: relative;
    display: flex;
    align-items: center;
    height: 62px;
    padding: 0 6px;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(25px) saturate(180%);
    -webkit-backdrop-filter: blur(25px) saturate(180%);
    border: 0.5px solid rgba(0, 0, 0, 0.08);
    box-shadow:
      0 -2px 20px rgba(0, 0, 0, 0.05),
      0 8px 24px rgba(0, 0, 0, 0.06);
    overflow: hidden;
  }

  /* 选中胶囊：与 .tab-pill 同尺寸同位置 */
  .tab-indicator {
    position: absolute;
    top: 50%;
    width: 56px;
    height: 44px;
    margin-top: -22px;
    border-radius: 12px;
    background: rgba(0, 122, 255, 0.1);
    transition: left 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
    pointer-events: none;
    z-index: 0;
  }

  .tab-item {
    position: relative;
    z-index: 1;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 44px;
    min-height: 44px;
    text-decoration: none;
    color: #8e8e93;
    min-width: 0;
    -webkit-tap-highlight-color: transparent;
    user-select: none;
  }

  /* 图标+文字的固定容器：与蓝块 56×44 完全重合，内部居中 */
  .tab-pill {
    width: 56px;
    height: 44px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    box-sizing: border-box;
  }

  .tab-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: inherit;
    line-height: 1;
    transition:
      transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1),
      color 0.2s ease;
  }

  .tab-icon :deep(svg) {
    display: block;
  }

  .tab-label {
    display: block;
    width: 100%;
    text-align: center;
    font-size: 10px;
    font-weight: 400;
    line-height: 1.2;
    color: inherit;
    transition:
      color 0.2s ease,
      font-weight 0.2s ease;
  }

  .tab-item.active {
    color: #007aff;
  }

  .tab-item.active .tab-label {
    font-weight: 600;
  }

  .tab-item:active .tab-icon {
    transform: scale(0.85);
    transition: transform 0.08s ease;
  }

  .tabbar-glass:has(.tab-item:active) .tab-indicator {
    background: rgba(0, 122, 255, 0.18);
  }

  @media (prefers-reduced-motion: reduce) {
    .tab-indicator,
    .tab-icon,
    .tab-label {
      transition: none !important;
    }

    .tab-item:active .tab-icon {
      transform: none;
    }
  }

  @media (prefers-reduced-transparency: reduce) {
    .tabbar-glass {
      background: #fff;
      backdrop-filter: none;
      -webkit-backdrop-filter: none;
    }
  }
}
</style>
