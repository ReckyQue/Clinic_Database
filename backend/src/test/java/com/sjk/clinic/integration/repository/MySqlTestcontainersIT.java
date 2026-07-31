package com.sjk.clinic.integration.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testcontainers 示例：需本机 Docker。默认 Disabled，避免 CI/本地无 Docker 时失败。
 * 启用：去掉 @Disabled 后执行 mvn -f backend/pom.xml verify
 */
@Testcontainers
@Disabled("需要本机 Docker；演示用，按需启用")
class MySqlTestcontainersIT {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("clinic_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void containerStarts() {
        assertTrue(mysql.isRunning());
    }
}
