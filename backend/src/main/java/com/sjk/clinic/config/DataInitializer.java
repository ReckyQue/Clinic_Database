package com.sjk.clinic.config;

import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.repository.DiagnosisRecordRepository;
import com.sjk.clinic.repository.PatientRepository;
import com.sjk.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final PatientRepository patientRepository;
    private final DiagnosisRecordRepository diagnosisRecordRepository;
    private final Environment environment;

    @Override
    public void run(String... args) {
        userService.initializeDefaultAdmin();
        // 生产环境不灌样例数据
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            initializeSampleCases();
        }
    }

    private void initializeSampleCases() {
        if (patientRepository.count() > 0) {
            return;
        }

        // 纯演示假数据：非真实姓名/手机/证件，仅本地/测试用
        Object[][] cases = {
                // name, gender, age, phone, idCard, address, disease, daysAgoCreate,
                // type: BP/GLU/COPD/CAD, diagnosisDaysAgo, metrics..., conclusion
                {"示例患者甲", "M", 62, "10000000001", null, "示例地址 A", "高血压", 5,
                        "BP", 0, 156, 96, "血压偏高，需加强随访"},
                {"示例患者乙", "F", 58, "10000000002", null, "示例地址 B", "Ⅱ型糖尿病", 8,
                        "GLU", 3, 8.6, 13.2, 7.8, "血糖偏高，建议复查"},
                {"示例患者丙", "M", 74, "10000000003", null, "示例地址 C", "高血压", 12,
                        "BP", 10, 168, 102, "高危高血压，建议转诊"},
                {"示例患者丁", "F", 69, "10000000004", null, "示例地址 D", "慢阻肺", 15,
                        "COPD", 7, "症状反复，需加强随访"},
                {"示例患者戊", "M", 55, "10000000005", null, "示例地址 E", "冠心病", 20,
                        "CAD", 20, 128, 78, "指标稳定，常规随访"},
                {"示例患者己", "F", 63, "10000000006", null, "示例地址 F", "Ⅱ型糖尿病", 30,
                        "GLU", 12, 6.2, 8.5, 6.4, "血糖控制达标"},
        };

        for (Object[] row : cases) {
            saveSampleCase(row);
        }
    }

    private void saveSampleCase(Object[] row) {
        Patient patient = new Patient();
        patient.setName((String) row[0]);
        patient.setGender("M".equals(row[1]) ? Patient.Gender.MALE : Patient.Gender.FEMALE);
        patient.setAge((Integer) row[2]);
        patient.setPhone((String) row[3]);
        patient.setIdCard((String) row[4]);
        patient.setAddress((String) row[5]);
        patient.setDiseaseType((String) row[6]);
        patient = patientRepository.save(patient);

        int createDaysAgo = (Integer) row[7];
        patient.setCreateTime(LocalDateTime.now().minusDays(createDaysAgo));
        patient.setUpdateTime(LocalDateTime.now().minusDays(createDaysAgo));
        patient = patientRepository.save(patient);

        String type = (String) row[8];
        int diagnosisDaysAgo = (Integer) row[9];
        LocalDate diagnosisDate = LocalDate.now().minusDays(diagnosisDaysAgo);

        if ("BP".equals(type) || "CAD".equals(type)) {
            saveBpLike(patient, type, diagnosisDate, (Integer) row[10], (Integer) row[11], (String) row[12]);
        } else if ("GLU".equals(type)) {
            saveGlucose(patient, diagnosisDate, (Double) row[10], (Double) row[11], (Double) row[12], (String) row[13]);
        } else {
            saveCopd(patient, diagnosisDate, (String) row[10]);
        }
    }

    private void saveBpLike(
            Patient patient, String type, LocalDate date, int systolic, int diastolic, String conclusion) {
        DiagnosisRecord record = baseRecord(patient, date);
        record.setDiseaseType("CAD".equals(type) ? "冠心病" : "高血压");
        record.setSymptoms("CAD".equals(type) ? "无症状" : "头晕");
        record.setSystolicBp(systolic);
        record.setDiastolicBp(diastolic);
        record.setHeight(168.0);
        record.setWeight(70.0);
        record.setHeartRate(78);
        record.setMedication("CAD".equals(type) ? "阿司匹林 100mg 每日1次" : "氨氯地平 5mg 每日1次");
        record.setDiagnosis(conclusion);
        record.setHealthGuidance("低盐饮食,规律服药");
        record.setNextFollowUpDate(date.plusDays(systolic >= 160 ? 7 : 30));
        if (systolic >= 160) {
            record.setReferralHospital("示例医院");
        }
        record.setDoctor("示例医生");
        diagnosisRecordRepository.save(record);
    }

    private void saveGlucose(
            Patient patient, LocalDate date, double fasting, double post, double hba1c, String conclusion) {
        DiagnosisRecord record = baseRecord(patient, date);
        record.setDiseaseType("Ⅱ型糖尿病");
        record.setSymptoms("多饮,乏力");
        record.setFastingGlucose(fasting);
        record.setPostprandialGlucose(post);
        record.setHba1c(hba1c);
        record.setHeight(160.0);
        record.setWeight(65.0);
        record.setHeartRate(76);
        record.setMedication("二甲双胍 0.5g 每日2次");
        record.setDiagnosis(conclusion);
        record.setHealthGuidance("控制体重,规律服药");
        record.setNextFollowUpDate(fasting >= 10 ? date.minusDays(1) : date.plusDays(30));
        if (fasting >= 10) {
            record.setReferralHospital("示例医院");
        }
        record.setDoctor("示例医生");
        diagnosisRecordRepository.save(record);
    }

    private void saveCopd(Patient patient, LocalDate date, String conclusion) {
        DiagnosisRecord record = baseRecord(patient, date);
        record.setDiseaseType("慢阻肺");
        record.setSymptoms("气短,胸闷");
        record.setHeight(155.0);
        record.setWeight(52.0);
        record.setHeartRate(86);
        record.setMedication("吸入剂按需使用");
        record.setDiagnosis(conclusion);
        record.setHealthGuidance("戒烟限酒,适量运动");
        record.setNextFollowUpDate(date.plusDays(14));
        record.setDoctor("示例医生");
        diagnosisRecordRepository.save(record);
    }

    private DiagnosisRecord baseRecord(Patient patient, LocalDate date) {
        DiagnosisRecord record = new DiagnosisRecord();
        record.setPatient(patient);
        record.setDiagnosisDate(date);
        return record;
    }
}
