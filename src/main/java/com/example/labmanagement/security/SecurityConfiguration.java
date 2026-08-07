package com.example.labmanagement.security;

import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.common.response.ApiErrorResponse;
import com.example.labmanagement.common.trace.TraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		RequestMatcher apiRequest = request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/");
		LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
		http.authorizeHttpRequests(authorization -> authorization
				.requestMatchers("/actuator/health/readiness", "/api/v1/auth/register", "/api/v1/auth/login", "/login",
						"/register", "/registration-pending", "/error", "/css/**", "/js/**", "/v3/api-docs",
						"/v3/api-docs.yaml", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
				.permitAll().requestMatchers("/api/v1/users/**", "/admin/users/**").hasRole("CBQL")
				.requestMatchers(HttpMethod.POST, "/api/v1/rooms", "/api/v1/devices").hasRole("CBQL")
				.requestMatchers(HttpMethod.PUT, "/api/v1/rooms/**", "/api/v1/devices/**").hasRole("CBQL")
				.requestMatchers(HttpMethod.POST, "/api/v1/registrations/*/approve", "/api/v1/registrations/*/reject")
				.hasRole("CBQL").requestMatchers("/api/v1/admin-blocks/**", "/admin-blocks/**").hasRole("CBQL")
				.requestMatchers("/api/v1/maintenances/**", "/maintenances/**").hasRole("CBQL")
				.requestMatchers("/api/v1/dashboard/**", "/dashboard/**").hasRole("CBQL")
				.requestMatchers(HttpMethod.PATCH, "/api/v1/incidents/**").hasRole("CBQL")
				.requestMatchers(HttpMethod.POST, "/incidents/*/update").hasRole("CBQL")
				.requestMatchers(HttpMethod.POST, "/api/v1/registrations").hasAnyRole("GV", "SV")
				.requestMatchers(HttpMethod.POST, "/api/v1/registrations/*/cancel").hasAnyRole("GV", "SV")
				.requestMatchers(HttpMethod.PUT, "/api/v1/registrations/**").hasAnyRole("GV", "SV")
				.requestMatchers(HttpMethod.GET, "/registrations/new", "/registrations/*/edit").hasAnyRole("GV", "SV")
				.requestMatchers(HttpMethod.POST, "/registrations/*/approve", "/registrations/*/reject").hasRole("CBQL")
				.requestMatchers(HttpMethod.POST, "/registrations").hasAnyRole("GV", "SV")
				.requestMatchers(HttpMethod.POST, "/registrations/*", "/registrations/*/cancel").hasAnyRole("GV", "SV")
				.requestMatchers(HttpMethod.GET, "/catalog/rooms", "/catalog/devices").authenticated()
				.requestMatchers("/catalog/**").hasRole("CBQL").anyRequest().authenticated())
				.formLogin(form -> form.loginPage("/login").usernameParameter("email").failureUrl("/login?error")
						.defaultSuccessUrl("/home", true).permitAll())
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
						.invalidateHttpSession(true).clearAuthentication(true))
				.sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
					if (apiRequest.matches(request)) {
						writeError(objectMapper, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED,
								"Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
						return;
					}
					loginEntryPoint.commence(request, response, exception);
				}).accessDeniedHandler((request, response, exception) -> {
					if (apiRequest.matches(request)) {
						writeError(objectMapper, response, HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED,
								"Bạn không có quyền thực hiện thao tác này.");
						return;
					}
					response.sendError(HttpStatus.FORBIDDEN.value());
				}));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	private static void writeError(ObjectMapper objectMapper, HttpServletResponse response, HttpStatus status,
			ErrorCode code, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(),
				new ApiErrorResponse(code.name(), message, List.of(), TraceContext.currentTraceId()));
	}
}
