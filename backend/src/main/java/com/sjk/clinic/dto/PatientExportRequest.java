package com.sjk.clinic.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PatientExportRequest {
    private String name;
    private String phone;
    private String idCard;
    private String address;
    private String diseaseType;
    private LocalDate createStart;
    private LocalDate createEnd;
    private LocalDate diagnosisStart;
    private LocalDate diagnosisEnd;
    private boolean onlyWithDiagnosis;
    private List<Long> ids = new ArrayList<>();

    private List<String> fields = new ArrayList<>();
    private boolean exportAllHistory;
    private boolean maskSensitive;
    /** xlsx 或 csv */
    private String format = "xlsx";
    private String filename;
}
