<template>
  <div class="diagnosis-page" :class="{ 'is-mobile': isMobile }">
    <el-row v-if="!isMobile" :gutter="16" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card today">
          <div class="stat-value">{{ stats.todayCount }}</div>
          <div class="stat-label">今日诊断数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card month">
          <div class="stat-value">{{ stats.monthCount }}</div>
          <div class="stat-label">本月新增</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card abnormal">
          <div class="stat-value">{{ stats.abnormalCount }}</div>
          <div class="stat-label">异常指标预警</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 移动端精简统计 -->
    <div v-else class="m-stats">
      <div class="m-stat">
        <strong>{{ stats.todayCount }}</strong
        ><span>今日</span>
      </div>
      <div class="m-stat">
        <strong>{{ stats.monthCount }}</strong
        ><span>本月</span>
      </div>
      <div class="m-stat danger">
        <strong>{{ stats.abnormalCount }}</strong
        ><span>异常</span>
      </div>
    </div>

    <div v-if="isMobile" class="m-filter-bar">
      <el-input
        v-model="searchForm.keyword"
        clearable
        placeholder="搜索姓名或电话"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-model="searchForm.diseaseType"
        placeholder="疾病"
        clearable
        @change="handleSearch"
      >
        <el-option v-for="item in diseaseOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-checkbox v-model="searchForm.onlyOverdue" @change="handleSearch">逾期</el-checkbox>
    </div>

    <el-card shadow="hover" :class="{ 'm-card-flat': isMobile }">
      <template v-if="!isMobile" #header>
        <div class="card-header">
          <span>诊断记录</span>
          <el-button type="primary" @click="openForm('create')">
            <el-icon><Plus /></el-icon> 新增诊断
          </el-button>
        </div>
      </template>

      <el-form v-if="!isMobile" :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="疾病类型">
          <el-select
            v-model="searchForm.diseaseType"
            placeholder="全部类型"
            clearable
            style="width: 150px"
          >
            <el-option v-for="item in diseaseOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="诊断日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="患者搜索">
          <el-input
            v-model="searchForm.keyword"
            placeholder="姓名或电话"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="searchForm.onlyOverdue">仅看逾期随访</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 移动端卡片 -->
      <div v-if="isMobile" v-loading="loading" class="m-list">
        <div v-if="!loading && !records.length" class="m-empty">
          <el-empty description="暂无诊断记录" />
        </div>
        <article
          v-for="row in records"
          :key="row.id"
          class="dx-card pressable"
          @click="openForm('view', row)"
        >
          <div class="dx-head">
            <span class="dx-name">{{ row.patientName }}</span>
            <span class="dx-disease"
              >{{ diseaseIcon(row.diseaseType) }} {{ row.diseaseType || '未分类' }}</span
            >
            <span class="dx-date">{{ row.diagnosisDate || '-' }}</span>
          </div>
          <div class="dx-metrics">{{ row.metricsSummary }}</div>
          <div v-if="row.medication" class="dx-med">用药：{{ row.medication }}</div>
          <div class="dx-follow">
            <span>下次随访：{{ row.nextFollowUpDate || '-' }}</span>
            <span v-if="row.followUpOverdue" class="overdue-tag">⚠️ 逾期</span>
          </div>
          <div class="dx-actions" @click.stop>
            <el-button type="primary" link @click="openForm('view', row)">详情 →</el-button>
            <el-button type="warning" link @click="openForm('edit', row)">编辑</el-button>
          </div>
        </article>
      </div>

      <el-table v-else v-loading="loading" :data="records" style="width: 100%">
        <el-table-column prop="id" label="记录编号" width="90" />
        <el-table-column label="患者姓名" min-width="110">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/patients/${row.patientId}`)">
              {{ row.patientName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="疾病类型" width="130">
          <template #default="{ row }">
            <el-tag :type="diseaseTagType(row.diseaseType)" effect="dark">
              {{ diseaseIcon(row.diseaseType) }} {{ row.diseaseType || '未分类' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="诊断/随访日期" width="130">
          <template #default="{ row }">{{ row.diagnosisDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="核心指标摘要" min-width="220">
          <template #default="{ row }">
            <div class="metrics-cell">
              <span>{{ row.metricsSummary }}</span>
              <span v-if="row.abnormalMetrics?.length" class="abnormal-tags">
                <el-tag
                  v-for="item in row.abnormalMetrics"
                  :key="item"
                  type="danger"
                  size="small"
                  effect="plain"
                >
                  ↑ {{ item }}
                </el-tag>
              </span>
              <el-tag
                v-else-if="row.metricsSummary !== '暂无指标'"
                type="success"
                size="small"
                effect="plain"
              >
                ↓ 正常
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="doctor" label="主治医生/村医" width="120" />
        <el-table-column label="下次随访" width="130">
          <template #default="{ row }">
            <span :class="{ overdue: row.followUpOverdue }">
              {{ row.nextFollowUpDate || '-' }}
              <el-tag v-if="row.followUpOverdue" type="danger" size="small">逾期</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="控制情况" width="120">
          <template #default="{ row }">
            <el-tag :type="controlTagType(row.controlLevel)" size="small">
              {{ controlLabel(row.controlLevel) }}
            </el-tag>
            <div class="control-source">
              {{ row.controlLevelSource === 'manual' ? '医生标记' : '系统辅助' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openForm('view', row)">查看详情</el-button>
            <el-button type="warning" link @click="openForm('edit', row)">编辑</el-button>
            <el-button v-if="isAdmin" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        :layout="isMobile ? 'total, prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
        class="pager"
        :class="{ 'pager-mobile': isMobile }"
        @size-change="fetchRecords"
        @current-change="fetchRecords"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :width="isMobile ? '100%' : '760px'"
      :fullscreen="isMobile"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        :disabled="dialogMode === 'view'"
      >
        <el-divider content-position="left">基础关联</el-divider>
        <el-form-item label="患者姓名" prop="patientId">
          <el-select
            v-model="form.patientId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索患者姓名或电话"
            :remote-method="searchPatients"
            :loading="patientLoading"
            style="width: 100%"
            @change="handlePatientChange"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.id"
              :label="`${item.name} (${item.phone})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-row v-if="selectedPatient" :gutter="12">
          <el-col :span="8"
            ><el-form-item label="性别"
              ><span>{{ selectedPatient.gender }}</span></el-form-item
            ></el-col
          >
          <el-col :span="8"
            ><el-form-item label="年龄"
              ><span>{{ selectedPatient.age ?? '-' }}</span></el-form-item
            ></el-col
          >
          <el-col :span="8"
            ><el-form-item label="身份证"
              ><span>{{ selectedPatient.idCard || '-' }}</span></el-form-item
            ></el-col
          >
        </el-row>

        <el-divider content-position="left">诊断核心信息</el-divider>
        <el-form-item label="疾病类型" prop="diseaseType">
          <el-select v-model="form.diseaseType" placeholder="请选择疾病类型" style="width: 100%">
            <el-option v-for="item in diseaseOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="诊断日期" prop="diagnosisDate">
          <el-date-picker
            v-model="form.diagnosisDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="当前症状">
          <el-select
            v-model="form.symptoms"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入症状"
            style="width: 100%"
          >
            <el-option v-for="item in symptomOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="体征测量">
          <div class="vitals-grid">
            <template v-if="showBloodPressure">
              <el-input-number
                v-model="form.systolicBp"
                :min="60"
                :max="260"
                placeholder="收缩压"
              />
              <span class="unit">收缩压 mmHg</span>
              <el-input-number
                v-model="form.diastolicBp"
                :min="40"
                :max="160"
                placeholder="舒张压"
              />
              <span class="unit">舒张压 mmHg</span>
            </template>
            <template v-if="showGlucose">
              <el-input-number
                v-model="form.fastingGlucose"
                :min="0"
                :max="30"
                :step="0.1"
                :precision="1"
              />
              <span class="unit">空腹血糖 mmol/L</span>
              <el-input-number
                v-model="form.postprandialGlucose"
                :min="0"
                :max="30"
                :step="0.1"
                :precision="1"
              />
              <span class="unit">餐后2h mmol/L</span>
              <el-input-number v-model="form.hba1c" :min="0" :max="20" :step="0.1" :precision="1" />
              <span class="unit">糖化血红蛋白 %</span>
            </template>
            <el-input-number
              v-model="form.height"
              :min="50"
              :max="250"
              :step="0.1"
              :precision="1"
            />
            <span class="unit">身高 cm</span>
            <el-input-number
              v-model="form.weight"
              :min="10"
              :max="300"
              :step="0.1"
              :precision="1"
            />
            <span class="unit">体重 kg</span>
            <el-input-number v-model="form.heartRate" :min="30" :max="220" />
            <span class="unit">心率 次/分</span>
            <span v-if="computedBmi" class="bmi-text">BMI：{{ computedBmi }}</span>
          </div>
        </el-form-item>

        <el-form-item label="用药情况">
          <el-input
            v-model="form.medication"
            type="textarea"
            :rows="2"
            placeholder="如：盐酸二甲双胍片，0.5g，每日2次"
          />
        </el-form-item>

        <el-divider content-position="left">诊断结论与计划</el-divider>
        <el-form-item label="控制情况">
          <el-radio-group v-model="form.manualControlLevel" :disabled="dialogMode === 'view'">
            <el-radio label="">系统辅助判断</el-radio>
            <el-radio label="controlled">达标</el-radio>
            <el-radio label="warning">预警</el-radio>
            <el-radio label="high">高危</el-radio>
          </el-radio-group>
          <div class="control-hint">
            医生手动标记优先；选「系统辅助」时按血压/血糖等指标自动判定。
            <span v-if="systemHintLabel">当前系统建议：{{ systemHintLabel }}</span>
          </div>
        </el-form-item>
        <el-form-item label="诊断结论">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="3"
            placeholder="简要描述病情变化"
          />
        </el-form-item>
        <el-form-item label="健康指导">
          <el-select
            v-model="form.healthGuidance"
            multiple
            placeholder="请选择健康指导"
            style="width: 100%"
          >
            <el-option v-for="item in guidanceOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="下次随访">
          <el-date-picker
            v-model="form.nextFollowUpDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="转诊建议">
          <el-input v-model="form.referralHospital" placeholder="如有转诊，填写上级医院名称" />
        </el-form-item>
        <el-form-item label="主治医生">
          <el-input v-model="form.doctor" placeholder="请输入村医/医生姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{
          dialogMode === 'view' ? '关闭' : '取消'
        }}</el-button>
        <el-button
          v-if="dialogMode !== 'view'"
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, onBeforeUnmount, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import {
  getDiagnoses,
  getDiagnosisStats,
  getDiagnosisById,
  createDiagnosis,
  updateDiagnosis,
  deleteDiagnosis,
} from '@/api/diagnosis';
import { getPatients } from '@/api/patient';
import { useMobile } from '@/composables/useMobile';

