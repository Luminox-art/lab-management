package com.example.labmanagement.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavigationModelAdvice {

	private static final String ROLE_MANAGER = "ROLE_CBQL";
	private static final Set<String> REGISTRATION_ROLES = Set.of("ROLE_GV", "ROLE_SV");

	@ModelAttribute
	void navigationModel(Authentication authentication, HttpServletRequest request, Model model) {
		boolean authenticated = authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
		model.addAttribute("navAuthenticated", authenticated);
		model.addAttribute("currentPath", request.getRequestURI().substring(request.getContextPath().length()));
		if (!authenticated) {
			model.addAttribute("navManager", false);
			model.addAttribute("navCanCreateRegistration", false);
			return;
		}

		Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(java.util.stream.Collectors.toSet());
		model.addAttribute("navManager", authorities.contains(ROLE_MANAGER));
		model.addAttribute("navCanCreateRegistration", authorities.stream().anyMatch(REGISTRATION_ROLES::contains));
	}
}
