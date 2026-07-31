package com.sjk.clinic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sjk.clinic.converter.GenderConverter;
import com.sjk.clinic.util.GenderDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Convert(converter = GenderConverter.class)
    @JsonDeserialize(using = GenderDeserializer.class)
    private Gender gender;
    
    private Integer age;
    @Column(nullable = false)
    private String phone;
    @Column(name = "id_card", unique = true)
    private String idCard;
    private String address;
    @Column(name = "disease_type")
    private String diseaseType;
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;
    private String remark;
    @Column(name = "create_time")
    private LocalDateTime createTime;
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiagnosisRecord> diagnosisRecords;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
    
    public enum Gender {
        MALE("男"), FEMALE("女");
        private final String displayName;
        Gender(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}
