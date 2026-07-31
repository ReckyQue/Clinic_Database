package com.sjk.clinic.dto;

import com.sjk.clinic.entity.Patient;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PatientDto {
    private Long id;
    private String name;
    private String gender;
    private Integer age;
    private String phone;
    private String idCard;
    private String address;
    private String diseaseType;
    private String medicalHistory;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    public static PatientDto fromPatient(Patient patient) {
        PatientDto dto = new PatientDto();
        dto.setId(patient.getId());
        dto.setName(patient.getName());
        dto.setGender(patient.getGender() != null ? patient.getGender().getDisplayName() : null);
        dto.setAge(patient.getAge());
        dto.setPhone(patient.getPhone());
        dto.setIdCard(patient.getIdCard());
        dto.setAddress(patient.getAddress());
        dto.setDiseaseType(patient.getDiseaseType());
        dto.setMedicalHistory(patient.getMedicalHistory());
        dto.setRemark(patient.getRemark());
        dto.setCreateTime(patient.getCreateTime());
        dto.setUpdateTime(patient.getUpdateTime());
        return dto;
    }
}
