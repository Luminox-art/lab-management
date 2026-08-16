package com.example.labmanagement.identity.controller;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.dto.UserAdminForms;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.service.IdentityService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class UserAdminWebController {

	private final IdentityService identityService;

	public UserAdminWebController(IdentityService identityService) {
		this.identityService = identityService;
	}

	@GetMapping
	String users(@RequestParam(required = false) NguoiDungTrangThai status, @RequestParam(required = false) String role,
			@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Principal principal, Authentication authentication,
			Model model) {
		Page<UserProfileResponse> result = identityService.searchUsers(principal.getName(), status, role, keyword, page,
				size);
		model.addAttribute("result", result);
		model.addAttribute("status", status);
		model.addAttribute("role", role);
		model.addAttribute("keyword", keyword);
		model.addAttribute("statuses", NguoiDungTrangThai.values());
		model.addAttribute("roles",
				isAdministrator(authentication) ? List.of("ADMIN", "CBQL", "GV", "SV") : List.of("CBQL", "GV", "SV"));
		model.addAttribute("administrator", isAdministrator(authentication));
		model.addAttribute("actorEmail", principal.getName());
		return "identity/user-approvals";
	}

	@GetMapping("/{id}/edit")
	String edit(@PathVariable String id, Principal principal, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("userForm",
					UserAdminForms.UserForm.from(identityService.getUser(principal.getName(), id)));
			return userForm(id, model, authentication);
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
			return "redirect:/admin/users";
		}
	}

	@PostMapping("/{id}")
	String update(@PathVariable String id, @Valid @ModelAttribute("userForm") UserAdminForms.UserForm form,
			BindingResult bindingResult, Principal principal, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return userForm(id, model, authentication);
		}
		try {
			UserProfileResponse updated = identityService.updateUser(principal.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success",
					"Đã cập nhật tài khoản " + updated.id() + " — " + updated.fullName() + ".");
			return "redirect:/admin/users";
		} catch (ApiException exception) {
			bindingResult.reject("identity.user", exception.getMessage());
			return userForm(id, model, authentication);
		}
	}

	@PostMapping("/{id}/approve")
	String approve(@PathVariable String id, @RequestParam long version, Principal principal,
			RedirectAttributes redirectAttributes) {
		try {
			UserProfileResponse approved = identityService.approveUser(principal.getName(), id, version);
			redirectAttributes.addFlashAttribute("success",
					"Đã kích hoạt tài khoản " + approved.id() + " — " + approved.fullName() + ".");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/{id}/status")
	String changeStatus(@PathVariable String id, @RequestParam NguoiDungTrangThai status, @RequestParam long version,
			Principal principal, RedirectAttributes redirectAttributes) {
		try {
			UserProfileResponse updated = identityService.changeUserStatus(principal.getName(), id, status, version);
			String action = switch (status) {
				case BI_KHOA -> "khóa";
				case HOAT_DONG -> "mở khóa";
				case CHO_DUYET -> "chuyển về chờ duyệt";
			};
			redirectAttributes.addFlashAttribute("success",
					"Đã " + action + " tài khoản " + updated.id() + " — " + updated.fullName() + ".");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/admin/users";
	}

	private String userForm(String id, Model model, Authentication authentication) {
		model.addAttribute("userId", id);
		model.addAttribute("statuses", NguoiDungTrangThai.values());
		model.addAttribute("roles",
				isAdministrator(authentication) ? List.of("ADMIN", "CBQL", "GV", "SV") : List.of("GV", "SV"));
		return "identity/user-form";
	}

	private boolean isAdministrator(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}
}
