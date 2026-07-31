package com.example.labmanagement.identity.web;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.identity.application.IdentityService;
import com.example.labmanagement.identity.application.LoginRequest;
import com.example.labmanagement.identity.application.RegistrationRequest;
import com.example.labmanagement.identity.application.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

	private final IdentityService identityService;
	private final AuthenticationManager authenticationManager;

	public AuthRestController(IdentityService identityService, AuthenticationManager authenticationManager) {
		this.identityService = identityService;
		this.authenticationManager = authenticationManager;
	}

	@PostMapping("/register")
	ResponseEntity<ApiResponse<UserProfileResponse>> register(@Valid @RequestBody RegistrationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(identityService.register(request)));
	}

	@PostMapping("/login")
	ApiResponse<UserProfileResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest) {
		try {
			String email = request.email().trim().toLowerCase(Locale.ROOT);
			Authentication authentication = authenticationManager
					.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			HttpSession session = servletRequest.getSession(true);
			servletRequest.changeSessionId();
			session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
			return ApiResponse.of(identityService.getProfile(authentication.getName()));
		} catch (AuthenticationException exception) {
			throw new ApiException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED,
					"Thông tin đăng nhập không hợp lệ.");
		}
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}
}
