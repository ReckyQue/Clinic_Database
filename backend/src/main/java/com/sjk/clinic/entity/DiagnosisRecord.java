package com.sjk.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "diagnosis_records")
public class DiagnosisRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "disease_type")
    private String diseaseType;

    @Column(name = "diagnosis_date", nullable = false)
    private LocalDate diagnosisDate;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    private Integer systolicBp;
    private Integer diastolicBp;
    private Double fastingGlucose;
    private Double postprandialGlucose;
    private Double hba1c;
    private Double height;
    private Double weight;
    private Integer heartRate;

    @Column(columnDefinition = "TEXT")
    private String medication;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "health_guidance", columnDefinition = "TEXT")
    private String healthGuidance;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @Column(name = "referral_hospital")
    private String referralHospital;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    private String doctor;

    /** 医生手动标记：high / warning / controlled；为空则系统辅助判断 */
    @Column(name = "manual_control_level", length = 20)
    private String manualControlLevel;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
