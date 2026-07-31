package com.sjk.clinic.dto;

import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.util.ControlLevelResolver;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class DiagnosisDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private String patientGender;
    private Integer patientAge;
    private String patientIdCard;
    private String diseaseType;
    private LocalDate diagnosisDate;
    private List<String> symptoms;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Double fastingGlucose;
    private Double postprandialGlucose;
    private Double hba1c;
    private Double height;
    private Double weight;
    private Double bmi;
    private Integer heartRate;
    private String medication;
    private String diagnosis;
    private List<String> healthGuidance;
    private LocalDate nextFollowUpDate;
    private String referralHospital;
    private String treatment;
    private String doctor;
    /** 医生手动标记，空表示交给系统辅助 */
    private String manualControlLevel;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String metricsSummary;
    private List<String> abnormalMetrics;
    private Boolean followUpOverdue;
    /** 最终控制级别：high / warning / controlled */
    private String controlLevel;
    /** 系统辅助级别（忽略手动） */
    private String systemControlLevel;
    /** manual / auto */
    private String controlLevelSource;

    public static DiagnosisDto fromEntity(DiagnosisRecord record) {
        DiagnosisDto dto = new DiagnosisDto();
        dto.setId(record.getId());
        dto.setDiseaseType(record.getDiseaseType());
        dto.setDiagnosisDate(record.getDiagnosisDate());
        dto.setSymptoms(splitCsv(record.getSymptoms()));
        dto.setSystolicBp(record.getSystolicBp());
        dto.setDiastolicBp(record.getDiastolicBp());
        dto.setFastingGlucose(record.getFastingGlucose());
        dto.setPostprandialGlucose(record.getPostprandialGlucose());
        dto.setHba1c(record.getHba1c());
        dto.setHeight(record.getHeight());
        dto.setWeight(record.getWeight());
        dto.setBmi(calculateBmi(record.getHeight(), record.getWeight()));
        dto.setHeartRate(record.getHeartRate());
        dto.setMedication(record.getMedication());
        dto.setDiagnosis(record.getDiagnosis());
        dto.setHealthGuidance(splitCsv(record.getHealthGuidance()));
        dto.setNextFollowUpDate(record.getNextFollowUpDate());
        dto.setReferralHospital(record.getReferralHospital());
        dto.setTreatment(record.getTreatment());
        dto.setDoctor(record.getDoctor());
        dto.setManualControlLevel(ControlLevelResolver.normalizeManualLevel(record.getManualControlLevel()));
        dto.setRemark(record.getRemark());
        dto.setCreateTime(record.getCreateTime());
        dto.setUpdateTime(record.getUpdateTime());

        Patient patient = record.getPatient();
        if (patient != null) {
            dto.setPatientId(patient.getId());
            dto.setPatientName(patient.getName());
            dto.setPatientPhone(patient.getPhone());
            dto.setPatientGender(patient.getGender() != null ? patient.getGender().getDisplayName() : null);
            dto.setPatientAge(patient.getAge());
            dto.setPatientIdCard(patient.getIdCard());
        }

        dto.setAbnormalMetrics(buildAbnormalMetrics(dto));
        dto.setMetricsSummary(buildMetricsSummary(dto));
        dto.setFollowUpOverdue(record.getNextFollowUpDate() != null
                && record.getNextFollowUpDate().isBefore(LocalDate.now()));
        dto.setSystemControlLevel(ControlLevelResolver.resolveSystemLevel(dto));
        dto.setControlLevel(ControlLevelResolver.resolveFinalLevel(dto));
        dto.setControlLevelSource(ControlLevelResolver.resolveSource(dto));
        return dto;
    }

    public static DiagnosisRecord toEntity(DiagnosisDto dto, Patient patient) {
        DiagnosisRecord record = new DiagnosisRecord();
        applyToEntity(record, dto, patient);
        return record;
    }

    public static void applyToEntity(DiagnosisRecord record, DiagnosisDto dto, Patient patient) {
        record.setPatient(patient);
        record.setDiseaseType(dto.getDiseaseType());
        record.setDiagnosisDate(dto.getDiagnosisDate());
        record.setSymptoms(joinCsv(dto.getSymptoms()));
        record.setSystolicBp(dto.getSystolicBp());
        record.setDiastolicBp(dto.getDiastolicBp());
        record.setFastingGlucose(dto.getFastingGlucose());
        record.setPostprandialGlucose(dto.getPostprandialGlucose());
        record.setHba1c(dto.getHba1c());
        record.setHeight(dto.getHeight());
        record.setWeight(dto.getWeight());
        record.setHeartRate(dto.getHeartRate());
        record.setMedication(dto.getMedication());
        record.setDiagnosis(dto.getDiagnosis());
        record.setHealthGuidance(joinCsv(dto.getHealthGuidance()));
        record.setNextFollowUpDate(dto.getNextFollowUpDate());
        record.setReferralHospital(dto.getReferralHospital());
        record.setTreatment(dto.getTreatment());
        record.setDoctor(dto.getDoctor());
        record.setManualControlLevel(ControlLevelResolver.normalizeManualLevel(dto.getManualControlLevel()));
        record.setRemark(dto.getRemark());
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }

    private static Double calculateBmi(Double height, Double weight) {
        if (height == null || weight == null || height <= 0) {
            return null;
        }
        double heightMeter = height / 100.0;
        return Math.round((weight / (heightMeter * heightMeter)) * 10.0) / 10.0;
    }

    private static List<String> buildAbnormalMetrics(DiagnosisDto dto) {
        List<String> abnormal = new ArrayList<>();
        if (dto.getSystolicBp() != null && dto.getSystolicBp() >= 140) {
            abnormal.add("收缩压偏高");
        }
        if (dto.getDiastolicBp() != null && dto.getDiastolicBp() >= 90) {
            abnormal.add("舒张压偏高");
        }
        if (dto.getFastingGlucose() != null && dto.getFastingGlucose() >= 7.0) {
            abnormal.add("空腹血糖偏高");
        }
        if (dto.getPostprandialGlucose() != null && dto.getPostprandialGlucose() >= 11.1) {
            abnormal.add("餐后血糖偏高");
        }
        if (dto.getHba1c() != null && dto.getHba1c() >= 7.0) {
            abnormal.add("糖化血红蛋白偏高");
        }
        if (dto.getHeartRate() != null && (dto.getHeartRate() < 60 || dto.getHeartRate() > 100)) {
            abnormal.add("心率异常");
        }
        if (dto.getBmi() != null && (dto.getBmi() >= 28 || dto.getBmi() < 18.5)) {
            abnormal.add("BMI异常");
        }
        return abnormal;
    }

    private static String buildMetricsSummary(DiagnosisDto dto) {
        List<String> parts = new ArrayList<>();
        if (dto.getSystolicBp() != null && dto.getDiastolicBp() != null) {
            parts.add("血压 " + dto.getSystolicBp() + "/" + dto.getDiastolicBp() + " mmHg");
        }
        if (dto.getFastingGlucose() != null) {
            parts.add("空腹血糖 " + dto.getFastingGlucose() + " mmol/L");
        }
        if (dto.getPostprandialGlucose() != null) {
            parts.add("餐后2h " + dto.getPostprandialGlucose() + " mmol/L");
        }
        if (dto.getHba1c() != null) {
            parts.add("HbA1c " + dto.getHba1c() + "%");
        }
        if (dto.getHeartRate() != null) {
            parts.add("心率 " + dto.getHeartRate() + " 次/分");
        }
        if (dto.getBmi() != null) {
            parts.add("BMI " + dto.getBmi());
        }
        return parts.isEmpty() ? "暂无指标" : String.join("；", parts);
    }
}
