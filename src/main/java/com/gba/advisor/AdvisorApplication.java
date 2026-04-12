package com.gba.advisor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gba.advisor.module.**.mapper")
public class AdvisorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdvisorApplication.class, args);
    }
}
