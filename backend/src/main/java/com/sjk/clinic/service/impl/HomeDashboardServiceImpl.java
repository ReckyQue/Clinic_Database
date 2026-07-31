package com.sjk.clinic.service.impl;

import com.sjk.clinic.dto.DiagnosisDto;
import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.repository.DiagnosisRecordRepository;
import com.sjk.clinic.repository.PatientRepository;
import com.sjk.clinic.service.HomeDashboardService;
import com.sjk.clinic.util.ControlLevelResolver;
import com.sjk.clinic.util.DashboardStatsSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HomeDashboardServiceImpl implements HomeDashboardService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");
    private static final int TODO_LIMIT = 12;
    private static final int RECENT_LIMIT = 8;

    private final PatientRepository patientRepository;
    private final DiagnosisRecordRepository diagnosisRecordRepository;

    @Override
    @Cacheable(value = "home", key = "'dashboard'")
    public Map<String, Object> getDashboard() {
        List<Patient> patients = patientRepository.findAll();
        List<DiagnosisRecord> latestRecords = diagnosisRecordRepository.findLatestPerPatient();
        Map<Long, DiagnosisRecord> latestByPatient = DashboardStatsSupport.toLatestByPatientId(latestRecords);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();

        long totalPatients = patients.size();
        long weekNew = 0;
        long todayNew = 0;
        for (Patient p : patients) {
            if (p.getCreateTime() == null) {
                continue;
            }
            if (!p.getCreateTime().isBefore(weekStart)) {
                weekNew++;
            }
            if (!p.getCreateTime().isBefore(todayStart)) {
                todayNew++;
            }
        }

        List<Map<String, Object>> overdueList = new ArrayList<>();
        List<Map<String, Object>> highRiskList = new ArrayList<>();
        List<Map<String, Object>> todos = new ArrayList<>();

        for (Patient patient : patients) {
            DiagnosisRecord latest = latestByPatient.get(patient.getId());
            if (latest == null) {
                continue;
            }
            DiagnosisDto dto = DiagnosisDto.fromEntity(latest);
            String level = ControlLevelResolver.resolveFinalLevel(dto);
            boolean overdue = Boolean.TRUE.equals(dto.getFollowUpOverdue());
            long overdueDays = 0;
            if (overdue && latest.getNextFollowUpDate() != null) {
                overdueDays = java.time.temporal.ChronoUnit.DAYS.between(latest.getNextFollowUpDate(), today);
            }

            if (ControlLevelResolver.HIGH.equals(level)) {
                Map<String, Object> brief = toPatientBrief(patient, latest, dto, level);
                highRiskList.add(brief);
                todos.add(buildTodo(patient, latest, dto, "high",
                        buildHighRiskReason(dto), 1, overdueDays));
            }
            if (overdue) {
                Map<String, Object> brief = toPatientBrief(patient, latest, dto, level);
                brief.put("overdueDays", overdueDays);
                overdueList.add(brief);
                if (!ControlLevelResolver.HIGH.equals(level)) {
                    todos.add(buildTodo(patient, latest, dto, "overdue",
                            "已逾期" + overdueDays + "天未随访", 2, overdueDays));
                }
            } else if (latest.getNextFollowUpDate() != null) {
                LocalDate next = latest.getNextFollowUpDate();
                if (!next.isAfter(today.plusDays(1)) && !next.isBefore(today)) {
                    String reason = next.equals(today) ? "今日需随访" : "明日需随访";
                    todos.add(buildTodo(patient, latest, dto, "due_soon", reason, 3, 0));
                }
            }
        }

        todos.sort(Comparator
                .comparingInt((Map<String, Object> item) -> (Integer) item.get("priority"))
                .thenComparingLong(item -> -((Number) item.getOrDefault("overdueDays", 0L)).longValue()));

        if (todos.size() > TODO_LIMIT) {
            todos = new ArrayList<>(todos.subList(0, TODO_LIMIT));
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPatients", totalPatients);
        summary.put("totalDelta", weekNew);
        summary.put("todayNewPatients", todayNew);
        summary.put("overdueFollowUpCount", overdueList.size());
        summary.put("highRiskCount", highRiskList.size());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("diseasePie", DashboardStatsSupport.buildDiseasePie(patients));
        result.put("visitTrend7Days", buildVisitTrend7Days(today));
        result.put("attentionTodos", todos);
        result.put("overduePatients", overdueList);
        result.put("highRiskPatients", highRiskList);
        result.put("recentPatients", buildRecentPatients(patients));
        result.put("updatedAt", LocalDateTime.now().toString());
        return result;
    }

    private Map<String, Object> buildVisitTrend7Days(LocalDate today) {
        LocalDate start = today.minusDays(6);
        Map<LocalDate, Long> byDate = DashboardStatsSupport.countByDate(
                diagnosisRecordRepository.findDatesBetween(start, today));

        List<String> labels = new ArrayList<>(7);
        List<Long> values = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            labels.add(day.format(DAY_FMT));
            values.add(byDate.getOrDefault(day, 0L));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        return result;
    }

    private List<Map<String, Object>> buildRecentPatients(List<Patient> patients) {
        return patients.stream()
                .sorted(Comparator.comparing(Patient::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_LIMIT)
                .map(patient -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", patient.getId());
                    map.put("name", patient.getName());
                    map.put("age", patient.getAge());
                    map.put("phone", patient.getPhone());
                    map.put("diseaseType", patient.getDiseaseType());
                    map.put("createTime", patient.getCreateTime());
                    return map;
                })
                .toList();
    }

    private Map<String, Object> buildTodo(
            Patient patient,
            DiagnosisRecord latest,
            DiagnosisDto dto,
            String type,
            String reason,
            int priority,
            long overdueDays) {
        Map<String, Object> todo = new HashMap<>();
        todo.put("patientId", patient.getId());
        todo.put("patientName", patient.getName());
        todo.put("type", type);
        todo.put("priority", priority);
        todo.put("reason", reason);
        todo.put("overdueDays", overdueDays);
        todo.put("metricsSummary", dto.getMetricsSummary());
        todo.put("nextFollowUpDate", latest.getNextFollowUpDate());
        todo.put("diseaseType", Objects.toString(
                latest.getDiseaseType() != null ? latest.getDiseaseType() : patient.getDiseaseType(),
                "未分类"));
        return todo;
    }

    private Map<String, Object> toPatientBrief(
            Patient patient,
            DiagnosisRecord latest,
            DiagnosisDto dto,
            String level) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", patient.getId());
        map.put("name", patient.getName());
        map.put("age", patient.getAge());
        map.put("phone", patient.getPhone());
        map.put("diseaseType", Objects.toString(
                latest.getDiseaseType() != null ? latest.getDiseaseType() : patient.getDiseaseType(),
                "未分类"));
        map.put("metricsSummary", dto.getMetricsSummary());
        map.put("controlLevel", level);
        map.put("nextFollowUpDate", latest.getNextFollowUpDate());
        return map;
    }

    private String buildHighRiskReason(DiagnosisDto dto) {
        if (dto.getSystolicBp() != null && dto.getSystolicBp() >= 160
                || dto.getDiastolicBp() != null && dto.getDiastolicBp() >= 100) {
            String bp = (dto.getSystolicBp() != null ? dto.getSystolicBp() : "-")
                    + "/"
                    + (dto.getDiastolicBp() != null ? dto.getDiastolicBp() : "-");
            return "血压" + bp + " 需立即复诊";
        }
        if (dto.getFastingGlucose() != null && dto.getFastingGlucose() >= 10) {
            return "空腹血糖" + dto.getFastingGlucose() + " 需立即复诊";
        }
        if (dto.getHba1c() != null && dto.getHba1c() >= 8) {
            return "糖化血红蛋白" + dto.getHba1c() + "% 需立即复诊";
        }
        if (dto.getAbnormalMetrics() != null && !dto.getAbnormalMetrics().isEmpty()) {
            return "指标异常：" + String.join("、", dto.getAbnormalMetrics());
        }
        return "高危预警，建议上门/转诊";
    }
}
