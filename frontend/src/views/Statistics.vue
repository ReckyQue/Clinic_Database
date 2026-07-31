<template>
  <div v-loading="loading" class="statistics">
    <div class="page-header">
      <h2>统计分析</h2>
      <span class="updated-at">最后更新：{{ updatedAtText }}</span>
    </div>

    <el-row :gutter="12" class="summary-row">
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="big-card">
          <div class="big-label">总管理人数</div>
          <div class="big-number">{{ summary.totalPatients || 0 }}</div>
          <div class="delta" :class="deltaClass(summary.totalDelta)">
            {{ formatDelta(summary.totalDelta) }} 较上月
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="big-card">
          <div class="big-label">本月初诊</div>
          <div class="big-number">{{ summary.monthNew || 0 }}</div>
          <div class="delta" :class="deltaClass(summary.monthNewDelta)">
            {{ formatDelta(summary.monthNewDelta) }} 较上月
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :lg="6">
        <div class="big-card" :class="{ warn: (summary.monthFollowUpDelta || 0) < 0 }">
          <div class="big-label">本月随访</div>
          <div class="big-number">{{ summary.monthFollowUp || 0 }}</div>
          <div class="delta" :class="deltaClass(summary.monthFollowUpDelta)">
            {{ formatDelta(summary.monthFollowUpDelta) }} 较上月
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :lg="6">
        <div
          class="big-card danger pressable"
          :class="{ pulse: (summary.highRiskCount || 0) > 0 }"
          @click="goHighRisk"
        >
          <div class="big-label">高危预警</div>
          <div class="big-number">{{ summary.highRiskCount || 0 }}</div>
          <div class="danger-action">点击筛选高危患者</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">疾病构成比</span></template>
          <div ref="diseaseChartRef" class="chart-box"></div>
          <div v-if="!diseasePie.length" class="empty-tip">暂无疾病统计数据</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">年龄与性别分布</span></template>
          <div ref="ageGenderChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">各村/片区患者分布</span></template>
          <div ref="villageChartRef" class="chart-box"></div>
          <div v-if="!villageBars.length" class="empty-tip">暂无地区分布数据</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-title">近半年新增 / 随访趋势</span></template>
          <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="control-card">
      <template #header>
        <div class="control-header">
          <span class="chart-title">病情控制状况</span>
          <span class="control-sub">点击查看名单</span>
        </div>
      </template>
      <el-row :gutter="10" class="control-row">
        <el-col :xs="8" :md="8">
          <div class="status-block green pressable" @click="openRiskDrawer('controlled')">
            <div class="status-ring">
              <span class="status-percent">{{ controlStatus.controlledPercent || 0 }}%</span>
            </div>
            <div class="status-name">达标</div>
            <div class="status-count">{{ controlStatus.controlledCount || 0 }} 人</div>
          </div>
        </el-col>
        <el-col :xs="8" :md="8">
          <div class="status-block yellow pressable" @click="openRiskDrawer('warning')">
            <div class="status-ring">
              <span class="status-percent">{{ controlStatus.warningPercent || 0 }}%</span>
            </div>
            <div class="status-name">预警</div>
            <div class="status-count">{{ controlStatus.warningCount || 0 }} 人</div>
          </div>
        </el-col>
        <el-col :xs="8" :md="8">
          <div class="status-block red pressable" @click="goHighRisk">
            <div class="status-ring">
              <span class="status-percent">{{ controlStatus.highPercent || 0 }}%</span>
            </div>
            <div class="status-name">高危</div>
            <div class="status-count">{{ controlStatus.highCount || 0 }} 人</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" :size="drawerSize" direction="rtl">
      <div class="drawer-table-wrap">
        <el-table :data="drawerPatients" style="width: 100%; min-width: 420px">
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column prop="age" label="年龄" width="70" />
          <el-table-column prop="diseaseType" label="疾病" min-width="100" />
          <el-table-column prop="phone" label="电话" min-width="120" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="goPatient(row.id)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-if="!drawerPatients.length" description="暂无此类患者" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import echarts from '@/utils/echarts';
