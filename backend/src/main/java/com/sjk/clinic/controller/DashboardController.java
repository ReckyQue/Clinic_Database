package com.sjk.clinic.controller;

import com.sjk.clinic.common.Result;
import com.sjk.clinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {
    private final PatientService patientService;
    
    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPatients", patientService.getTotalPatients());
        stats.put("todayPatients", patientService.getTodayPatients());
        return Result.success(stats);
    }
}
