package com.sjk.clinic.service.impl;

import com.sjk.clinic.dto.PatientQuery;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.repository.PatientRepository;
import com.sjk.clinic.repository.PatientSpecifications;
import com.sjk.clinic.service.PatientService;
import com.sjk.clinic.util.DashboardStatsSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {
    private static final int MAX_PAGE_SIZE = 100;

    private final PatientRepository patientRepository;
    
    @Override
    public Page<Patient> getPatients(PatientQuery query) {
        int size = DashboardStatsSupport.clampPageSize(query.getSize(), MAX_PAGE_SIZE);
        int page = Math.max(query.getPage(), 1);
        query.setSize(size);
        query.setPage(page);
        Pageable pageable = PageRequest.of(page - 1, size);
        return patientRepository.findAll(PatientSpecifications.fromQuery(query), pageable);
    }
    
    @Override
    @Cacheable(value = "patients", key = "'recent'")
    public List<Patient> getRecentPatients() {
        return patientRepository.findTop10ByOrderByCreateTimeDesc(PageRequest.of(0, 10));
    }
    
    @Override
    public Optional<Patient> getPatientById(Long id) {
        // 不缓存实体：Redis 序列化会碰到 diagnosisRecords 懒加载集合导致 500
        return patientRepository.findById(id);
    }
    
    @Override
    @CacheEvict(value = {"patients", "statistics", "home"}, allEntries = true)
    public Patient createPatient(Patient patient) {
        normalizePatient(patient);
        return patientRepository.save(patient);
    }
    
    @Override
    @CacheEvict(value = {"patients", "statistics", "home"}, allEntries = true)
    public Patient updatePatient(Long id, Patient patientDetails) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("患者不存在"));
        
        patient.setName(patientDetails.getName());
        patient.setGender(patientDetails.getGender());
        patient.setAge(patientDetails.getAge());
        patient.setPhone(patientDetails.getPhone());
        patient.setIdCard(patientDetails.getIdCard());
        patient.setAddress(patientDetails.getAddress());
        patient.setDiseaseType(patientDetails.getDiseaseType());
        patient.setMedicalHistory(patientDetails.getMedicalHistory());
        patient.setRemark(patientDetails.getRemark());
        normalizePatient(patient);
        
        return patientRepository.save(patient);
    }

    /** 空身份证存 NULL，避免 uk_id_card 把多个 '' 当成重复 */
    private void normalizePatient(Patient patient) {
        if (patient.getIdCard() != null && patient.getIdCard().isBlank()) {
            patient.setIdCard(null);
        }
    }
    
    @Override
    @CacheEvict(value = {"patients", "statistics", "home"}, allEntries = true)
    public void deletePatient(Long id) {
        patientRepository.deleteById(id);
    }
    
    @Override
    @Cacheable(value = "statistics", key = "'totalPatients'")
    public Long getTotalPatients() {
        return patientRepository.count();
    }
    
    @Override
    public Long getTodayPatients() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX).plusNanos(1);
        return patientRepository.countTodayPatients(dayStart, dayEnd);
    }
    
    @Override
    @Cacheable(value = "statistics", key = "'disease'")
    public List<Map<String, Object>> getDiseaseStatistics() {
        List<Object[]> results = patientRepository.countByDiseaseType();
        long total = getTotalPatients();
        return results.stream()
                .map(result -> {
                    Object type = result[0];
                    long count = result[1] instanceof Number ? ((Number) result[1]).longValue() : 0L;
                    return Map.<String, Object>of(
                            "diseaseType", type != null ? type : "未分类",
                            "count", count,
                            "percentage", calculatePercentage(count, total)
                    );
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "statistics", key = "'age'")
    public List<Map<String, Object>> getAgeStatistics() {
        List<Object[]> results = patientRepository.countByAgeRange();
        long total = getTotalPatients();
        return results.stream()
                .map(result -> {
                    Object range = result[0];
                    long count = result[1] instanceof Number ? ((Number) result[1]).longValue() : 0L;
                    return Map.<String, Object>of(
                            "ageRange", range != null ? range : "未知",
                            "count", count,
                            "percentage", calculatePercentage(count, total)
                    );
                })
                .collect(Collectors.toList());
    }
    
    private double calculatePercentage(long count, long total) {
        if (total == 0) return 0.0;
        return Math.round((double) count / total * 1000) / 10.0;
    }
}