const route = useRoute();
const store = useStore();
const { isMobile } = useMobile();
const mobileActions = inject('mobileActions', null);
const isAdmin = computed(() => store.getters.isAdmin);

const loading = ref(false);
const submitting = ref(false);
const patientLoading = ref(false);
const records = ref([]);
const total = ref(0);
const stats = reactive({ todayCount: 0, monthCount: 0, abnormalCount: 0 });
const dateRange = ref([]);
const dialogVisible = ref(false);
const dialogMode = ref('create');
const formRef = ref(null);
const patientOptions = ref([]);
const selectedPatient = ref(null);

const diseaseOptions = ['高血压', '糖尿病', 'Ⅱ型糖尿病', '慢阻肺', '冠心病'];
const symptomOptions = ['头晕', '多饮', '多尿', '胸闷', '气短', '乏力', '无症状'];
const guidanceOptions = ['低盐饮食', '适量运动', '戒烟限酒', '心理调节', '规律服药', '控制体重'];

const searchForm = reactive({
  page: 1,
  size: 20,
  diseaseType: '',
  keyword: '',
  onlyOverdue: false,
});

const defaultForm = () => ({
  id: null,
  patientId: null,
  diseaseType: '',
  diagnosisDate: new Date().toISOString().slice(0, 10),
  symptoms: [],
  systolicBp: null,
  diastolicBp: null,
  fastingGlucose: null,
  postprandialGlucose: null,
  hba1c: null,
  height: null,
  weight: null,
  heartRate: null,
  medication: '',
  diagnosis: '',
  healthGuidance: [],
  nextFollowUpDate: '',
  referralHospital: '',
  doctor: '',
  manualControlLevel: '',
  remark: '',
});