import { getStatisticsDashboard } from '@/api/statistics';
import { useMobile } from '@/composables/useMobile';

const router = useRouter();
const { isMobile } = useMobile();
const loading = ref(false);
const updatedAt = ref('');
const summary = reactive({});
const diseasePie = ref([]);
const ageGender = reactive({ categories: [], male: [], female: [] });
const villageBars = ref([]);
const trend = reactive({ months: [], newPatients: [], followUps: [] });
const controlStatus = reactive({});

const diseaseChartRef = ref(null);
const ageGenderChartRef = ref(null);
const villageChartRef = ref(null);
const trendChartRef = ref(null);

let diseaseChart;
let ageGenderChart;
let villageChart;
let trendChart;

const drawerVisible = ref(false);
const drawerLevel = ref('high');
const drawerPatients = ref([]);

const drawerSize = computed(() => (isMobile.value ? '92%' : '480px'));

const drawerTitle = computed(
  () =>
    ({
      controlled: '控制达标患者',
      warning: '临界/预警患者',
      high: '高危/失控患者',
    })[drawerLevel.value]
);

const updatedAtText = computed(() => {
  if (!updatedAt.value) return '--';
  const d = new Date(updatedAt.value);
  const today = new Date();
  const sameDay = d.toDateString() === today.toDateString();
  const time = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  return sameDay ? `今日 ${time}` : d.toLocaleString('zh-CN');
});

const formatDelta = (value) => {
  const num = Number(value || 0);
  if (num > 0) return `↑ +${num}`;
  if (num < 0) return `↓ ${num}`;
  return '— 0';
};

const deltaClass = (value) => {
  const num = Number(value || 0);
  if (num > 0) return 'up';
  if (num < 0) return 'down';
  return 'flat';
};

const openRiskDrawer = (level) => {
  drawerLevel.value = level;
  if (level === 'controlled') {
    drawerPatients.value = controlStatus.controlledPatients || [];
  } else if (level === 'warning') {
    drawerPatients.value = controlStatus.warningPatients || [];
  } else {
    drawerPatients.value = controlStatus.highPatients || summary.highRiskPatients || [];
  }
  drawerVisible.value = true;
};

/** 与首页一致：跳转患者列表并按高危 ids 筛选 */
const goHighRisk = () => {
  const list = summary.highRiskPatients || controlStatus.highPatients || [];
  const ids = list.map((item) => item.id).filter(Boolean);
  if (!ids.length) {
    ElMessage.info('当前暂无高危患者');
    return;
  }
  router.push({
    path: '/patients',
    query: { ids: ids.join(','), controlStatus: 'high' },
  });
};

const goPatient = (id) => {
  drawerVisible.value = false;
  router.push(`/patients/${id}`);
};

const renderDiseaseChart = () => {
  if (!diseaseChartRef.value) return;
  if (!diseaseChart) diseaseChart = echarts.init(diseaseChartRef.value);
  const mobile = isMobile.value;
  const total = diseasePie.value.reduce((sum, item) => sum + Number(item.value || 0), 0);
  diseaseChart.setOption(
    {
      color: ['#007AFF', '#FF9500', '#34C759', '#AF52DE', '#FF3B30', '#5AC8FA'],
      tooltip: {
        trigger: 'item',
        triggerOn: mobile ? 'click' : 'mousemove',
        formatter: '{b}: {c}人 ({d}%)',
      },
      legend: {
        bottom: 0,
        left: 'center',
        orient: 'horizontal',
        itemWidth: mobile ? 10 : 12,
        itemHeight: mobile ? 10 : 12,
        itemGap: mobile ? 6 : 12,
        textStyle: { fontSize: mobile ? 11 : 13, color: '#8E8E93' },
      },
      series: [
        {
          type: 'pie',
          radius: mobile ? ['36%', '58%'] : ['48%', '72%'],
          center: mobile ? ['50%', '42%'] : ['50%', '45%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: {
            show: !mobile,
            formatter: '{b}\n{d}%',
            fontSize: 13,
          },
          data: diseasePie.value.length
            ? diseasePie.value
            : [{ name: '暂无数据', value: 0, itemStyle: { color: '#d1d5db' } }],
          emphasis: { scale: !mobile },
        },
      ],
      graphic: [
        {
          type: 'text',
          left: 'center',
          top: mobile ? '36%' : '40%',
          style: {
            text: `${total}\n总数`,
            textAlign: 'center',
            fill: '#1C1C1E',
            fontSize: mobile ? 13 : 16,
            fontWeight: 700,
            lineHeight: mobile ? 18 : 24,
          },
        },
      ],
    },
    true
  );
};

