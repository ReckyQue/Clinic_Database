package com.sjk.clinic.unit.util;

import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.util.DashboardStatsSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardStatsSupportTest {

    @Test
    void clampPageSize() {
        assertEquals(1, DashboardStatsSupport.clampPageSize(0, 100));
        assertEquals(100, DashboardStatsSupport.clampPageSize(999, 100));
        assertEquals(20, DashboardStatsSupport.clampPageSize(20, 100));
    }

    @Test
    void countByDate() {
        java.util.ArrayList<LocalDate> dates = new java.util.ArrayList<>();
        dates.add(LocalDate.of(2026, 7, 1));
        dates.add(LocalDate.of(2026, 7, 1));
        dates.add(LocalDate.of(2026, 7, 2));
        dates.add(null);
        Map<LocalDate, Long> map = DashboardStatsSupport.countByDate(dates);
        assertEquals(2L, map.get(LocalDate.of(2026, 7, 1)));
        assertEquals(1L, map.get(LocalDate.of(2026, 7, 2)));
    }

    @Test
    void toLatestByPatientId() {
        Patient p = new Patient();
        p.setId(1L);
        DiagnosisRecord d = new DiagnosisRecord();
        d.setId(9L);
        d.setPatient(p);
        Map<Long, DiagnosisRecord> map = DashboardStatsSupport.toLatestByPatientId(List.of(d));
        assertEquals(9L, map.get(1L).getId());
    }

    @Test
    void buildDiseasePie() {
        Patient a = new Patient();
        a.setDiseaseType("高血压");
        Patient b = new Patient();
        b.setDiseaseType(null);
        List<Map<String, Object>> pie = DashboardStatsSupport.buildDiseasePie(List.of(a, b));
        assertEquals(2, pie.size());
    }

    @Test
    void isMetricAbnormal() {
        assertTrue(DashboardStatsSupport.isMetricAbnormal(
                new Object[]{150, 80, null, null, null, 72, 170.0, 60.0}));
        assertFalse(DashboardStatsSupport.isMetricAbnormal(
                new Object[]{120, 80, 5.0, null, 6.0, 72, 170.0, 60.0}));
        assertFalse(DashboardStatsSupport.isMetricAbnormal(null));
    }
}