const form = reactive(defaultForm());

const rules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  diseaseType: [{ required: true, message: '请选择疾病类型', trigger: 'change' }],
  diagnosisDate: [{ required: true, message: '请选择诊断日期', trigger: 'change' }],
};

const dialogTitle = computed(
  () =>
    ({
      create: '新增诊断记录',
      edit: '编辑诊断记录',
      view: '诊断记录详情',
    })[dialogMode.value]
);

const showBloodPressure = computed(() => ['高血压', '冠心病'].includes(form.diseaseType));
const showGlucose = computed(() => ['糖尿病', 'Ⅱ型糖尿病'].includes(form.diseaseType));

const computedBmi = computed(() => {
  if (!form.height || !form.weight || form.height <= 0) return null;
  const heightMeter = form.height / 100;
  return Math.round((form.weight / (heightMeter * heightMeter)) * 10) / 10;
});

const controlLabel = (level) =>
  ({
    high: '高危',
    warning: '预警',
    controlled: '达标',
  })[level] || '未评估';

const controlTagType = (level) =>
  ({
    high: 'danger',
    warning: 'warning',
    controlled: 'success',
  })[level] || 'info';

const buildAbnormalHints = () => {
  const items = [];
  if (form.systolicBp != null && form.systolicBp >= 140) items.push('收缩压');
  if (form.diastolicBp != null && form.diastolicBp >= 90) items.push('舒张压');
  if (form.fastingGlucose != null && form.fastingGlucose >= 7) items.push('空腹血糖');
  if (form.postprandialGlucose != null && form.postprandialGlucose >= 11.1) items.push('餐后血糖');
  if (form.hba1c != null && form.hba1c >= 7) items.push('HbA1c');
  if (form.heartRate != null && (form.heartRate < 60 || form.heartRate > 100)) items.push('心率');
  if (computedBmi.value != null && (computedBmi.value >= 28 || computedBmi.value < 18.5))
    items.push('BMI');
  return items;
};

