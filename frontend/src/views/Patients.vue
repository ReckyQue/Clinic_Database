<template>
  <div class="patients" :class="{ 'is-mobile': isMobile }">
    <!-- 移动端：搜索 + 筛选 -->
    <div v-if="isMobile" class="m-toolbar">
      <el-input
        v-model="mobileKeyword"
        class="m-search"
        clearable
        placeholder="搜索姓名或电话"
        @keyup.enter="applyMobileSearch"
        @clear="applyMobileSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-badge :is-dot="activeFilterCount > 0 || focusHighRisk" class="filter-badge">
        <button type="button" class="m-filter-btn pressable" @click="openDrawer">
          <el-icon :size="20"><Filter /></el-icon>
        </button>
      </el-badge>
    </div>

    <div v-if="isMobile && (isAdmin || isMember)" class="m-export-bar">
      <span class="selected-count">已选 {{ selectedIds.size }}</span>
      <el-button size="small" :disabled="!selectedIds.size" @click="clearSelection">清空</el-button>
      <el-button size="small" type="primary" plain @click="openExportDialog">导出选中</el-button>
    </div>

    <el-card shadow="hover" :class="{ 'm-card-flat': isMobile }">
      <template v-if="!isMobile" #header>
        <div class="card-header">
          <span>患者列表</span>
          <div class="header-actions">
            <el-badge :is-dot="activeFilterCount > 0" class="filter-badge">
              <el-button @click="openDrawer">
                <el-icon><Filter /></el-icon> 筛选
              </el-button>
            </el-badge>
            <span v-if="isAdmin || isMember" class="selected-count"
              >已选择 {{ selectedIds.size }} 条</span
            >
            <el-button
              v-if="isAdmin || isMember"
              :disabled="selectedIds.size === 0"
              @click="clearSelection"
            >
              清空选择
            </el-button>
            <el-button v-if="isAdmin || isMember" @click="openExportDialog">
              <el-icon><Download /></el-icon> 导出选中
            </el-button>
            <el-button
              v-if="isAdmin || isMember"
              type="primary"
              @click="$router.push('/patients/add')"
            >
              <el-icon><Plus /></el-icon> 添加患者
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="activeTags.length || focusHighRisk" class="filter-tags">
        <span class="tags-label">筛选条件：</span>
        <el-tag
          v-if="focusHighRisk"
          closable
          type="danger"
          effect="plain"
          class="filter-tag"
          @close="clearFocusFilter"
        >
          控制情况：高危
        </el-tag>
        <el-tag
          v-for="tag in activeTags"
          :key="tag.key"
          closable
          type="info"
          effect="plain"
          class="filter-tag"
          @close="removeTag(tag.key)"
        >
          {{ tag.label }}
        </el-tag>
        <el-button link type="primary" @click="clearAllFilters">清空全部筛选</el-button>
      </div>

      <!-- 移动端卡片列表 -->
      <div v-if="isMobile" v-loading="loading" class="m-list">
        <div v-if="!loading && !patients.length" class="m-empty">
          <el-empty
            :description="
              activeFilterCount || mobileKeyword ? '未找到匹配患者' : '暂无患者，点击右上角添加'
            "
          />
        </div>
        <article
          v-for="row in patients"
          :key="row.id"
          class="patient-card pressable"
          @click="$router.push(`/patients/${row.id}`)"
        >
          <div class="pc-top">
            <div class="pc-main">
              <div class="pc-name-row">
                <label v-if="isAdmin || isMember" class="pc-check" @click.stop>
                  <el-checkbox
                    :model-value="selectedIds.has(row.id)"
                    @change="(val) => toggleMobileSelect(row, val)"
                  />
                </label>
                <span class="pc-name">{{ row.name }}</span>
                <span class="pc-meta">{{ row.age ?? '-' }}岁</span>
                <span class="pc-dot" :class="diseaseDotClass(row.diseaseType)" />
                <span class="pc-disease">{{ row.diseaseType || '未分类' }}</span>
              </div>
              <a v-if="row.phone" class="pc-phone" :href="`tel:${row.phone}`" @click.stop
                >📞 {{ row.phone }}</a
              >
              <div class="pc-date">📅 收录：{{ formatDateShort(row.createTime) }}</div>
            </div>
          </div>
          <div class="pc-actions" @click.stop>
            <el-button type="primary" link @click="$router.push(`/patients/${row.id}`)"
              >详情 →</el-button
            >
          </div>
        </article>
      </div>

      <div v-else class="table-wrap">
        <el-table
          ref="tableRef"
          v-loading="loading"
          :data="patients"
          row-key="id"
          style="width: 100%; min-width: 900px"
          @selection-change="handleSelectionChange"
        >
          <el-table-column
            v-if="isAdmin || isMember"
            type="selection"
            width="55"
            reserve-selection
          />
          <el-table-column prop="name" label="姓名" min-width="90" />
          <el-table-column prop="gender" label="性别" width="70" />
          <el-table-column prop="age" label="年龄" width="70" />
          <el-table-column prop="phone" label="电话" min-width="120" />
          <el-table-column prop="idCard" label="身份证号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="address" label="地区" min-width="140" show-overflow-tooltip />
          <el-table-column prop="diseaseType" label="疾病类型" min-width="110" />
          <el-table-column prop="createTime" label="收录时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="$router.push(`/patients/${row.id}`)"
                >详情</el-button
              >
              <el-button
                v-if="isAdmin || isMember"
                type="success"
                link
                @click="$router.push({ path: '/diagnosis', query: { patientId: row.id } })"
              >
                诊断
              </el-button>
              <el-button
                v-if="isAdmin || isMember"
                type="warning"
                link
                @click="$router.push(`/patients/edit/${row.id}`)"
              >
                编辑
              </el-button>
              <el-button v-if="isAdmin" type="danger" link @click="handleDelete(row)"
                >删除</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-model:current-page="pager.page"
        v-model:page-size="pager.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        :layout="isMobile ? 'total, prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
        class="pager"
        :class="{ 'pager-mobile': isMobile }"
        @size-change="fetchPatients"
        @current-change="fetchPatients"
      />
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      title="筛选条件"
      :size="isMobile ? '78%' : '420px'"
      :direction="isMobile ? 'btt' : 'rtl'"
      class="filter-drawer"
    >
      <div class="drawer-body">
        <div class="section">
          <div class="section-title">基本信息</div>
          <el-form label-position="top">
            <el-form-item label="姓名">
              <el-input v-model="draft.name" placeholder="支持模糊搜索，如输入“张”" clearable />
            </el-form-item>
            <el-form-item label="身份证号">
              <el-input v-model="draft.idCard" placeholder="可只输入后几位" clearable />
            </el-form-item>
            <el-form-item label="所属地区">
              <el-input
                v-model="draft.address"
                placeholder="请输入村/镇名称，如“王家坝村”"
                clearable
              />
            </el-form-item>
            <el-form-item label="电话">
              <el-input v-model="draft.phone" placeholder="可选" clearable />
            </el-form-item>
            <el-form-item label="疾病类型">
              <el-input v-model="draft.diseaseType" placeholder="可选" clearable />
            </el-form-item>
          </el-form>
        </div>

        <div class="section">
          <div class="section-title">时间范围</div>
          <el-form label-position="top">
            <el-form-item label="收录时间（创建时间）">
              <el-date-picker
                v-model="draft.createRange"
                type="daterange"
                range-separator="~"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <div class="quick-row">
                <el-button size="small" @click="setCreateQuick('today')">今天</el-button>
                <el-button size="small" @click="setCreateQuick('7d')">近7天</el-button>
                <el-button size="small" @click="setCreateQuick('30d')">近30天</el-button>
                <el-button size="small" @click="setCreateQuick('month')">本月</el-button>
              </div>
            </el-form-item>
            <el-form-item label="诊断时间（最后一次诊断）">
              <el-date-picker
                v-model="draft.diagnosisRange"
                type="daterange"
                range-separator="~"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="draft.onlyWithDiagnosis">仅显示有诊断记录的患者</el-checkbox>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button size="large" class="footer-btn" @click="resetDraft">重置</el-button>
          <el-button type="success" size="large" class="footer-btn" @click="applyFilters"
            >确定</el-button
          >
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="exportVisible" title="导出数据" width="640px" destroy-on-close>
      <div class="export-tip">
        将导出已勾选的 <strong>{{ selectedIds.size }}</strong> 位患者
      </div>
      <div class="export-hint">
        勾选患者后导出；翻页后勾选会保留。也可配合上方筛选缩小列表再勾选。
      </div>

      <div class="field-panel">
        <el-checkbox v-model="checkAll" :indeterminate="isIndeterminate" @change="handleCheckAll">
          全选
        </el-checkbox>

        <div class="field-group">
          <div class="group-title">患者基础信息</div>
          <el-checkbox-group v-model="exportForm.fields">
            <el-checkbox v-for="item in basicFields" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="field-group">
          <div class="group-title">诊疗与病情（诊断记录）</div>
          <el-checkbox-group v-model="exportForm.fields">
            <el-checkbox v-for="item in clinicalFields" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="field-group">
          <div class="group-title">管理时间轴</div>
          <el-checkbox-group v-model="exportForm.fields">
            <el-checkbox v-for="item in timelineFields" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="field-group">
          <div class="group-title">病情综合评估</div>
          <el-checkbox-group v-model="exportForm.fields">
            <el-checkbox v-for="item in assessFields" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <el-divider />
        <el-checkbox v-model="exportForm.exportAllHistory">
          导出全部历史诊断记录（开启后一人可能多行）
        </el-checkbox>
      </div>

      <el-form label-width="80px" style="margin-top: 12px">
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio label="xlsx">Excel (.xlsx)</el-radio>
            <el-radio label="csv">CSV (.csv)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="文件名">
          <el-input v-model="exportForm.filename" placeholder="可自定义，留空则自动生成" />
        </el-form-item>
        <el-form-item label="隐私">
          <el-checkbox v-model="exportForm.maskSensitive">对电话/身份证进行脱敏处理</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="success" :loading="exporting" @click="handleExport">开始导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, nextTick, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Download, Filter, Plus, Search } from '@element-plus/icons-vue';
