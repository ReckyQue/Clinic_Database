<template>
  <div v-loading="loading" class="dashboard" :data-ready="ready ? 'true' : 'false'">
    <el-row :gutter="12" class="metric-row">
      <el-col v-for="(card, index) in metricCards" :key="card.key" :xs="12" :sm="12" :lg="6">
        <div
          class="metric-card pressable"
          :class="[card.tone, { clickable: card.clickable, pulse: card.pulse }]"
          :style="{ '--stagger': `${index * 45}ms` }"
          @click="card.onClick?.()"
        >
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value" :class="{ 'danger-text': card.dangerValue }">
            {{ card.value }}
          </div>
          <div class="metric-meta" :class="card.metaClass">{{ card.meta }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="dual-panels">
      <el-card shadow="never" class="panel-card panel-card--chart enter-panel">
        <template #header>
          <div class="panel-header">
            <span class="panel-title">近一周就诊趋势</span>
            <el-button link type="primary" class="pressable" @click="$router.push('/statistics')">
              查看全部
            </el-button>
          </div>
        </template>
        <div class="chart-body">
          <div ref="trendChartRef" class="mini-chart"></div>
          <div class="mini-disease">
            <div class="mini-disease-title">疾病分布</div>
            <div ref="pieChartRef" class="mini-pie"></div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card panel-card--todo enter-panel enter-panel-delay">
        <template #header>
          <div class="panel-header">
            <span class="panel-title">今日需关注的患者</span>
            <el-tag size="small" type="info" effect="plain">按优先级</el-tag>
          </div>
        </template>
        <div class="todo-scroll">
          <div v-if="!attentionTodos.length" class="empty-todo">暂无待办，状态良好</div>
          <div v-else class="todo-list">
            <div
              v-for="(item, index) in attentionTodos"
              :key="`${item.type}-${item.patientId}`"
              class="todo-item pressable"
              :class="item.type"
              :style="{ '--stagger': `${index * 40}ms` }"
              role="button"
              tabindex="0"
              @click="handleTodoClick(item)"
              @keydown.enter="handleTodoClick(item)"
            >
              <div class="todo-main">
                <span class="todo-dot" aria-hidden="true" />
                <div class="todo-text">
                  <div class="todo-name">{{ item.patientName }}</div>
                  <div class="todo-reason">{{ item.reason }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="recent-card enter-panel enter-panel-late">
      <template #header>
        <div class="panel-header">
          <span class="panel-title">最近患者</span>
          <el-button link type="primary" class="pressable" @click="$router.push('/patients')">
            患者列表
          </el-button>
        </div>
      </template>
      <!-- 最近患者：桌面表格 / 移动卡片 -->
      <div v-if="isMobile" class="recent-cards">
        <div v-if="!recentPatients.length" class="empty-todo">暂无最近患者</div>
        <article
          v-for="row in recentPatients.slice(0, 3)"
          :key="row.id"
          class="recent-item pressable"
          @click="$router.push(`/patients/${row.id}`)"
        >
          <div class="recent-main">
            <div class="recent-name-row">
              <span class="recent-name">{{ row.name }}</span>
              <span class="recent-age">{{ row.age ?? '-' }}岁</span>
              <span class="recent-disease">· {{ row.diseaseType || '未分类' }}</span>
            </div>
          </div>
          <span class="recent-link">详情 ></span>
        </article>
        <button type="button" class="view-all pressable" @click="$router.push('/patients')">
          查看全部 →
        </button>
      </div>
      <div v-else class="table-scroll">
        <el-table :data="recentPatients" style="width: 100%" class="recent-table">
          <el-table-column prop="name" label="姓名" min-width="100" />
          <el-table-column prop="age" label="年龄" width="80" />
          <el-table-column prop="phone" label="电话" min-width="130" />
          <el-table-column prop="diseaseType" label="疾病" min-width="110" />
          <el-table-column prop="createTime" label="收录时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                class="pressable"
                @click="$router.push(`/patients/${row.id}`)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onActivated,
  onMounted,
  onBeforeUnmount,
  reactive,
  ref,
  watch,
} from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import echarts from '@/utils/echarts';
import { getHomeDashboard } from '@/api/home';
import { useMobile } from '@/composables/useMobile';

defineOptions({ name: 'Dashboard' });

const router = useRouter();
const store = useStore();
const { isMobile } = useMobile();
const loading = ref(false);
const ready = ref(false);
const summary = reactive({
  totalPatients: 0,
  totalDelta: 0,
  todayNewPatients: 0,
  overdueFollowUpCount: 0,
  highRiskCount: 0,
});
const attentionTodos = ref([]);
const recentPatients = ref([]);
const highRiskPatients = ref([]);
const diseasePie = ref([]);
const visitTrend = reactive({ labels: [], values: [] });

const displayTotal = ref(0);
const displayToday = ref(0);
const displayOverdue = ref(0);
const displayHighRisk = ref(0);

const trendChartRef = ref(null);
const pieChartRef = ref(null);
let trendChart = null;
let pieChart = null;

const isAdmin = computed(() => store.getters.isAdmin);
const isMember = computed(() => store.getters.isMember);

const prefersReducedMotion = () =>
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('zh-CN');
};

