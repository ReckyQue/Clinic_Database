package com.sjk.clinic.util;

import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页 / 统计看板共用的聚合工具，避免两处复制同一套扫表逻辑。
 */
public final class DashboardStatsSupport {

    private DashboardStatsSupport() {
    }

    public static Map<Long, DiagnosisRecord> toLatestByPatientId(List<DiagnosisRecord> latestRecords) {
        Map<Long, DiagnosisRecord> latest = new HashMap<>(Math.max(16, latestRecords.size() * 2));
        for (DiagnosisRecord record : latestRecords) {
            if (record.getPatient() == null || record.getPatient().getId() == null) {
                continue;
            }
            latest.put(record.getPatient().getId(), record);
        }
        return latest;
    }

    public static List<Map<String, Object>> buildDiseasePie(List<Patient> patients) {
        Map<String, Long> counts = patients.stream()
                .collect(Collectors.groupingBy(
                        p -> StringUtils.hasText(p.getDiseaseType()) ? p.getDiseaseType() : "其他",
                        Collectors.counting()));
        long total = Math.max(patients.size(), 1);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>(4);
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    item.put("percentage", Math.round(entry.getValue() * 1000.0 / total) / 10.0);
                    return item;
                })
                .toList();
    }

    /** 单次遍历统计日期频次，避免 O(天数 × N) 重复扫描。 */
    public static Map<LocalDate, Long> countByDate(List<LocalDate> dates) {
        Map<LocalDate, Long> counts = new HashMap<>();
        for (LocalDate date : dates) {
            if (date != null) {
                counts.merge(date, 1L, Long::sum);
            }
        }
        return counts;
    }

    /** row: systolicBp, diastolicBp, fastingGlucose, postprandialGlucose, hba1c, heartRate, height, weight */
    public static boolean isMetricAbnormal(Object[] row) {
        if (row == null || row.length < 8) {
            return false;
        }
        Integer systolicBp = (Integer) row[0];
        Integer diastolicBp = (Integer) row[1];
        Double fastingGlucose = (Double) row[2];
        Double postprandialGlucose = (Double) row[3];
        Double hba1c = (Double) row[4];
        Integer heartRate = (Integer) row[5];
        Double height = (Double) row[6];
        Double weight = (Double) row[7];

        if (systolicBp != null && systolicBp >= 140) {
            return true;
        }
        if (diastolicBp != null && diastolicBp >= 90) {
            return true;
        }
        if (fastingGlucose != null && fastingGlucose >= 7.0) {
            return true;
        }
        if (postprandialGlucose != null && postprandialGlucose >= 11.1) {
            return true;
        }
        if (hba1c != null && hba1c >= 7.0) {
            return true;
        }
        if (heartRate != null && (heartRate < 60 || heartRate > 100)) {
            return true;
        }
        Double bmi = calculateBmi(height, weight);
        return bmi != null && (bmi >= 28 || bmi < 18.5);
    }

    private static Double calculateBmi(Double height, Double weight) {
        if (height == null || weight == null || height <= 0) {
            return null;
        }
        double heightMeter = height / 100.0;
        return Math.round((weight / (heightMeter * heightMeter)) * 10.0) / 10.0;
    }

    public static int clampPageSize(int size, int max) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, max);
    }
}