const renderAgeGenderChart = () => {
  if (!ageGenderChartRef.value) return;
  if (!ageGenderChart) ageGenderChart = echarts.init(ageGenderChartRef.value);
  const mobile = isMobile.value;
  ageGenderChart.setOption(
    {
      color: ['#007AFF', '#FF6B8A'],
      tooltip: { trigger: 'axis', triggerOn: mobile ? 'click' : 'mousemove' },
      legend: {
        data: ['男', '女'],
        top: 0,
        right: mobile ? 0 : 'auto',
        textStyle: { fontSize: mobile ? 11 : 13 },
      },
      grid: {
        left: mobile ? 28 : 40,
        right: mobile ? 12 : 20,
        top: mobile ? 32 : 40,
        bottom: mobile ? 24 : 30,
      },
      xAxis: {
        type: 'category',
        data: ageGender.categories || [],
        axisLabel: { fontSize: mobile ? 10 : 13, color: '#8E8E93', interval: 0 },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { fontSize: mobile ? 10 : 12, color: '#8E8E93' },
      },
      series: [
        {
          name: '男',
          type: 'bar',
          stack: 'total',
          barWidth: mobile ? 18 : 28,
          data: ageGender.male || [],
        },
        {
          name: '女',
          type: 'bar',
          stack: 'total',
          barWidth: mobile ? 18 : 28,
          data: ageGender.female || [],
        },
      ],
    },
    true
  );
};

const renderVillageChart = () => {
  if (!villageChartRef.value) return;
  if (!villageChart) villageChart = echarts.init(villageChartRef.value);
  const mobile = isMobile.value;
  const names = villageBars.value.map((item) => item.name);
  const values = villageBars.value.map((item) => item.value);
  villageChart.setOption(
    {
      color: ['#007AFF'],
      tooltip: { trigger: 'axis', triggerOn: mobile ? 'click' : 'mousemove' },
      grid: {
        left: mobile ? 72 : 100,
        right: mobile ? 36 : 30,
        top: 12,
        bottom: 12,
      },
      xAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { fontSize: mobile ? 10 : 12, color: '#8E8E93' },
      },
      yAxis: {
        type: 'category',
        data: [...names].reverse(),
        axisLabel: {
          fontSize: mobile ? 11 : 13,
          color: '#1C1C1E',
          width: mobile ? 64 : 90,
          overflow: 'truncate',
        },
      },
      series: [
        {
          type: 'bar',
          data: [...values].reverse(),
          barWidth: mobile ? 12 : 18,
          label: {
            show: true,
            position: 'right',
            fontSize: mobile ? 11 : 13,
            color: '#8E8E93',
          },
        },
      ],
    },
    true
  );
};

