package com.sjk.clinic.util;

import com.sjk.clinic.dto.DiagnosisDto;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 控制情况判定：医生手动标记优先，未标记时使用系统辅助判断。
 */
public final class ControlLevelResolver {

    public static final String HIGH = "high";
    public static final String WARNING = "warning";
    public static final String CONTROLLED = "controlled";
    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_AUTO = "auto";

    private static final Set<String> VALID_LEVELS = Set.of(HIGH, WARNING, CONTROLLED);

    private ControlLevelResolver() {
    }

    public static String normalizeManualLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String level = raw.trim().toLowerCase(Locale.ROOT);
        return VALID_LEVELS.contains(level) ? level : null;
    }

    /** 仅系统辅助判断（忽略手动标记） */
    public static String resolveSystemLevel(DiagnosisDto dto) {
        List<String> abnormal = dto.getAbnormalMetrics() == null ? List.of() : dto.getAbnormalMetrics();
        boolean severe = (dto.getSystolicBp() != null && dto.getSystolicBp() >= 160)
                || (dto.getDiastolicBp() != null && dto.getDiastolicBp() >= 100)
                || (dto.getFastingGlucose() != null && dto.getFastingGlucose() >= 10)
                || (dto.getHba1c() != null && dto.getHba1c() >= 8);
        if (severe || abnormal.size() >= 2) {
            return HIGH;
        }
        if (!abnormal.isEmpty()) {
            return WARNING;
        }
        return CONTROLLED;
    }

    /** 最终级别：手动优先，否则系统辅助 */
    public static String resolveFinalLevel(DiagnosisDto dto) {
        String manual = normalizeManualLevel(dto.getManualControlLevel());
        if (manual != null) {
            return manual;
        }
        return resolveSystemLevel(dto);
    }

    public static String resolveSource(DiagnosisDto dto) {
        return normalizeManualLevel(dto.getManualControlLevel()) != null ? SOURCE_MANUAL : SOURCE_AUTO;
    }

    public static String toLabel(String level) {
        if (HIGH.equals(level)) {
            return "高危";
        }
        if (WARNING.equals(level)) {
            return "预警";
        }
        if (CONTROLLED.equals(level)) {
            return "达标";
        }
        return "未评估";
    }
}
