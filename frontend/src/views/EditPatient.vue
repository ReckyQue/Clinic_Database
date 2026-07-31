<template>
  <div class="edit-patient">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>编辑患者</span>
        </div>
      </template>

      <el-form
        ref="patientFormRef"
        :model="patientForm"
        :rules="rules"
        label-width="100px"
        style="max-width: 600px; margin: 0 auto"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="patientForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="patientForm.gender" placeholder="请选择性别">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="patientForm.age" :min="0" :max="150" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="patientForm.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="patientForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="patientForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="疾病类型" prop="diseaseType">
          <el-input v-model="patientForm.diseaseType" placeholder="请输入疾病类型" />
        </el-form-item>
        <el-form-item label="病史" prop="medicalHistory">
          <el-input
            v-model="patientForm.medicalHistory"
            type="textarea"
            :rows="4"
            placeholder="请输入病史"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="patientForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';

const router = useRouter();
const route = useRoute();
const store = useStore();
const patientFormRef = ref(null);
const loading = ref(false);

const patientForm = reactive({
  name: '',
  gender: '',
  age: null,
  phone: '',
  idCard: '',
  address: '',
  diseaseType: '',
  medicalHistory: '',
  remark: '',
});

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
};

const fetchPatient = async () => {
  try {
    const res = await request.get(`/patients/${route.params.id}`);
    const patient = res.data;
    Object.assign(patientForm, patient);
    if (patientForm.gender === '男') patientForm.gender = 'MALE';
    if (patientForm.gender === '女') patientForm.gender = 'FEMALE';
  } catch (error) {
    console.error('获取患者信息失败:', error);
  }
};

const handleSubmit = async () => {
  if (!patientFormRef.value) return;

  await patientFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const payload = {
          ...patientForm,
          idCard: patientForm.idCard?.trim() ? patientForm.idCard.trim() : null,
        };
        await request.put(`/patients/${route.params.id}`, payload);
        ElMessage.success('保存成功');
        store.dispatch('notifyHomeRefresh');
        router.push('/patients');
      } catch (error) {
        console.error('保存失败:', error);
      } finally {
        loading.value = false;
      }
    }
  });
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