const renderTrendChart = () => {
  if (!trendChartRef.value) return;
  if (!trendChart) trendChart = echarts.init(trendChartRef.value);
  const mobile = isMobile.value;
  trendChart.setOption(
    {
      color: ['#007AFF', '#34C759'],
      tooltip: { trigger: 'axis', triggerOn: mobile ? 'click' : 'mousemove' },
      legend: {
        data: ['新增确诊', '随访复诊'],
        top: 0,
        right: mobile ? 0 : 'auto',
        textStyle: { fontSize: mobile ? 11 : 13 },
      },
      grid: {
        left: mobile ? 28 : 40,
        right: mobile ? 12 : 20,
        top: mobile ? 32 : 40,
        bottom: mobile ? 24 : 30,
      },
      xAxis: {
        type: 'category',
        data: trend.months || [],
        axisLabel: { fontSize: mobile ? 11 : 12, color: '#8E8E93' },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { fontSize: mobile ? 10 : 12, color: '#8E8E93' },
      },
      series: [
        {
          name: '新增确诊',
          type: 'line',
          smooth: true,
          symbolSize: mobile ? 6 : 8,
          data: trend.newPatients || [],
        },
        {
          name: '随访复诊',
          type: 'line',
          smooth: true,
          symbolSize: mobile ? 6 : 8,
          data: trend.followUps || [],
        },
      ],
    },
    true
  );
};

const renderAllCharts = async () => {
  await nextTick();
  renderDiseaseChart();
  renderAgeGenderChart();
  renderVillageChart();
  renderTrendChart();
  requestAnimationFrame(() => handleResize());
};

const fetchDashboard = async () => {
  loading.value = true;
  try {
    const res = await getStatisticsDashboard();
    const data = res.data || {};
    Object.assign(summary, data.summary || {});
    diseasePie.value = data.diseasePie || [];
    Object.assign(ageGender, data.ageGender || { categories: [], male: [], female: [] });
    villageBars.value = data.villageBars || [];
    Object.assign(trend, data.trend || { months: [], newPatients: [], followUps: [] });
    Object.assign(controlStatus, data.controlStatus || {});
    updatedAt.value = data.updatedAt || '';
    await renderAllCharts();
  } catch (error) {
    console.error('获取统计仪表盘失败:', error);
  } finally {
    loading.value = false;
  }
};

let resizeTimer = 0;
const handleResize = () => {
  // 仅 resize，避免窗口拖动时反复 setOption 重绘
  window.clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => {
    diseaseChart?.resize();
    ageGenderChart?.resize();
    villageChart?.resize();
    trendChart?.resize();
  }, 160);
};

onMounted(() => {
  fetchDashboard();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.clearTimeout(resizeTimer);
  window.removeEventListener('resize', handleResize);
  diseaseChart?.dispose();
  ageGenderChart?.dispose();
  villageChart?.dispose();
  trendChart?.dispose();
  diseaseChart = null;
  ageGenderChart = null;
  villageChart = null;
  trendChart = null;
});
</script>

<style scoped>
.statistics {
  min-height: 100%;
  padding-bottom: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-ink, #1c1c1e);
  line-height: 1.4;
}

.updated-at {
  color: var(--color-muted, #8e8e93);
  font-size: 13px;
  flex-shrink: 0;
}

.summary-row,
.chart-row {
  margin-bottom: 16px;
}

.summary-row :deep(.el-col),
.chart-row :deep(.el-col) {
  margin-bottom: 12px;
}

.big-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px 18px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  min-height: 120px;
  height: 100%;
}

.big-card.warn {
  background: rgba(255, 149, 0, 0.08);
}

.big-card.danger {
  background: #fff;
  color: inherit;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.12);
}

