package com.careerpilot.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuthenticationWithSpringSecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthenticationWithSpringSecurityApplication.class, args);
	}

}
