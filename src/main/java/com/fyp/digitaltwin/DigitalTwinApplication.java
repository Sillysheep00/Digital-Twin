package com.fyp.digitaltwin; // Adjust to your package name

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // <--- This allows the background simulation loop to run
public class DigitalTwinApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalTwinApplication.class, args);
    }
}