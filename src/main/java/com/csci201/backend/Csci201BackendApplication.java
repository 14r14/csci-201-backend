package com.csci201.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Csci201BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(Csci201BackendApplication.class, args);
    }
}