const easeOut = (t) => 1 - Math.pow(1 - t, 3);

const animateNumber = (targetRef, toValue) => {
  const to = Number(toValue) || 0;
  if (prefersReducedMotion()) {
    targetRef.value = to;
    return;
  }
  const from = Number(targetRef.value) || 0;
  if (from === to) {
    targetRef.value = to;
    return;
  }
  const duration = 280;
  const start = performance.now();
  const step = (now) => {
    const progress = Math.min((now - start) / duration, 1);
    targetRef.value = Math.round(from + (to - from) * easeOut(progress));
    if (progress < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
};

const goOverdue = () => {
  if (!(isAdmin.value || isMember.value)) {
    ElMessage.warning('请使用成员/管理员账号查看诊断记录');
    return;
  }
  router.push({ path: '/diagnosis', query: { overdue: '1' } });
};

const goHighRisk = () => {
  const ids = highRiskPatients.value.map((item) => item.id).filter(Boolean);
  if (!ids.length) {
    ElMessage.info('当前暂无高危患者');
    return;
  }
  router.push({
    path: '/patients',
    query: { ids: ids.join(','), controlStatus: 'high' },
  });
};

const goDiagnosis = (patientId) => {
  router.push({ path: '/diagnosis', query: { patientId } });
};

const handleTodoClick = (item) => {
  if (isAdmin.value || isMember.value) {
    goDiagnosis(item.patientId);
    return;
  }
  router.push(`/patients/${item.patientId}`);
};

const metricCards = computed(() => [
  {
    key: 'total',
    label: '总管理人数',
    value: displayTotal.value,
    meta: summary.totalDelta > 0 ? `近7天 +${summary.totalDelta}` : '近7天持平',
    metaClass: summary.totalDelta > 0 ? 'up' : '',
    tone: 'neutral',
  },
  {
    key: 'today',
    label: '今日新增患者',
    value: displayToday.value,
    meta: '人',
    tone: 'neutral',
  },
  {
    key: 'overdue',
    label: '待随访（逾期）',
    value: displayOverdue.value,
    meta: '点击查看逾期记录',
    metaClass: 'action',
    tone: 'warn',
    dangerValue: true,
    clickable: true,
    onClick: goOverdue,
  },
  {
    key: 'high',
    label: '高危预警',
    value: displayHighRisk.value,
    meta: '点击筛选高危患者',
    metaClass: 'action',
    tone: 'danger',
    dangerValue: true,
    clickable: true,
    pulse: (summary.highRiskCount || 0) > 0,
    onClick: goHighRisk,
  },
]);

const renderCharts = () => {
  if (trendChartRef.value) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value);
    const mobile = isMobile.value;
    trendChart.setOption({
      animationDuration: prefersReducedMotion() ? 0 : 280,
      animationEasing: 'cubicOut',
      grid: mobile
        ? { left: 28, right: 8, top: 12, bottom: 22, containLabel: false }
        : { left: 28, right: 12, top: 16, bottom: 24 },
      xAxis: {
        type: 'category',
        data: visitTrend.labels,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#d8dde3' } },
        axisLabel: { color: '#6b7280', fontSize: mobile ? 10 : 11 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { type: 'dashed', color: '#e8ecf0' } },
        axisLabel: { color: '#6b7280', fontSize: mobile ? 10 : 11 },
      },
      series: [
        {
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: mobile ? 5 : 6,
          data: visitTrend.values,
          lineStyle: { width: 2.5, color: '#007AFF' },
          itemStyle: { color: '#007AFF' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(0,122,255,0.22)' },
                { offset: 1, color: 'rgba(0,122,255,0.02)' },
              ],
            },
          },
        },
      ],
      tooltip: { trigger: 'axis' },
    });
    if (mobile) {
      requestAnimationFrame(() => trendChart?.resize());
    }
  }

  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value);
    pieChart.setOption({
      animationDuration: prefersReducedMotion() ? 0 : 280,
      animationEasing: 'cubicOut',
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['50%', '72%'],
          center: ['50%', '50%'],
          label: { fontSize: 11, color: '#4b5563' },
          data: diseasePie.value.map((item) => ({ name: item.name, value: item.value })),
          color: ['#007AFF', '#FF9500', '#FF3B30', '#34C759', '#AF52DE'],
        },
      ],
    });
  }
};

