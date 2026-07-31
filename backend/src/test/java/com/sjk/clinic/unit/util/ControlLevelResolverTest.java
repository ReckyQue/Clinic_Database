package com.sjk.clinic.unit.util;

import com.sjk.clinic.dto.DiagnosisDto;
import com.sjk.clinic.util.ControlLevelResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ControlLevelResolverTest {

    @Test
    @DisplayName("normalizeManualLevel 仅接受合法级别")
    void normalizeManualLevel() {
        assertEquals("high", ControlLevelResolver.normalizeManualLevel(" HIGH "));
        assertNull(ControlLevelResolver.normalizeManualLevel(""));
        assertNull(ControlLevelResolver.normalizeManualLevel("unknown"));
    }

    @Test
    @DisplayName("系统判定：严重指标或多项异常为高危")
    void resolveSystemLevelHigh() {
        DiagnosisDto severe = new DiagnosisDto();
        severe.setSystolicBp(170);
        assertEquals(ControlLevelResolver.HIGH, ControlLevelResolver.resolveSystemLevel(severe));

        DiagnosisDto multi = new DiagnosisDto();
        multi.setAbnormalMetrics(List.of("收缩压偏高", "空腹血糖偏高"));
        assertEquals(ControlLevelResolver.HIGH, ControlLevelResolver.resolveSystemLevel(multi));
    }

    @Test
    @DisplayName("系统判定：单项异常为预警，无异常为达标")
    void resolveSystemLevelWarningAndControlled() {
        DiagnosisDto warning = new DiagnosisDto();
        warning.setAbnormalMetrics(List.of("心率异常"));
        assertEquals(ControlLevelResolver.WARNING, ControlLevelResolver.resolveSystemLevel(warning));

        DiagnosisDto ok = new DiagnosisDto();
        ok.setAbnormalMetrics(List.of());
        assertEquals(ControlLevelResolver.CONTROLLED, ControlLevelResolver.resolveSystemLevel(ok));
    }

    @Test
    @DisplayName("最终级别：手动优先于系统")
    void resolveFinalLevelManualFirst() {
        DiagnosisDto dto = new DiagnosisDto();
        dto.setSystolicBp(180);
        dto.setManualControlLevel("controlled");
        assertEquals(ControlLevelResolver.CONTROLLED, ControlLevelResolver.resolveFinalLevel(dto));
        assertEquals(ControlLevelResolver.SOURCE_MANUAL, ControlLevelResolver.resolveSource(dto));
    }

    @Test
    @DisplayName("toLabel 中文映射")
    void toLabel() {
        assertEquals("高危", ControlLevelResolver.toLabel(ControlLevelResolver.HIGH));
        assertEquals("预警", ControlLevelResolver.toLabel(ControlLevelResolver.WARNING));
        assertEquals("达标", ControlLevelResolver.toLabel(ControlLevelResolver.CONTROLLED));
        assertEquals("未评估", ControlLevelResolver.toLabel("x"));
    }
}
