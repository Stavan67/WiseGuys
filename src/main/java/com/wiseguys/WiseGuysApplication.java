package com.wiseguys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WiseGuysApplication {

    public static void main(String[] args) {
        SpringApplication.run(WiseGuysApplication.class, args);
    }

}