const fetchDashboard = async () => {
  loading.value = true;
  try {
    const res = await getHomeDashboard();
    const data = res.data || {};
    Object.assign(summary, data.summary || {});
    attentionTodos.value = data.attentionTodos || [];
    recentPatients.value = data.recentPatients || [];
    highRiskPatients.value = data.highRiskPatients || [];
    diseasePie.value = data.diseasePie || [];
    Object.assign(visitTrend, data.visitTrend7Days || { labels: [], values: [] });

    animateNumber(displayTotal, summary.totalPatients);
    animateNumber(displayToday, summary.todayNewPatients);
    animateNumber(displayOverdue, summary.overdueFollowUpCount);
    animateNumber(displayHighRisk, summary.highRiskCount);

    await nextTick();
    renderCharts();
    ready.value = true;
    // 移动端布局稳定后再 resize，避免图表变形
    requestAnimationFrame(() => {
      handleResize();
    });
  } catch (error) {
    console.error('获取首页数据失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleResize = () => {
  trendChart?.resize();
  pieChart?.resize();
};

watch(
  () => store.state.homeRefreshKey,
  () => {
    fetchDashboard();
  }
);

let firstActivate = true;
onMounted(() => {
  fetchDashboard();
  window.addEventListener('resize', handleResize);
});

// keep-alive：首次 onMounted 已拉取，避免与 onActivated 双请求
onActivated(() => {
  if (firstActivate) {
    firstActivate = false;
    return;
  }
  fetchDashboard();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  trendChart?.dispose();
  pieChart?.dispose();
  trendChart = null;
  pieChart = null;
});
</script>

<style scoped>
.dashboard {
  --stagger: 0ms;
}

.metric-row {
  margin-bottom: 16px;
}

.metric-card {
  background: var(--color-surface);
  border: none;
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  padding: 20px;
  min-height: 120px;
  opacity: 0;
  transform: translateY(8px) scale(0.98);
  transition:
    opacity var(--duration-panel) var(--ease-out),
    transform var(--duration-panel) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
  transition-delay: var(--stagger);
}

.dashboard[data-ready='true'] .metric-card {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.metric-card.warn {
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.12);
}

.metric-card.danger {
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.12);
}

.metric-card.clickable {
  cursor: pointer;
}

.metric-card.clickable:active {
  transform: scale(0.97);
}

@media (hover: hover) and (pointer: fine) {
  .metric-card.clickable:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  }
}

