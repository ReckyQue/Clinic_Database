package com.sjk.clinic.service.impl;

import com.sjk.clinic.dto.DiagnosisDto;
import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.repository.DiagnosisRecordRepository;
import com.sjk.clinic.repository.PatientRepository;
import com.sjk.clinic.service.StatisticsDashboardService;
import com.sjk.clinic.util.ControlLevelResolver;
import com.sjk.clinic.util.DashboardStatsSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StatisticsDashboardServiceImpl implements StatisticsDashboardService {

    private static final Pattern VILLAGE_PATTERN = Pattern.compile("([\u4e00-\u9fa5]{1,12}(?:村|镇|乡|组|庄))");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PatientRepository patientRepository;
    private final DiagnosisRecordRepository diagnosisRecordRepository;

    @Override
    @Cacheable(value = "statistics", key = "'dashboard'")
    public Map<String, Object> getDashboard() {
        List<Patient> patients = patientRepository.findAll();
        List<DiagnosisRecord> latestRecords = diagnosisRecordRepository.findLatestPerPatient();
        Map<Long, DiagnosisRecord> latestByPatient = DashboardStatsSupport.toLatestByPatientId(latestRecords);

        YearMonth thisMonth = YearMonth.now();
        LocalDate trendStart = thisMonth.minusMonths(5).atDay(1);
        LocalDate trendEnd = thisMonth.atEndOfMonth();
        Map<LocalDate, Long> diagnosisByDate = DashboardStatsSupport.countByDate(
                diagnosisRecordRepository.findDatesBetween(trendStart, trendEnd));

        Map<String, Object> summary = buildSummary(patients, latestByPatient, diagnosisByDate);
        Map<String, Object> controlStatus = buildControlStatus(patients, latestByPatient);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("diseasePie", DashboardStatsSupport.buildDiseasePie(patients));
        result.put("ageGender", buildAgeGender(patients));
        result.put("villageBars", buildVillageBars(patients));
        result.put("trend", buildTrend(patients, diagnosisByDate));
        result.put("controlStatus", controlStatus);
        result.put("updatedAt", LocalDateTime.now().toString());
        return result;
    }

    private Map<String, Object> buildSummary(
            List<Patient> patients,
            Map<Long, DiagnosisRecord> latestByPatient,
            Map<LocalDate, Long> diagnosisByDate) {
        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);
        LocalDateTime thisMonthStart = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime lastMonthStart = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime lastMonthEnd = thisMonthStart;

        long total = patients.size();
        long totalLastMonth = 0;
        long monthNew = 0;
        long lastMonthNew = 0;
        for (Patient p : patients) {
            if (p.getCreateTime() == null) {
                continue;
            }
            if (p.getCreateTime().isBefore(thisMonthStart)) {
                totalLastMonth++;
            }
            if (!p.getCreateTime().isBefore(thisMonthStart)) {
                monthNew++;
            } else if (!p.getCreateTime().isBefore(lastMonthStart) && p.getCreateTime().isBefore(lastMonthEnd)) {
                lastMonthNew++;
            }
        }

        LocalDate thisMonthStartDate = thisMonth.atDay(1);
        LocalDate nextMonthStartDate = thisMonth.plusMonths(1).atDay(1);
        LocalDate lastMonthStartDate = lastMonth.atDay(1);

        long monthFollowUp = sumDatesInRange(diagnosisByDate, thisMonthStartDate, nextMonthStartDate);
        long lastMonthFollowUp = sumDatesInRange(diagnosisByDate, lastMonthStartDate, thisMonthStartDate);

        List<Map<String, Object>> highRiskPatients = new ArrayList<>();
        for (Patient patient : patients) {
            DiagnosisRecord latest = latestByPatient.get(patient.getId());
            if (latest == null) {
                continue;
            }
            DiagnosisDto dto = DiagnosisDto.fromEntity(latest);
            if (ControlLevelResolver.HIGH.equals(ControlLevelResolver.resolveFinalLevel(dto))) {
                highRiskPatients.add(toPatientBrief(patient, latest, dto));
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPatients", total);
        summary.put("totalDelta", total - totalLastMonth);
        summary.put("monthNew", monthNew);
        summary.put("monthNewDelta", monthNew - lastMonthNew);
        summary.put("monthFollowUp", monthFollowUp);
        summary.put("monthFollowUpDelta", monthFollowUp - lastMonthFollowUp);
        summary.put("highRiskCount", highRiskPatients.size());
        summary.put("highRiskPatients", highRiskPatients);
        return summary;
    }

    private long sumDatesInRange(Map<LocalDate, Long> byDate, LocalDate startInclusive, LocalDate endExclusive) {
        long sum = 0;
        for (Map.Entry<LocalDate, Long> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            if (date != null && !date.isBefore(startInclusive) && date.isBefore(endExclusive)) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    private Map<String, Object> buildAgeGender(List<Patient> patients) {
        String[] ranges = {"<50", "50-59", "60-69", "70-79", "≥80"};
        long[] male = new long[ranges.length];
        long[] female = new long[ranges.length];

        for (Patient patient : patients) {
            int index = ageRangeIndex(patient.getAge());
            if (patient.getGender() == Patient.Gender.MALE) {
                male[index]++;
            } else {
                female[index]++;
            }
        }

        List<Long> maleList = new ArrayList<>(ranges.length);
        List<Long> femaleList = new ArrayList<>(ranges.length);
        for (int i = 0; i < ranges.length; i++) {
            maleList.add(male[i]);
            femaleList.add(female[i]);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("categories", List.of(ranges));
        result.put("male", maleList);
        result.put("female", femaleList);
        return result;
    }

    private int ageRangeIndex(Integer age) {
        if (age == null || age < 50) {
            return 0;
        }
        if (age < 60) {
            return 1;
        }
        if (age < 70) {
            return 2;
        }
        if (age < 80) {
            return 3;
        }
        return 4;
    }

    private List<Map<String, Object>> buildVillageBars(List<Patient> patients) {
        Map<String, Long> counts = new HashMap<>();
        for (Patient patient : patients) {
            counts.merge(extractVillage(patient.getAddress()), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>(2);
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .toList();
    }

    private String extractVillage(String address) {
        if (!StringUtils.hasText(address)) {
            return "未填写地区";
        }
        Matcher matcher = VILLAGE_PATTERN.matcher(address);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return address.length() > 12 ? address.substring(0, 12) : address;
    }

    private Map<String, Object> buildTrend(List<Patient> patients, Map<LocalDate, Long> diagnosisByDate) {
        List<String> months = new ArrayList<>(6);
        List<Long> newCounts = new ArrayList<>(6);
        List<Long> followUpCounts = new ArrayList<>(6);
        YearMonth current = YearMonth.now();

        long[] newByOffset = new long[6];
        for (Patient p : patients) {
            if (p.getCreateTime() == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(p.getCreateTime());
            for (int i = 5; i >= 0; i--) {
                if (ym.equals(current.minusMonths(i))) {
                    newByOffset[5 - i]++;
                    break;
                }
            }
        }

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            months.add(month.format(MONTH_FMT));
            newCounts.add(newByOffset[5 - i]);
            followUpCounts.add(sumDatesInRange(
                    diagnosisByDate, month.atDay(1), month.plusMonths(1).atDay(1)));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months);
        result.put("newPatients", newCounts);
        result.put("followUps", followUpCounts);
        return result;
    }

    private Map<String, Object> buildControlStatus(
            List<Patient> patients,
            Map<Long, DiagnosisRecord> latestByPatient) {
        List<Map<String, Object>> controlled = new ArrayList<>();
        List<Map<String, Object>> warning = new ArrayList<>();
        List<Map<String, Object>> high = new ArrayList<>();

        for (Patient patient : patients) {
            DiagnosisRecord latest = latestByPatient.get(patient.getId());
            if (latest == null) {
                continue;
            }
            DiagnosisDto dto = DiagnosisDto.fromEntity(latest);
            String level = ControlLevelResolver.resolveFinalLevel(dto);
            Map<String, Object> brief = toPatientBrief(patient, latest, dto);
            if (ControlLevelResolver.HIGH.equals(level)) {
                high.add(brief);
            } else if (ControlLevelResolver.WARNING.equals(level)) {
                warning.add(brief);
            } else {
                controlled.add(brief);
            }
        }

        int diagnosed = controlled.size() + warning.size() + high.size();
        int total = Math.max(diagnosed, 1);

        Map<String, Object> result = new HashMap<>();
        result.put("controlledCount", controlled.size());
        result.put("warningCount", warning.size());
        result.put("highCount", high.size());
        result.put("controlledPercent", Math.round(controlled.size() * 1000.0 / total) / 10.0);
        result.put("warningPercent", Math.round(warning.size() * 1000.0 / total) / 10.0);
        result.put("highPercent", Math.round(high.size() * 1000.0 / total) / 10.0);
        result.put("controlledPatients", controlled);
        result.put("warningPatients", warning);
        result.put("highPatients", high);
        result.put("diagnosedTotal", diagnosed);
        return result;
    }

    private Map<String, Object> toPatientBrief(Patient patient, DiagnosisRecord latest, DiagnosisDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", patient.getId());
        map.put("name", patient.getName());
        map.put("age", patient.getAge());
        map.put("phone", patient.getPhone());
        map.put("address", patient.getAddress());
        map.put("diseaseType", Objects.toString(
                latest.getDiseaseType() != null ? latest.getDiseaseType() : patient.getDiseaseType(), "未分类"));
        map.put("diagnosisDate", latest.getDiagnosisDate());
        map.put("metricsSummary", dto.getMetricsSummary());
        return map;
    }
}
