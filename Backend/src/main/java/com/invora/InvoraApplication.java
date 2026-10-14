package com.invora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvoraApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoraApplication.class, args);
    }
}
