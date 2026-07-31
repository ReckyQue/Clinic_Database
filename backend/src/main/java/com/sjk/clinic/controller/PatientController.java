package com.sjk.clinic.controller;

import com.sjk.clinic.common.Result;
import com.sjk.clinic.dto.PatientDto;
import com.sjk.clinic.dto.PatientQuery;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PatientController {
    private final PatientService patientService;

    @GetMapping
    public Result<Map<String, Object>> getPatients(PatientFilterRequest request) {
        PatientQuery query = request.toQuery();
        Page<Patient> patientPage = patientService.getPatients(query);

        Map<String, Object> result = new HashMap<>();
        result.put("records", patientPage.getContent().stream()
                .map(PatientDto::fromPatient)
                .toList());
        result.put("total", patientPage.getTotalElements());
        result.put("current", query.getPage());
        result.put("size", query.getSize());

        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<PatientDto> getPatientById(@PathVariable Long id) {
        Patient patient = patientService.getPatientById(id)
                .orElseThrow(() -> new RuntimeException("患者不存在"));
        return Result.success(PatientDto.fromPatient(patient));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Result<Map<String, Object>> createPatient(@RequestBody Patient patient) {
        Patient savedPatient = patientService.createPatient(patient);

        Map<String, Object> result = new HashMap<>();
        result.put("id", savedPatient.getId());

        return Result.success("添加成功", result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Result<Void> updatePatient(@PathVariable Long id, @RequestBody Patient patient) {
        patientService.updatePatient(id, patient);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> getRecentPatients() {
        List<Patient> patients = patientService.getRecentPatients();
        List<Map<String, Object>> result = patients.stream()
                .map(patient -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", patient.getId());
                    map.put("name", patient.getName());
                    map.put("age", patient.getAge());
                    map.put("phone", patient.getPhone());
                    map.put("createTime", patient.getCreateTime());
                    return map;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    @lombok.Data
    public static class PatientFilterRequest {
        private int page = 1;
        private int size = 20;
        private String name;
        private String phone;
        private String idCard;
        private String address;
        private String diseaseType;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate createStart;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate createEnd;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate diagnosisStart;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate diagnosisEnd;
        private boolean onlyWithDiagnosis;
        private String ids;

        public PatientQuery toQuery() {
            return PatientQuery.builder()
                    .page(page)
                    .size(size)
                    .name(name)
                    .phone(phone)
                    .idCard(idCard)
                    .address(address)
                    .diseaseType(diseaseType)
                    .createStart(createStart != null ? createStart.atStartOfDay() : null)
                    .createEnd(createEnd != null ? createEnd.plusDays(1).atStartOfDay() : null)
                    .diagnosisStart(diagnosisStart)
                    .diagnosisEnd(diagnosisEnd)
                    .onlyWithDiagnosis(onlyWithDiagnosis)
                    .ids(parseIds(ids))
                    .build();
        }

        private List<Long> parseIds(String raw) {
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .toList();
        }
    }
}
