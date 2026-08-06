package com.example.labmanagement.identity.controller;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.dto.RegistrationRequest;
import com.example.labmanagement.identity.dto.RegistrationRole;
import com.example.labmanagement.identity.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class IdentityWebController {

	private final IdentityService identityService;

	public IdentityWebController(IdentityService identityService) {
		this.identityService = identityService;
	}

	@GetMapping("/login")
	String login() {
		return "login";
	}

	@GetMapping({"/", "/home"})
	String home(Authentication authentication, Model model) {
		model.addAttribute("profile", identityService.getProfile(authentication.getName()));
		return "home";
	}

	@GetMapping("/register")
	String registrationForm(Model model) {
		model.addAttribute("registration", new RegistrationRequest("", "", "", "", "", RegistrationRole.SV));
		model.addAttribute("roles", RegistrationRole.values());
		return "register";
	}

	@PostMapping("/register")
	String register(@Valid @ModelAttribute("registration") RegistrationRequest request, BindingResult bindingResult,
			Model model) {
		model.addAttribute("roles", RegistrationRole.values());
		if (bindingResult.hasErrors()) {
			return "register";
		}
		try {
			identityService.register(request);
			return "redirect:/registration-pending";
		} catch (ApiException exception) {
			bindingResult.reject("registration.conflict", exception.getMessage());
			return "register";
		}
	}

	@GetMapping("/registration-pending")
	String registrationPending() {
		return "registration-pending";
	}

	@GetMapping("/profile")
	String profile(Authentication authentication, Model model) {
		model.addAttribute("profile", identityService.getProfile(authentication.getName()));
		return "profile";
	}
}