.metric-card.pulse {
  animation: pulse-glow 1.8s var(--ease-in-out) infinite;
}

@keyframes pulse-glow {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(255, 59, 48, 0.25);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(255, 59, 48, 0);
  }
}

.metric-label {
  color: var(--color-muted);
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-ink);
  line-height: 1.05;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
}

.danger-text {
  color: var(--color-danger);
}

.metric-meta {
  margin-top: 8px;
  font-size: 14px;
  color: var(--color-muted);
}

.metric-meta.up {
  color: var(--color-safe);
}

.metric-meta.action {
  color: var(--color-primary);
  font-size: 13px;
}

.main-row {
  margin-bottom: 16px;
}

.dual-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
  align-items: stretch;
}

.panel-card {
  min-height: 0;
}

.panel-card--chart {
  /* 自然高度由图表内容决定，作为行高基准 */
}

.panel-card--todo {
  /* 高度跟左侧对齐：不参与撑开行高，内部滚动 */
  height: 0;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-card--todo :deep(.el-card__header) {
  flex-shrink: 0;
}

.panel-card--todo :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 12px !important;
}

.panel-card--chart :deep(.el-card__body) {
  padding-bottom: 16px !important;
}

.chart-body {
  /* 固定图表区，避免被右侧影响 */
}

.todo-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
}

.recent-card {
  min-height: auto;
  margin-bottom: 8px;
}

.enter-panel {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
  transition:
    opacity var(--duration-panel) var(--ease-out),
    transform var(--duration-panel) var(--ease-out);
}

.dashboard[data-ready='true'] .enter-panel {
  opacity: 1;
  transform: translateY(0) scale(1);
  transition-delay: 120ms;
}

.dashboard[data-ready='true'] .enter-panel-delay {
  transition-delay: 170ms;
}

.dashboard[data-ready='true'] .enter-panel-late {
  transition-delay: 220ms;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 17px;
  font-weight: 650;
  letter-spacing: -0.015em;
  color: var(--color-ink);
}

.mini-chart {
  height: 160px;
}

.mini-disease {
  margin-top: 8px;
  border-top: 1px solid var(--color-line);
  padding-top: 12px;
}

.mini-disease-title {
  font-size: 13px;
  color: var(--color-muted);
  margin-bottom: 4px;
}

