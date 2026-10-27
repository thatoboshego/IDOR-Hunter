package com.idorhunter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the IDOR Hunter Security Lab.
 *
 * This application is an intentionally self-contained, local-only lab used to
 * teach and test Insecure Direct Object Reference (IDOR) / Broken Access
 * Control vulnerabilities (OWASP API1:2023 / A01:2021). All exploitation is
 * restricted to seeded synthetic data running in-process against an embedded
 * H2 database. Nothing in this project sends traffic to third-party systems
 * unless the operator explicitly configures real Twilio credentials.
 */
@SpringBootApplication
@EnableScheduling
public class IdorHunterApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdorHunterApplication.class, args);
    }
}