const systemHintLabel = computed(() => {
  const abnormal = buildAbnormalHints();
  const severe =
    (form.systolicBp != null && form.systolicBp >= 160) ||
    (form.diastolicBp != null && form.diastolicBp >= 100) ||
    (form.fastingGlucose != null && form.fastingGlucose >= 10) ||
    (form.hba1c != null && form.hba1c >= 8);
  if (severe || abnormal.length >= 2) return '高危';
  if (abnormal.length > 0) return '预警';
  if (form.systolicBp != null || form.fastingGlucose != null || form.hba1c != null) return '达标';
  return '';
});

const diseaseTagType = (type) =>
  ({
    高血压: 'danger',
    糖尿病: 'warning',
    Ⅱ型糖尿病: 'warning',
    慢阻肺: 'info',
    冠心病: 'primary',
  })[type] || 'info';

const diseaseIcon = (type) =>
  ({
    高血压: '🔴',
    糖尿病: '🟡',
    Ⅱ型糖尿病: '🟡',
    慢阻肺: '🔵',
    冠心病: '🟣',
  })[type] || '⚪';

const fetchStats = async () => {
  try {
    const res = await getDiagnosisStats();
    Object.assign(stats, res.data);
  } catch (error) {
    console.error('获取统计失败:', error);
  }
};

const fetchRecords = async () => {
  loading.value = true;
  try {
    const params = {
      ...searchForm,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
    };
    const res = await getDiagnoses(params);
    records.value = res.data.records;
    total.value = res.data.total;
  } catch (error) {
    console.error('获取诊断记录失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  searchForm.page = 1;
  fetchRecords();
};

const handleReset = () => {
  searchForm.diseaseType = '';
  searchForm.keyword = '';
  searchForm.onlyOverdue = false;
  dateRange.value = [];
  handleSearch();
};

const searchPatients = async (query) => {
  if (!query) {
    patientOptions.value = [];
    return;
  }
  patientLoading.value = true;
  try {
    const res = await getPatients({ page: 1, size: 20, name: query });
    patientOptions.value = res.data.records;
  } catch (error) {
    console.error('搜索患者失败:', error);
  } finally {
    patientLoading.value = false;
  }
};

const handlePatientChange = (patientId) => {
  selectedPatient.value = patientOptions.value.find((item) => item.id === patientId) || null;
};

const fillForm = (data) => {
  Object.assign(form, defaultForm(), {
    ...data,
    symptoms: data.symptoms || [],
    healthGuidance: data.healthGuidance || [],
    manualControlLevel: data.manualControlLevel || '',
  });
  if (data.patientId) {
    patientOptions.value = [
      {
        id: data.patientId,
        name: data.patientName,
        phone: data.patientPhone,
        gender: data.patientGender,
        age: data.patientAge,
        idCard: data.patientIdCard,
      },
    ];
    selectedPatient.value = patientOptions.value[0];
  }
};

const openForm = async (mode, row) => {
  dialogMode.value = mode;
  if (row?.id) {
    try {
      const res = await getDiagnosisById(row.id);
      fillForm(res.data);
    } catch (error) {
      console.error('获取详情失败:', error);
      return;
    }
  } else {
    resetForm();
    form.nextFollowUpDate = getDefaultFollowUpDate();
  }
  dialogVisible.value = true;
};

const getDefaultFollowUpDate = () => {
  const date = new Date();
  date.setDate(date.getDate() + 30);
  return date.toISOString().slice(0, 10);
};

const resetForm = () => {
  Object.assign(form, defaultForm());
  selectedPatient.value = null;
  patientOptions.value = [];
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const payload = { ...form };
      if (!payload.nextFollowUpDate) {
        payload.nextFollowUpDate = getDefaultFollowUpDate();
      }
      if (dialogMode.value === 'edit') {
        await updateDiagnosis(form.id, payload);
        ElMessage.success('更新成功');
      } else {
        await createDiagnosis(payload);
        ElMessage.success('创建成功');
      }
      dialogVisible.value = false;
      store.dispatch('notifyHomeRefresh');
      fetchRecords();
      fetchStats();
    } catch (error) {
      console.error('保存失败:', error);
    } finally {
      submitting.value = false;
    }
  });
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除记录 #${row.id} 吗？`, '提示', { type: 'warning' });
    await deleteDiagnosis(row.id);
    ElMessage.success('删除成功');
    store.dispatch('notifyHomeRefresh');
    fetchRecords();
    fetchStats();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};

