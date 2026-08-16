package com.example.labmanagement.identity.controller;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.dto.PasswordChangeRequest;
import com.example.labmanagement.identity.dto.ProfileWebForms;
import com.example.labmanagement.identity.dto.RegistrationRequest;
import com.example.labmanagement.identity.dto.RegistrationRole;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.service.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
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
		return profilePage(authentication.getName(), model);
	}

	@PostMapping("/profile")
	String updateProfile(Authentication authentication,
			@Valid @ModelAttribute("profileForm") ProfileWebForms.ProfileForm form, BindingResult bindingResult,
			Model model, HttpServletRequest request, HttpServletResponse response) {
		if (bindingResult.hasErrors()) {
			return profilePage(authentication.getName(), model);
		}
		try {
			UserProfileResponse updated = identityService.updateProfile(authentication.getName(), form.toRequest());
			if (!authentication.getName().equalsIgnoreCase(updated.email())) {
				new SecurityContextLogoutHandler().logout(request, response, authentication);
				return "redirect:/login?profileUpdated";
			}
			return "redirect:/profile?updated";
		} catch (ApiException exception) {
			bindingResult.reject("profile.update", exception.getMessage());
			return profilePage(authentication.getName(), model);
		}
	}

	@PostMapping("/profile/password")
	String changePassword(Authentication authentication,
			@Valid @ModelAttribute("passwordForm") ProfileWebForms.PasswordForm form, BindingResult bindingResult,
			Model model) {
		if (!Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp.");
		}
		if (bindingResult.hasErrors()) {
			return profilePage(authentication.getName(), model);
		}
		try {
			identityService.changePassword(authentication.getName(),
					new PasswordChangeRequest(form.getCurrentPassword(), form.getNewPassword()));
			return "redirect:/profile?passwordUpdated";
		} catch (ApiException exception) {
			bindingResult.rejectValue("currentPassword", "password.current", exception.getMessage());
			return profilePage(authentication.getName(), model);
		}
	}

	private String profilePage(String email, Model model) {
		UserProfileResponse profile = identityService.getProfile(email);
		model.addAttribute("profile", profile);
		if (!model.containsAttribute("profileForm")) {
			model.addAttribute("profileForm", ProfileWebForms.ProfileForm.from(profile));
		}
		if (!model.containsAttribute("passwordForm")) {
			model.addAttribute("passwordForm", new ProfileWebForms.PasswordForm());
		}
		return "profile";
	}
}