import request from '@/utils/request';
import { exportFiltered } from '@/api/export';
import { useMobile } from '@/composables/useMobile';

const FILTER_STORAGE_KEY = 'patient_filter_prefs';

const route = useRoute();
const router = useRouter();
const store = useStore();
const { isMobile } = useMobile();
const mobileActions = inject('mobileActions', null);
const mobileKeyword = ref('');
const loading = ref(false);
const patients = ref([]);
const total = ref(0);
const drawerVisible = ref(false);
const exportVisible = ref(false);
const exporting = ref(false);
const checkAll = ref(true);
const isIndeterminate = ref(false);
const tableRef = ref(null);
const selectedIds = ref(new Set());
const restoringSelection = ref(false);
const focusIds = ref('');
const focusHighRisk = ref(false);

const basicFields = [
  { label: '姓名', value: 'name' },
  { label: '性别', value: 'gender' },
  { label: '年龄', value: 'age' },
  { label: '电话', value: 'phone' },
  { label: '身份证', value: 'idCard' },
  { label: '地址', value: 'address' },
];
const clinicalFields = [
  { label: '疾病类型', value: 'diseaseType' },
  { label: '最近诊断日期', value: 'lastDiagnosisDate' },
  { label: '症状', value: 'symptoms' },
  { label: '用药', value: 'medication' },
  { label: '健康指导', value: 'healthGuidance' },
  { label: '血压/血糖值', value: 'bpGlucose' },
];
const timelineFields = [
  { label: '收录时间', value: 'createTime' },
  { label: '下次随访日期', value: 'nextFollowUpDate' },
  { label: '主治村医', value: 'doctor' },
];
const assessFields = [{ label: '控制情况（达标/预警/高危）', value: 'controlStatus' }];
const allFieldValues = [...basicFields, ...clinicalFields, ...timelineFields, ...assessFields].map(
  (item) => item.value
);

