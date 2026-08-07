package com.example.labmanagement.identity.controller;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.service.IdentityService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
	String pendingUsers(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Model model) {
		Page<UserProfileResponse> result = identityService.searchUsers(NguoiDungTrangThai.CHO_DUYET, null, keyword,
				page, size);
		model.addAttribute("result", result);
		model.addAttribute("keyword", keyword);
		return "identity/user-approvals";
	}

	@PostMapping("/{id}/approve")
	String approve(@PathVariable String id, @RequestParam long version, RedirectAttributes redirectAttributes) {
		try {
			UserProfileResponse approved = identityService.approveUser(id, version);
			redirectAttributes.addFlashAttribute("success",
					"Đã kích hoạt tài khoản " + approved.id() + " — " + approved.fullName() + ".");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/admin/users";
	}
}
