package com.example.labmanagement.registration.web;

import com.example.labmanagement.catalog.application.CatalogService;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.registration.application.RegistrationResponse;
import com.example.labmanagement.registration.application.RegistrationService;
import com.example.labmanagement.registration.application.RegistrationSummaryResponse;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.scheduling.application.SchedulingService;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/registrations")
public class RegistrationWebController {

	private final RegistrationService registrationService;
	private final CatalogService catalogService;
	private final SchedulingService schedulingService;

	public RegistrationWebController(RegistrationService registrationService, CatalogService catalogService,
			SchedulingService schedulingService) {
		this.registrationService = registrationService;
		this.catalogService = catalogService;
		this.schedulingService = schedulingService;
	}

	@GetMapping
	String registrations(@RequestParam(required = false) LoaiPhieu type,
			@RequestParam(required = false) PhieuDangKyTrangThai status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Authentication authentication, Model model) {
		Page<RegistrationSummaryResponse> result = registrationService.search(authentication.getName(), type, status,
				page, size);
		boolean manager = hasRole(authentication, "CBQL");
		model.addAttribute("result", result);
		model.addAttribute("types", LoaiPhieu.values());
		model.addAttribute("statuses",
				manager ? List.of(PhieuDangKyTrangThai.CHO_DUYET) : List.of(PhieuDangKyTrangThai.values()));
		model.addAttribute("type", type);
		model.addAttribute("status", manager ? PhieuDangKyTrangThai.CHO_DUYET : status);
		model.addAttribute("canCreate", hasRole(authentication, "GV") || hasRole(authentication, "SV"));
		model.addAttribute("manager", manager);
		return "registration/list";
	}

	@GetMapping("/new")
	String newRegistration(Model model, Authentication authentication) {
		model.addAttribute("registrationForm", new RegistrationForms.RegistrationForm());
		return form(model, authentication, false, null);
	}

	@PostMapping
	String create(@Valid @ModelAttribute("registrationForm") RegistrationForms.RegistrationForm form,
			BindingResult bindingResult, Model model, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return form(model, authentication, false, null);
		}
		try {
			RegistrationResponse created = registrationService.create(authentication.getName(), form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã gửi phiếu và chuyển sang trạng thái chờ duyệt.");
			return "redirect:/registrations/" + created.id();
		} catch (ApiException exception) {
			bindingResult.reject("registration.create", exception.getMessage());
			return form(model, authentication, false, null);
		}
	}

	@GetMapping("/{id}")
	String detail(@PathVariable String id, Principal principal, Model model) {
		RegistrationResponse registration = registrationService.get(principal.getName(), id);
		RegistrationForms.CancellationForm cancellationForm = new RegistrationForms.CancellationForm();
		cancellationForm.setVersion(registration.version());
		model.addAttribute("registration", registration);
		model.addAttribute("cancellationForm", cancellationForm);
		return "registration/detail";
	}

	@GetMapping("/{id}/edit")
	String edit(@PathVariable String id, Authentication authentication, Model model) {
		RegistrationResponse registration = registrationService.get(authentication.getName(), id);
		if (!registration.canEdit()) {
			throw accessDenied();
		}
		model.addAttribute("registrationForm", RegistrationForms.RegistrationForm.from(registration));
		return form(model, authentication, true, id);
	}

	@PostMapping("/{id}")
	String update(@PathVariable String id,
			@Valid @ModelAttribute("registrationForm") RegistrationForms.RegistrationForm form,
			BindingResult bindingResult, Model model, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return form(model, authentication, true, id);
		}
		try {
			registrationService.update(authentication.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã cập nhật phiếu đăng ký.");
			return "redirect:/registrations/" + id;
		} catch (ApiException exception) {
			bindingResult.reject("registration.update", exception.getMessage());
			return form(model, authentication, true, id);
		}
	}

	@PostMapping("/{id}/cancel")
	String cancel(@PathVariable String id,
			@Valid @ModelAttribute("cancellationForm") RegistrationForms.CancellationForm form,
			BindingResult bindingResult, Principal principal, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("registration", registrationService.get(principal.getName(), id));
			return "registration/detail";
		}
		try {
			registrationService.cancel(principal.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã hủy phiếu đăng ký.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/registrations/" + id;
	}

	private String form(Model model, Authentication authentication, boolean editing, String registrationId) {
		model.addAttribute("editing", editing);
		model.addAttribute("registrationId", registrationId);
		model.addAttribute("rooms", catalogService.selectableRooms());
		model.addAttribute("devices", registrationService.deviceOptions());
		model.addAttribute("supervisors", registrationService.supervisorOptions());
		model.addAttribute("periods", schedulingService.periods());
		model.addAttribute("dayOptions", dayOptions());
		model.addAttribute("allowedTypes", allowedTypes(authentication));
		model.addAttribute("student", hasRole(authentication, "SV"));
		return "registration/form";
	}

	private List<DayOption> dayOptions() {
		List<DayOption> options = new ArrayList<>();
		for (int day = ScheduleDateCalculator.MONDAY; day <= ScheduleDateCalculator.SUNDAY; day++) {
			options.add(new DayOption(day, ScheduleDateCalculator.systemDayLabel(day)));
		}
		return List.copyOf(options);
	}

	private List<LoaiPhieu> allowedTypes(Authentication authentication) {
		return hasRole(authentication, "SV")
				? List.of(LoaiPhieu.HOC_TAP, LoaiPhieu.NGHIEN_CUU)
				: List.of(LoaiPhieu.values());
	}

	private boolean hasRole(Authentication authentication, String role) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
	}

	private ApiException accessDenied() {
		return new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
				"Bạn không có quyền sửa phiếu đăng ký này.");
	}

	public record DayOption(int value, String label) {
	}
}