const defaultExportFields = [...allFieldValues];

const exportForm = reactive({
  fields: [...defaultExportFields],
  exportAllHistory: false,
  format: 'xlsx',
  filename: '',
  maskSensitive: true,
});

const todayFileName = () => {
  const d = new Date();
  const day = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  return `乡村慢病_患者列表_${day}`;
};

const isAdmin = computed(() => store.getters.isAdmin);
const isMember = computed(() => store.getters.isMember);

const emptyFilters = () => ({
  name: '',
  phone: '',
  idCard: '',
  address: '',
  diseaseType: '',
  createRange: null,
  diagnosisRange: null,
  onlyWithDiagnosis: false,
});

const applied = reactive(emptyFilters());
const draft = reactive(emptyFilters());
const pager = reactive({ page: 1, size: 20 });

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('zh-CN');
};

const formatDateShort = (dateStr) => {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return String(dateStr).slice(0, 10);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

const diseaseDotClass = (type) => {
  if (!type) return 'dot-other';
  if (type.includes('高血压')) return 'dot-bp';
  if (type.includes('糖尿')) return 'dot-dm';
  if (type.includes('慢阻肺')) return 'dot-copd';
  if (type.includes('冠心')) return 'dot-chd';
  return 'dot-other';
};

const applyMobileSearch = () => {
  const kw = (mobileKeyword.value || '').trim();
  if (/^\d{3,}/.test(kw)) {
    applied.name = '';
    applied.phone = kw;
  } else {
    applied.phone = '';
    applied.name = kw;
  }
  pager.page = 1;
  saveFilters();
  fetchPatients();
};

const toggleMobileSelect = (row, checked) => {
  const next = new Set(selectedIds.value);
  if (checked) next.add(row.id);
  else next.delete(row.id);
  selectedIds.value = next;
};

const formatDay = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const copyFilters = (from, to) => {
  to.name = from.name || '';
  to.phone = from.phone || '';
  to.idCard = from.idCard || '';
  to.address = from.address || '';
  to.diseaseType = from.diseaseType || '';
  to.createRange = from.createRange ? [...from.createRange] : null;
  to.diagnosisRange = from.diagnosisRange ? [...from.diagnosisRange] : null;
  to.onlyWithDiagnosis = !!from.onlyWithDiagnosis;
};

const saveFilters = () => {
  localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(applied));
};

