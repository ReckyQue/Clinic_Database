package com.sjk.clinic.service;

import com.sjk.clinic.dto.ExportFileResult;
import com.sjk.clinic.dto.PatientExportRequest;

import java.io.IOException;
import java.util.List;

public interface ExportService {
    byte[] exportPatients(String name, String phone, String idCard, String diseaseType, List<Long> ids)
            throws IOException;

    byte[] exportDiagnoses(String patientName, String diseaseType, List<Long> patientIds) throws IOException;

    ExportFileResult exportByFilter(PatientExportRequest request) throws IOException;
}
