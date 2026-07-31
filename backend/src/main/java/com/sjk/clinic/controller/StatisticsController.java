package com.sjk.clinic.controller;

import com.sjk.clinic.common.Result;
import com.sjk.clinic.service.PatientService;
import com.sjk.clinic.service.StatisticsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {
    private final PatientService patientService;
    private final StatisticsDashboardService statisticsDashboardService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        return Result.success(statisticsDashboardService.getDashboard());
    }

    @GetMapping("/disease")
    public Result<List<Map<String, Object>>> getDiseaseStatistics() {
        return Result.success(patientService.getDiseaseStatistics());
    }

    @GetMapping("/age")
    public Result<List<Map<String, Object>>> getAgeStatistics() {
        return Result.success(patientService.getAgeStatistics());
    }
}
