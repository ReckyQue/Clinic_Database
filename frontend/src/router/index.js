import { createRouter, createWebHistory } from 'vue-router';
import store from '@/store';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'patients',
        name: 'Patients',
        component: () => import('@/views/Patients.vue'),
        meta: { title: '患者管理' },
      },
      {
        path: 'patients/add',
        name: 'AddPatient',
        component: () => import('@/views/AddPatient.vue'),
        meta: { title: '添加患者', requiresAuth: true },
      },
      {
        path: 'patients/:id',
        name: 'PatientDetail',
        component: () => import('@/views/PatientDetail.vue'),
        meta: { title: '患者详情' },
      },
      {
        path: 'patients/edit/:id',
        name: 'EditPatient',
        component: () => import('@/views/EditPatient.vue'),
        meta: { title: '编辑患者', requiresAuth: true },
      },
      {
        path: 'diagnosis',
        name: 'Diagnosis',
        component: () => import('@/views/Diagnosis.vue'),
        meta: { title: '诊断记录', requiresAuth: true },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计分析' },
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理', requiresAdmin: true },
      },
      {
        path: 'users/add',
        name: 'AddUser',
        component: () => import('@/views/UserForm.vue'),
        meta: { title: '添加用户', requiresAdmin: true },
      },
      {
        path: 'users/:id',
        name: 'UserDetail',
        component: () => import('@/views/UserDetail.vue'),
        meta: { title: '用户详情', requiresAdmin: true },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', requiresAdmin: true },
      },
      {
        path: 'more',
        name: 'More',
        component: () => import('@/views/MobileMore.vue'),
        meta: { title: '设置', requiresAuth: true },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const isLoggedIn = store.getters.isLoggedIn;
  const isAdmin = store.getters.isAdmin;
  const isMember = store.getters.isMember;

  if (to.meta.requiresAdmin && !isAdmin) {
    next('/dashboard');
    return;
  }

  if (to.meta.requiresAuth && !(isAdmin || isMember)) {
    next('/login');
    return;
  }

  if (!isLoggedIn) {
    const allowedRoutes = ['Dashboard', 'Patients', 'PatientDetail', 'Statistics', 'Login'];
    if (!allowedRoutes.includes(to.name)) {
      next('/dashboard');
      return;
    }
  }

  next();
});

export default router;
