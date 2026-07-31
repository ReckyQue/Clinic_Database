package com.sjk.clinic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "staging", "test"})
public class OpenApiConfig {

    @Bean
    public OpenAPI clinicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("乡村慢性病管理系统 API")
                        .description("clinic OpenAPI 3 文档")
                        .version("4.0.0")
                        .contact(new Contact().name("clinic")));
    }
}
