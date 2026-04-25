package com.priyansh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

//@SpringBootApplication
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.priyansh")
public class GuardrailApiEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuardrailApiEngineApplication.class, args);
	}

}