package com.securebiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecureBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureBizApplication.class, args);
    }
}
