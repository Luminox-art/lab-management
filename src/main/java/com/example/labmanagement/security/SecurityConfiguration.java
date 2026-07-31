package com.example.labmanagement.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorization -> authorization.requestMatchers("/actuator/health/readiness")
				.permitAll().anyRequest().authenticated()).formLogin(withDefaults()).logout(withDefaults())
				.sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()));
		return http.build();
	}
}
