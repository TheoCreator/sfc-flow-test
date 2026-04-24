package com.sfc.flowtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * Entry point. Profiles: {@code application.yml}, {@code application-dev.yml}, {@code application-prod.yml}.
 */
@SpringBootApplication
@MapperScan("com.sfc.flowtest")
public class FlowTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowTestApplication.class, args);
    }
}
