package com.kilivana.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KilivanaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KilivanaBackendApplication.class, args);
    }
}
