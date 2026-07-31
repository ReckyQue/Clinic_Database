package com.sjk.clinic.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PatientQuery {
    private String name;
    private String phone;
    private String idCard;
    private String address;
    private String diseaseType;
    private LocalDateTime createStart;
    private LocalDateTime createEnd;
    private LocalDate diagnosisStart;
    private LocalDate diagnosisEnd;
    private boolean onlyWithDiagnosis;
    private List<Long> ids;
    private int page;
    private int size;
}
