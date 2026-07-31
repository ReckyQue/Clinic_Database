package com.sjk.clinic.service;

import com.sjk.clinic.dto.DiagnosisDto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface DiagnosisService {
    Page<DiagnosisDto> getDiagnoses(String diseaseType, LocalDate startDate, LocalDate endDate,
                                    String keyword, boolean onlyOverdue, int page, int size);

    Map<String, Object> getStats();

    Optional<DiagnosisDto> getById(Long id);

    DiagnosisDto create(DiagnosisDto dto);

    DiagnosisDto update(Long id, DiagnosisDto dto);

    void delete(Long id);
}
