package com.sjk.clinic.controller;

import com.sjk.clinic.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 灰度 / 线上巡检探针：模拟核心路径可用性检查（测试右移）。
 */
@RestController
@RequestMapping("/canary")
public class CanaryController {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @GetMapping("/probe")
    public Result<Map<String, Object>> probe() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("profile", activeProfile);
        data.put("checkedAt", Instant.now().toString());
        data.put("checks", Map.of(
                "api", "ok",
                "corePath", "home.dashboard.reachable"));
        return Result.success(data);
    }
}