.big-card.danger.pulse {
  animation: pulse-glow 1.8s cubic-bezier(0.77, 0, 0.175, 1) infinite;
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

.big-label {
  font-size: 14px;
  color: var(--color-muted, #8e8e93);
  margin-bottom: 8px;
}

.big-card.danger .big-label {
  color: var(--color-muted, #8e8e93);
}

.big-number {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--color-ink, #1c1c1e);
  font-variant-numeric: tabular-nums;
}

.big-card.danger .big-number {
  color: #ff3b30;
}

.delta {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
}

.delta.up {
  color: #34c759;
}
.delta.down {
  color: #ff3b30;
}
.delta.flat {
  color: #8e8e93;
}

.big-card.danger .delta,
.big-card.danger .danger-action {
  color: #007aff;
}

.danger-action {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 650;
}

.chart-card {
  border: none !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
  border-radius: 16px !important;
}

.chart-card :deep(.el-card__header) {
  padding: 16px 20px !important;
  border-bottom: 1px solid #e5e5ea !important;
}

.chart-card :deep(.el-card__body) {
  padding: 16px 20px 20px !important;
}

.chart-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--color-ink, #1c1c1e);
  letter-spacing: -0.01em;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.empty-tip {
  text-align: center;
  color: #8e8e93;
  font-size: 13px;
  margin-top: -12px;
}

.control-card {
  margin-bottom: 20px;
  border: none !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
  border-radius: 16px !important;
}

.control-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.control-sub {
  color: #8e8e93;
  font-size: 13px;
}

.status-block {
  border-radius: 14px;
  padding: 18px 12px;
  text-align: center;
  cursor: pointer;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: transform 160ms var(--ease-out, ease-out);
}

@media (hover: hover) and (pointer: fine) {
  .status-block:hover {
    transform: translateY(-2px);
  }
}

.status-block.green {
  background: rgba(52, 199, 89, 0.12);
  border: 1px solid rgba(52, 199, 89, 0.35);
}

.status-block.yellow {
  background: rgba(255, 149, 0, 0.12);
  border: 1px solid rgba(255, 149, 0, 0.35);
}

.status-block.red {
  background: rgba(255, 59, 48, 0.12);
  border: 1px solid rgba(255, 59, 48, 0.35);
}

.status-ring {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  background: #fff;
  box-shadow: inset 0 0 0 6px currentColor;
}

.status-block.green .status-ring {
  color: #34c759;
}
.status-block.yellow .status-ring {
  color: #ff9500;
}
.status-block.red .status-ring {
  color: #ff3b30;
}

.status-name {
  font-size: 15px;
  font-weight: 650;
  margin-bottom: 4px;
  color: #1c1c1e;
}

.status-percent {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.status-block.green .status-percent {
  color: #34c759;
}
.status-block.yellow .status-percent {
  color: #ff9500;
}
.status-block.red .status-percent {
  color: #ff3b30;
}

.status-count {
  font-size: 13px;
  color: #8e8e93;
}

.drawer-table-wrap {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

/* iPhone 15 / 小屏：做减法 */
@media (max-width: 430px) {
  .statistics {
    padding-bottom: 20px;
  }

  .page-header {
    margin-bottom: 12px;
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .page-header h2 {
    display: none;
  }

  .updated-at {
    font-size: 11px;
  }

  .summary-row,
  .chart-row {
    margin-bottom: 4px;
  }

  .summary-row :deep(.el-col),
  .chart-row :deep(.el-col) {
    margin-bottom: 12px;
  }

  .big-card {
    border-radius: 12px;
    padding: 12px 10px;
    min-height: 76px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  }

  .big-label {
    font-size: 12px;
    margin-bottom: 4px;
  }

  .big-number {
    font-size: 22px;
  }

  .delta,
  .danger-action {
    margin-top: 4px;
    font-size: 11px;
  }

  .chart-card {
    border-radius: 12px !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  }

  .chart-card :deep(.el-card__header) {
    padding: 12px 14px !important;
  }

  .chart-card :deep(.el-card__body) {
    padding: 8px 12px 12px !important;
  }

  .chart-title {
    font-size: 15px;
  }

  .chart-box {
    height: 180px;
  }

  .control-card {
    border-radius: 12px !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
    margin-bottom: 8px;
  }

  .control-card :deep(.el-card__header) {
    padding: 12px 14px !important;
  }

  .control-card :deep(.el-card__body) {
    padding: 12px 10px 14px !important;
  }

  .control-sub {
    font-size: 11px;
  }

  .status-block {
    min-height: 120px;
    padding: 10px 4px;
    border-radius: 12px;
  }

  .status-ring {
    width: 65px;
    height: 65px;
    margin-bottom: 8px;
    box-shadow: inset 0 0 0 5px currentColor;
  }

  .status-percent {
    font-size: 15px;
  }

  .status-name {
    font-size: 13px;
    margin-bottom: 2px;
  }

  .status-count {
    font-size: 12px;
  }
}
</style>