onMounted(async () => {
  if (route.query.overdue === '1' || route.query.overdue === 'true') {
    searchForm.onlyOverdue = true;
  }
  if (mobileActions) {
    mobileActions.openAdd = () => openForm('create');
  }
  fetchStats();
  await fetchRecords();
  if (route.query.create === '1') {
    openForm('create');
  }
  if (route.query.patientId) {
    const patientId = Number(route.query.patientId);
    if (!Number.isNaN(patientId)) {
      openForm('create', { patientId });
      try {
        const res = await getPatients({ page: 1, size: 1, ids: String(patientId) });
        const patient = res.data?.records?.[0];
        if (patient) {
          patientOptions.value = [patient];
          selectedPatient.value = patient;
          form.patientId = patient.id;
          if (patient.diseaseType) form.diseaseType = patient.diseaseType;
        }
      } catch (error) {
        console.error('预选患者失败:', error);
      }
    }
  }
});

onBeforeUnmount(() => {
  if (mobileActions) {
    mobileActions.openAdd = null;
  }
});
</script>

<style scoped>
.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  margin-top: 8px;
  color: #909399;
}

.stat-card.today .stat-value {
  color: #409eff;
}
.stat-card.month .stat-value {
  color: #67c23a;
}
.stat-card.abnormal .stat-value {
  color: #f56c6c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.metrics-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.abnormal-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.overdue {
  color: #f56c6c;
  font-weight: 600;
}

.control-source {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.control-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.vitals-grid {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 10px 12px;
  align-items: center;
  width: 100%;
}

.unit {
  color: #909399;
  font-size: 13px;
}

.bmi-text {
  grid-column: span 2;
  color: #409eff;
  font-weight: 500;
}

.pager {
  margin-top: 20px;
  justify-content: flex-end;
}

.m-stats,
.m-filter-bar,
.m-list,
.dx-card {
  display: none;
}

.diagnosis-page.is-mobile .m-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.diagnosis-page.is-mobile .m-stat {
  background: #fff;
  border-radius: 12px;
  padding: 12px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.diagnosis-page.is-mobile .m-stat strong {
  display: block;
  font-size: 22px;
  color: #1c1c1e;
}

.diagnosis-page.is-mobile .m-stat span {
  font-size: 12px;
  color: #8e8e93;
}

.diagnosis-page.is-mobile .m-stat.danger strong {
  color: #ff3b30;
}

.diagnosis-page.is-mobile .m-filter-bar {
  display: grid;
  grid-template-columns: 1fr 100px auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.diagnosis-page.is-mobile .m-card-flat {
  border: none;
  background: transparent;
  box-shadow: none !important;
}

.diagnosis-page.is-mobile .m-card-flat :deep(.el-card__body) {
  padding: 0 !important;
}

.diagnosis-page.is-mobile .m-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.diagnosis-page.is-mobile .dx-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  min-height: 90px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

.dx-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
}

.dx-name {
  font-size: 17px;
  font-weight: 700;
  color: #1c1c1e;
}

.dx-disease,
.dx-date {
  font-size: 15px;
  color: #8e8e93;
}

.dx-metrics,
.dx-med {
  font-size: 15px;
  color: #1c1c1e;
}

.dx-med {
  color: #8e8e93;
}

.dx-follow {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #aeaeb2;
}

.overdue-tag {
  color: #ff3b30;
  font-weight: 600;
}

.dx-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.pager-mobile {
  justify-content: center !important;
}

.m-empty {
  padding: 24px 0;
}

@media (max-width: 430px) {
  .vitals-grid {
    grid-template-columns: 1fr;
  }

  .bmi-text {
    grid-column: span 1;
  }
}
</style>