const loadFilters = () => {
  try {
    const raw = localStorage.getItem(FILTER_STORAGE_KEY);
    if (!raw) return;
    const parsed = JSON.parse(raw);
    copyFilters(parsed, applied);
  } catch (error) {
    console.error('读取筛选偏好失败:', error);
  }
};

const activeTags = computed(() => {
  const tags = [];
  if (applied.name) tags.push({ key: 'name', label: `姓名：${applied.name}` });
  if (applied.idCard) tags.push({ key: 'idCard', label: `身份证：${applied.idCard}` });
  if (applied.address) tags.push({ key: 'address', label: `地区：${applied.address}` });
  if (applied.phone) tags.push({ key: 'phone', label: `电话：${applied.phone}` });
  if (applied.diseaseType) tags.push({ key: 'diseaseType', label: `疾病：${applied.diseaseType}` });
  if (applied.createRange?.length === 2) {
    tags.push({
      key: 'createRange',
      label: `收录时间：${applied.createRange[0]} ~ ${applied.createRange[1]}`,
    });
  }
  if (applied.diagnosisRange?.length === 2) {
    tags.push({
      key: 'diagnosisRange',
      label: `诊断时间：${applied.diagnosisRange[0]} ~ ${applied.diagnosisRange[1]}`,
    });
  }
  if (applied.onlyWithDiagnosis) {
    tags.push({ key: 'onlyWithDiagnosis', label: '仅有诊断记录' });
  }
  return tags;
});

const activeFilterCount = computed(() => activeTags.value.length);

const buildQueryParams = () => {
  const params = {
    page: pager.page,
    size: pager.size,
  };
  if (applied.name) params.name = applied.name;
  if (applied.phone) params.phone = applied.phone;
  if (applied.idCard) params.idCard = applied.idCard;
  if (applied.address) params.address = applied.address;
  if (applied.diseaseType) params.diseaseType = applied.diseaseType;
  if (applied.createRange?.length === 2) {
    params.createStart = applied.createRange[0];
    params.createEnd = applied.createRange[1];
  }
  if (applied.diagnosisRange?.length === 2) {
    params.diagnosisStart = applied.diagnosisRange[0];
    params.diagnosisEnd = applied.diagnosisRange[1];
  }
  if (applied.onlyWithDiagnosis) {
    params.onlyWithDiagnosis = true;
  }
  if (focusIds.value) {
    params.ids = focusIds.value;
  }
  return params;
};

const restoreSelection = () => {
  if (!tableRef.value) return;
  restoringSelection.value = true;
  nextTick(() => {
    patients.value.forEach((row) => {
      tableRef.value.toggleRowSelection(row, selectedIds.value.has(row.id));
    });
    restoringSelection.value = false;
  });
};

