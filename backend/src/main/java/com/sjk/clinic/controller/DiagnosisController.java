package com.sjk.clinic.controller;

import com.sjk.clinic.common.Result;
import com.sjk.clinic.dto.DiagnosisDto;
import com.sjk.clinic.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/diagnoses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Result<Map<String, Object>> getDiagnoses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String diseaseType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean onlyOverdue) {

        Page<DiagnosisDto> diagnosisPage = diagnosisService.getDiagnoses(
                diseaseType, startDate, endDate, keyword, onlyOverdue, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("records", diagnosisPage.getContent());
        result.put("total", diagnosisPage.getTotalElements());
        result.put("current", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Result<Map<String, Object>> getStats() {
        return Result.success(diagnosisService.getStats());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Result<DiagnosisDto> getById(@PathVariable Long id) {
        DiagnosisDto dto = diagnosisService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("诊断记录不存在"));
        return Result.success(dto);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Result<DiagnosisDto> create(@RequestBody DiagnosisDto dto) {
        return Result.success("创建成功", diagnosisService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    public Result<DiagnosisDto> update(@PathVariable Long id, @RequestBody DiagnosisDto dto) {
        return Result.success("更新成功", diagnosisService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        diagnosisService.delete(id);
        return Result.success("删除成功", null);
    }
}
