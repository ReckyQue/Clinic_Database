package com.sjk.clinic.support;

import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;

import java.time.LocalDate;

/**
 * 测试数据工厂：避免依赖生产数据，统一构造合法实体。
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Patient patient(String name, String phone) {
        Patient patient = new Patient();
        patient.setName(name);
        patient.setPhone(phone);
        patient.setAge(60);
        patient.setGender(Patient.Gender.MALE);
        patient.setDiseaseType("高血压");
        patient.setAddress("测试村1号");
        return patient;
    }

    public static DiagnosisRecord diagnosis(Patient patient, LocalDate date) {
        DiagnosisRecord record = new DiagnosisRecord();
        record.setPatient(patient);
        record.setDiseaseType(patient.getDiseaseType());
        record.setDiagnosisDate(date);
        record.setSystolicBp(120);
        record.setDiastolicBp(80);
        record.setNextFollowUpDate(date.plusDays(30));
        record.setDoctor("测试村医");
        return record;
    }
}
