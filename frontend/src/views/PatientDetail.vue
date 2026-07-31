<template>
  <div class="patient-detail">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>患者详情</span>
          <div>
            <el-button @click="$router.back()">返回</el-button>
            <el-button
              v-if="isAdmin || isMember"
              type="primary"
              @click="$router.push(`/patients/edit/${patientId}`)"
              >编辑</el-button
            >
          </div>
        </div>
      </template>

      <el-descriptions v-loading="loading" :column="2" border>
        <el-descriptions-item label="姓名">{{ patient.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ patient.gender }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ patient.age }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ patient.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ patient.idCard }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ patient.address }}</el-descriptions-item>
        <el-descriptions-item label="疾病类型">{{ patient.diseaseType }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{
          formatDate(patient.createTime)
        }}</el-descriptions-item>
        <el-descriptions-item label="病史" :span="2">
          {{ patient.medicalHistory || '暂无' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ patient.remark || '暂无' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import request from '@/utils/request';

const route = useRoute();
const store = useStore();
const patientId = computed(() => route.params.id);
const loading = ref(false);
const patient = ref({});

const isAdmin = computed(() => store.getters.isAdmin);
const isMember = computed(() => store.getters.isMember);

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN');
};

const fetchPatient = async () => {
  loading.value = true;
  try {
    const res = await request.get(`/patients/${patientId.value}`);
    patient.value = res.data;
  } catch (error) {
    console.error('获取患者详情失败:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchPatient();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
