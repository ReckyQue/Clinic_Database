package com.sjk.clinic.service;

import com.sjk.clinic.dto.PatientQuery;
import com.sjk.clinic.entity.Patient;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PatientService {
    Page<Patient> getPatients(PatientQuery query);

    List<Patient> getRecentPatients();

    Optional<Patient> getPatientById(Long id);

    Patient createPatient(Patient patient);

    Patient updatePatient(Long id, Patient patientDetails);

    void deletePatient(Long id);

    Long getTotalPatients();

    Long getTodayPatients();

    List<Map<String, Object>> getDiseaseStatistics();

    List<Map<String, Object>> getAgeStatistics();
}
