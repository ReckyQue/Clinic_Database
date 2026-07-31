package com.sjk.clinic.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 混沌工程：在 test/staging 可通过请求头注入延迟，验证超时与降级。
 * 启用：chaos.fault-injection.enabled=true
 * 用法：Header X-Chaos-Delay-Ms: 3000
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(name = "chaos.fault-injection.enabled", havingValue = "true")
public class ChaosFaultInjectionFilter extends OncePerRequestFilter {

    @Value("${chaos.fault-injection.max-delay-ms:5000}")
    private long maxDelayMs;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String delayHeader = request.getHeader("X-Chaos-Delay-Ms");
        if (delayHeader != null && !delayHeader.isBlank()) {
            try {
                long delay = Math.min(Long.parseLong(delayHeader.trim()), maxDelayMs);
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            } catch (NumberFormatException ignored) {
                // 非法延迟头忽略
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        filterChain.doFilter(request, response);
    }
}