const fetchPatients = async () => {
  loading.value = true;
  try {
    const res = await request.get('/patients', { params: buildQueryParams() });
    patients.value = res.data.records;
    total.value = res.data.total;
    restoreSelection();
  } catch (error) {
    console.error('获取患者列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleSelectionChange = (selection) => {
  if (restoringSelection.value) return;
  const next = new Set(selectedIds.value);
  const currentPageIds = patients.value.map((item) => item.id);
  currentPageIds.forEach((id) => next.delete(id));
  selection.forEach((row) => next.add(row.id));
  selectedIds.value = next;
};

const clearSelection = () => {
  selectedIds.value = new Set();
  tableRef.value?.clearSelection();
};

const openDrawer = () => {
  copyFilters(applied, draft);
  drawerVisible.value = true;
};

const setCreateQuick = (type) => {
  const today = new Date();
  const end = formatDay(today);
  let startDate = new Date(today);

  if (type === 'today') {
    // keep today
  } else if (type === '7d') {
    startDate.setDate(today.getDate() - 6);
  } else if (type === '30d') {
    startDate.setDate(today.getDate() - 29);
  } else if (type === 'month') {
    startDate = new Date(today.getFullYear(), today.getMonth(), 1);
  }

  draft.createRange = [formatDay(startDate), end];
};

const resetDraft = () => {
  copyFilters(emptyFilters(), draft);
};

const applyFilters = () => {
  copyFilters(draft, applied);
  saveFilters();
  pager.page = 1;
  drawerVisible.value = false;
  fetchPatients();
};

const removeTag = (key) => {
  if (key === 'createRange' || key === 'diagnosisRange') {
    applied[key] = null;
  } else if (key === 'onlyWithDiagnosis') {
    applied.onlyWithDiagnosis = false;
  } else {
    applied[key] = '';
  }
  saveFilters();
  pager.page = 1;
  fetchPatients();
};

const clearFocusFilter = () => {
  focusIds.value = '';
  focusHighRisk.value = false;
  pager.page = 1;
  fetchPatients();
};

const clearAllFilters = () => {
  copyFilters(emptyFilters(), applied);
  focusIds.value = '';
  focusHighRisk.value = false;
  saveFilters();
  pager.page = 1;
  fetchPatients();
};

const openExportDialog = () => {
  if (selectedIds.value.size === 0) {
    ElMessage.warning('请先勾选需要导出的患者');
    return;
  }
  exportForm.fields = [...defaultExportFields];
  exportForm.exportAllHistory = false;
  exportForm.format = 'xlsx';
  exportForm.filename = todayFileName();
  exportForm.maskSensitive = true;
  checkAll.value = true;
  isIndeterminate.value = false;
  exportVisible.value = true;
};

const handleCheckAll = (val) => {
  exportForm.fields = val ? [...allFieldValues] : [];
  isIndeterminate.value = false;
};

watch(
  () => exportForm.fields,
  (val) => {
    const checkedCount = val.length;
    checkAll.value = checkedCount === allFieldValues.length;
    isIndeterminate.value = checkedCount > 0 && checkedCount < allFieldValues.length;
  }
);

const buildExportPayload = () => ({
  ids: [...selectedIds.value],
  fields: exportForm.fields,
  exportAllHistory: exportForm.exportAllHistory,
  format: exportForm.format,
  filename: exportForm.filename || todayFileName(),
  maskSensitive: exportForm.maskSensitive,
});

const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};

const handleExport = async () => {
  if (!exportForm.fields.length) {
    ElMessage.warning('请至少选择一个导出字段');
    return;
  }
  if (selectedIds.value.size === 0) {
    ElMessage.warning('请先勾选需要导出的患者');
    return;
  }

  exporting.value = true;
  try {
    const payload = buildExportPayload();
    const blob = await exportFiltered(payload);
    const ext = payload.format === 'csv' ? '.csv' : '.xlsx';
    const name = (payload.filename || todayFileName()).replace(/\.(xlsx|csv)$/i, '') + ext;
    downloadBlob(blob, name);
    ElMessage.success(`已导出 ${selectedIds.value.size} 位患者数据`);
    exportVisible.value = false;
  } catch (error) {
    console.error('导出失败:', error);
  } finally {
    exporting.value = false;
  }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除患者 ${row.name} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await request.delete(`/patients/${row.id}`);
    ElMessage.success('删除成功');
    store.dispatch('notifyHomeRefresh');
    fetchPatients();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};

onMounted(() => {
  loadFilters();
  mobileKeyword.value = applied.name || applied.phone || '';
  if (route.query.ids) {
    focusIds.value = String(route.query.ids);
  }
  if (route.query.controlStatus === 'high') {
    focusHighRisk.value = true;
  }
  if (mobileActions) {
    mobileActions.openSearch = () => {
      const el = document.querySelector('.patients .m-search input');
      el?.focus();
    };
    mobileActions.openAdd = () => router.push('/patients/add');
  }
  fetchPatients();
});

onBeforeUnmount(() => {
  if (mobileActions) {
    mobileActions.openSearch = null;
    mobileActions.openAdd = null;
  }
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.selected-count {
  color: var(--color-primary);
  font-weight: 600;
}

.filter-badge :deep(.el-badge__content.is-dot) {
  right: 8px;
  top: 6px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 14px;
  background: var(--color-hover);
  border-radius: 12px;
}

.tags-label {
  color: var(--color-muted);
  font-size: 13px;
}

.filter-tag {
  max-width: 100%;
}

.drawer-body {
  padding: 0 4px 12px;
}

.section {
  margin-bottom: 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--color-ink);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-line);
}

.quick-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.drawer-footer {
  display: flex;
  gap: 12px;
  width: 100%;
}

.footer-btn {
  flex: 1;
  height: 44px;
  font-size: 16px;
}

.export-tip {
  font-size: 15px;
  margin-bottom: 6px;
}

.export-hint {
  color: var(--color-muted);
  font-size: 13px;
  margin-bottom: 14px;
  background: var(--color-hover);
  padding: 10px 12px;
  border-radius: 10px;
}

.field-panel {
  border: 1px solid var(--color-line);
  border-radius: 12px;
  padding: 12px 14px;
  max-height: 360px;
  overflow: auto;
}

.field-group {
  margin-top: 12px;
}

.group-title {
  font-weight: 650;
  margin-bottom: 8px;
  color: var(--color-ink);
}

.field-group :deep(.el-checkbox) {
  margin-right: 14px;
  margin-bottom: 6px;
}

:deep(.el-table) {
  --el-table-row-hover-bg-color: var(--color-hover);
}

.pager {
  margin-top: 20px;
  justify-content: flex-end;
}

.m-toolbar,
.m-export-bar,
.m-list,
.patient-card {
  display: none;
}

.patients.is-mobile .m-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.patients.is-mobile .m-search {
  flex: 1;
}

.patients.is-mobile .m-filter-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: #fff;
  color: #007aff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

.patients.is-mobile .m-export-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 0 2px;
}