.mini-pie {
  height: 150px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-todo {
  color: var(--color-muted);
  text-align: center;
  padding: 48px 0;
  font-size: 15px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 991px) and (min-width: 431px) {
  .dual-panels {
    grid-template-columns: 1fr;
  }

  .panel-card--todo {
    height: auto;
    min-height: 320px;
    max-height: 420px;
  }
}

/* iPhone 15 / 移动端独立布局（非 PC 压缩） */
@media (max-width: 430px) {
  .dashboard {
    max-width: 393px;
    margin: 0 auto;
  }

  .metric-row {
    margin-bottom: 12px;
  }

  .metric-row :deep(.el-col) {
    margin-bottom: 12px;
  }

  .metric-card {
    border-radius: 12px;
    padding: 14px 12px;
    min-height: 96px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }

  .metric-label {
    font-size: 12px;
    margin-bottom: 4px;
  }

  .metric-value {
    font-size: 24px;
  }

  .metric-meta {
    font-size: 12px;
    margin-top: 4px;
  }

  .dual-panels {
    grid-template-columns: 1fr;
    gap: 12px;
    margin-bottom: 12px;
  }

  .panel-card--chart {
    display: flex;
    flex-direction: column;
    height: auto;
    overflow: hidden;
  }

  .panel-card--chart :deep(.el-card__header) {
    flex-shrink: 0;
    padding: 12px 16px !important;
  }

  .panel-card--chart :deep(.el-card__body) {
    flex: none;
    min-height: 0;
    overflow: hidden;
    padding: 0 12px 12px !important;
  }

  .chart-body {
    height: 130px;
    overflow: hidden;
  }

  .mini-chart {
    height: 130px;
    width: 100%;
    overflow: hidden;
  }

  .mini-disease {
    display: none !important;
  }

  .panel-card--todo {
    height: 220px;
    min-height: 220px;
    max-height: 220px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .panel-card--todo :deep(.el-card__header) {
    padding: 12px 16px !important;
  }

  .panel-card--todo :deep(.el-card__body) {
    padding: 8px 12px 12px !important;
  }

  .todo-scroll {
    overscroll-behavior: contain;
  }

  .panel-title {
    font-size: 15px;
  }

  .todo-item {
    min-height: 72px;
    padding: 12px 14px 12px 12px;
  }

  .todo-name {
    font-size: 15px;
  }

  .todo-reason {
    font-size: 13px;
  }

  .recent-card :deep(.el-card__header),
  .recent-card :deep(.el-card__body) {
    padding-left: 16px !important;
    padding-right: 16px !important;
  }

  .recent-cards {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .recent-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 52px;
    padding: 10px 0;
    border-bottom: 0.5px solid #e5e5ea;
    cursor: pointer;
  }

  .recent-item:last-of-type {
    border-bottom: none;
  }

  .recent-name {
    font-size: 17px;
    font-weight: 700;
    color: #1c1c1e;
  }

  .recent-age,
  .recent-disease {
    font-size: 15px;
    color: #8e8e93;
    margin-left: 6px;
  }

  .recent-link {
    font-size: 15px;
    color: #c7c7cc;
    flex-shrink: 0;
  }

  .view-all {
    width: 100%;
    margin-top: 4px;
    min-height: 44px;
    border: none;
    background: transparent;
    color: #007aff;
    font-size: 15px;
    cursor: pointer;
  }
}

.todo-item {
  display: flex;
  align-items: center;
  padding: 14px 14px 14px 12px;
  border-radius: var(--radius-item);
  background: var(--color-hover);
  border-left: 3px solid transparent;
  gap: 0;
  min-height: 64px;
  cursor: pointer;
  opacity: 0;
  transform: translateY(6px) scale(0.98);
  transition:
    opacity var(--duration-ui) var(--ease-out),
    transform var(--duration-ui) var(--ease-out),
    background-color var(--duration-fast) var(--ease);
  transition-delay: var(--stagger);
}

.dashboard[data-ready='true'] .todo-item {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.todo-item.high {
  background: rgba(255, 59, 48, 0.06);
  border-left-color: #ff3b30;
}

.todo-item.overdue {
  background: rgba(255, 149, 0, 0.08);
  border-left-color: #ff9500;
}

.todo-item.due_soon {
  background: rgba(52, 199, 89, 0.08);
  border-left-color: #34c759;
}

.todo-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
  width: 100%;
}

.todo-text {
  min-width: 0;
  flex: 1;
}

.todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 7px;
  background: var(--color-safe);
  flex-shrink: 0;
}

.todo-item.high .todo-dot {
  background: #ff3b30;
}
.todo-item.overdue .todo-dot {
  background: #ff9500;
}
.todo-item.due_soon .todo-dot {
  background: #34c759;
}

.todo-name {
  font-weight: 650;
  color: var(--color-ink);
  letter-spacing: -0.01em;
  font-size: 16px;
}

.todo-item.high .todo-name {
  color: #ff3b30;
}

.todo-item.overdue .todo-name {
  color: #ff9500;
}

.todo-reason {
  margin-top: 4px;
  color: var(--color-muted);
  font-size: 14px;
  line-height: 1.4;
}

.todo-item.high .todo-reason {
  color: rgba(255, 59, 48, 0.75);
}

.todo-item.overdue .todo-reason {
  color: rgba(255, 149, 0, 0.85);
}

@media (prefers-reduced-motion: reduce) {
  .metric-card,
  .enter-panel,
  .todo-item {
    opacity: 1 !important;
    transform: none !important;
    transition: none !important;
    animation: none !important;
  }

  .metric-card.pulse {
    animation: none;
  }
}
</style>
