package com.neuroforged.leadsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class LeadSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(LeadSystemApplication.class, args);
	}
}