.patients.is-mobile .m-card-flat {
  border: none;
  background: transparent;
  box-shadow: none !important;
}

.patients.is-mobile .m-card-flat :deep(.el-card__body) {
  padding: 0 !important;
}

.patients.is-mobile .table-wrap {
  display: none;
}

.patients.is-mobile .m-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.patients.is-mobile .patient-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  min-height: 80px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

.pc-top {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.pc-check {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  margin: 0;
  height: 22px;
  line-height: 1;
}

.pc-check :deep(.el-checkbox) {
  height: 22px;
  display: inline-flex;
  align-items: center;
}

.pc-check :deep(.el-checkbox__label) {
  display: none;
}

.pc-check :deep(.el-checkbox__input) {
  line-height: 1;
}

.pc-main {
  flex: 1;
  min-width: 0;
}

.pc-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
  min-height: 22px;
}

.pc-name {
  font-size: 17px;
  font-weight: 700;
  color: #1c1c1e;
}

.pc-meta {
  font-size: 15px;
  color: #8e8e93;
}

.pc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8e8e93;
}

.pc-dot.dot-bp {
  background: #ff3b30;
}
.pc-dot.dot-dm {
  background: #ff9500;
}
.pc-dot.dot-copd {
  background: #007aff;
}
.pc-dot.dot-chd {
  background: #af52de;
}
.pc-dot.dot-other {
  background: #8e8e93;
}

.pc-disease {
  font-size: 15px;
  color: #8e8e93;
}

.pc-phone {
  display: inline-block;
  font-size: 15px;
  color: #007aff;
  text-decoration: none;
  margin-bottom: 4px;
  min-height: 22px;
}

.pc-date {
  font-size: 13px;
  color: #aeaeb2;
}

.pc-actions {
  display: flex;
  justify-content: flex-end;
}

.pager-mobile {
  justify-content: center !important;
  margin-top: 16px;
}

.m-empty {
  padding: 24px 0;
}
</style>
