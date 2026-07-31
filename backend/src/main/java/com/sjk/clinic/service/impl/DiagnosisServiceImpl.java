package com.sjk.clinic.service.impl;

import com.sjk.clinic.dto.DiagnosisDto;
import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.repository.DiagnosisRecordRepository;
import com.sjk.clinic.repository.PatientRepository;
import com.sjk.clinic.service.DiagnosisService;
import com.sjk.clinic.util.DashboardStatsSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiagnosisServiceImpl implements DiagnosisService {

    private static final int MAX_PAGE_SIZE = 100;

    private final DiagnosisRecordRepository diagnosisRecordRepository;
    private final PatientRepository patientRepository;

    @Override
    public Page<DiagnosisDto> getDiagnoses(String diseaseType, LocalDate startDate, LocalDate endDate,
                                           String keyword, boolean onlyOverdue, int page, int size) {
        int safeSize = DashboardStatsSupport.clampPageSize(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 1);
        return diagnosisRecordRepository.findBySearchCriteria(
                        diseaseType, startDate, endDate, keyword, onlyOverdue,
                        PageRequest.of(safePage - 1, safeSize))
                .map(DiagnosisDto::fromEntity);
    }

    @Override
    public Map<String, Object> getStats() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        long todayCount = diagnosisRecordRepository.countByDiagnosisDate(today);
        long monthCount = diagnosisRecordRepository.countByDiagnosisDateBetween(monthStart, monthEnd);

        long abnormalCount = 0;
        List<Object[]> metrics = diagnosisRecordRepository.findAllMetricColumns();
        for (Object[] row : metrics) {
            if (DashboardStatsSupport.isMetricAbnormal(row)) {
                abnormalCount++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayCount", todayCount);
        stats.put("monthCount", monthCount);
        stats.put("abnormalCount", abnormalCount);
        return stats;
    }

    @Override
    public Optional<DiagnosisDto> getById(Long id) {
        return diagnosisRecordRepository.findByIdWithPatient(id).map(DiagnosisDto::fromEntity);
    }

    @Override
    @CacheEvict(value = {"statistics", "home"}, allEntries = true)
    public DiagnosisDto create(DiagnosisDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("患者不存在"));
        if (dto.getDiagnosisDate() == null) {
            dto.setDiagnosisDate(LocalDate.now());
        }
        if (dto.getNextFollowUpDate() == null) {
            dto.setNextFollowUpDate(dto.getDiagnosisDate().plusDays(30));
        }
        DiagnosisRecord record = DiagnosisDto.toEntity(dto, patient);
        return DiagnosisDto.fromEntity(diagnosisRecordRepository.save(record));
    }

    @Override
    @CacheEvict(value = {"statistics", "home"}, allEntries = true)
    public DiagnosisDto update(Long id, DiagnosisDto dto) {
        DiagnosisRecord record = diagnosisRecordRepository.findByIdWithPatient(id)
                .orElseThrow(() -> new IllegalArgumentException("诊断记录不存在"));
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("患者不存在"));
        DiagnosisDto.applyToEntity(record, dto, patient);
        return DiagnosisDto.fromEntity(diagnosisRecordRepository.save(record));
    }

    @Override
    @CacheEvict(value = {"statistics", "home"}, allEntries = true)
    public void delete(Long id) {
        if (!diagnosisRecordRepository.existsById(id)) {
            throw new IllegalArgumentException("诊断记录不存在");
        }
        diagnosisRecordRepository.deleteById(id);
    }
}
