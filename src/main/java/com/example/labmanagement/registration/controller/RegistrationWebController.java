package com.example.labmanagement.registration.controller;

import com.example.labmanagement.catalog.service.CatalogService;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.dto.ApprovalPreviewResponse;
import com.example.labmanagement.registration.dto.RegistrationForms;
import com.example.labmanagement.registration.dto.RegistrationResponse;
import com.example.labmanagement.registration.dto.RegistrationSummaryResponse;
import com.example.labmanagement.registration.service.ApprovalService;
import com.example.labmanagement.registration.service.RegistrationService;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import com.example.labmanagement.scheduling.service.SchedulingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
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
	private final ApprovalService approvalService;

	public RegistrationWebController(RegistrationService registrationService, CatalogService catalogService,
			SchedulingService schedulingService, ApprovalService approvalService) {
		this.registrationService = registrationService;
		this.catalogService = catalogService;
		this.schedulingService = schedulingService;
		this.approvalService = approvalService;
	}

	@GetMapping
	String registrations(@RequestParam(required = false) LoaiPhieu type,
			@RequestParam(required = false) PhieuDangKyTrangThai status, @RequestParam(required = false) String roomId,
			@RequestParam(required = false) LocalDate date, @RequestParam(required = false) String creator,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Authentication authentication, Model model) {
		Page<RegistrationSummaryResponse> result = registrationService.search(authentication.getName(), type, status,
				roomId, date, creator, page, size);
		boolean manager = hasRole(authentication, "CBQL");
		model.addAttribute("result", result);
		model.addAttribute("types", LoaiPhieu.values());
		model.addAttribute("statuses",
				manager ? List.of(PhieuDangKyTrangThai.CHO_DUYET) : List.of(PhieuDangKyTrangThai.values()));
		model.addAttribute("type", type);
		model.addAttribute("status", manager ? PhieuDangKyTrangThai.CHO_DUYET : status);
		model.addAttribute("canCreate", hasRole(authentication, "GV") || hasRole(authentication, "SV"));
		model.addAttribute("manager", manager);
		model.addAttribute("rooms", manager ? catalogService.selectableRooms() : List.of());
		model.addAttribute("roomId", roomId);
		model.addAttribute("date", date);
		model.addAttribute("creator", creator);
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
	String detail(@PathVariable String id, Authentication authentication, Model model) {
		RegistrationResponse registration = registrationService.get(authentication.getName(), id);
		RegistrationForms.CancellationForm cancellationForm = new RegistrationForms.CancellationForm();
		cancellationForm.setVersion(registration.version());
		model.addAttribute("cancellationForm", cancellationForm);
		prepareDecisionModel(model, authentication, registration);
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

	@PostMapping("/{id}/approve")
	String approve(@PathVariable String id, @Valid @ModelAttribute("approvalForm") RegistrationForms.ApprovalForm form,
			BindingResult bindingResult, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			RegistrationResponse registration = registrationService.get(authentication.getName(), id);
			RegistrationForms.RejectionForm rejectionForm = new RegistrationForms.RejectionForm();
			rejectionForm.setVersion(registration.version());
			model.addAttribute("rejectionForm", rejectionForm);
			model.addAttribute("cancellationForm", new RegistrationForms.CancellationForm());
			prepareDecisionModel(model, authentication, registration);
			return "registration/detail";
		}
		try {
			approvalService.approve(authentication.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã phê duyệt và phân bổ tài nguyên cho phiếu.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/registrations/" + id;
	}

	@PostMapping("/{id}/reject")
	String reject(@PathVariable String id, @Valid @ModelAttribute("rejectionForm") RegistrationForms.RejectionForm form,
			BindingResult bindingResult, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			RegistrationResponse registration = registrationService.get(authentication.getName(), id);
			RegistrationForms.ApprovalForm approvalForm = approvalForm(registration);
			model.addAttribute("approvalForm", approvalForm);
			model.addAttribute("cancellationForm", new RegistrationForms.CancellationForm());
			prepareDecisionModel(model, authentication, registration);
			return "registration/detail";
		}
		try {
			approvalService.reject(authentication.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã từ chối phiếu đăng ký.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/registrations/" + id;
	}

	private void prepareDecisionModel(Model model, Authentication authentication, RegistrationResponse registration) {
		boolean manager = hasRole(authentication, "CBQL");
		boolean pending = registration.status() == PhieuDangKyTrangThai.CHO_DUYET;
		model.addAttribute("registration", registration);
		model.addAttribute("manager", manager);
		model.addAttribute("canDecide", manager && pending);
		if (!manager) {
			return;
		}
		ApprovalPreviewResponse preview = approvalService.preview(authentication.getName(), registration.id());
		model.addAttribute("approvalPreview", preview);
		if (!model.containsAttribute("approvalForm")) {
			model.addAttribute("approvalForm", approvalForm(registration));
		}
		if (!model.containsAttribute("rejectionForm")) {
			RegistrationForms.RejectionForm rejectionForm = new RegistrationForms.RejectionForm();
			rejectionForm.setVersion(registration.version());
			model.addAttribute("rejectionForm", rejectionForm);
		}
	}

	private RegistrationForms.ApprovalForm approvalForm(RegistrationResponse registration) {
		RegistrationForms.ApprovalForm form = new RegistrationForms.ApprovalForm();
		form.setVersion(registration.version());
		form.setDeviceIds(registration.devices().stream().map(device -> device.id()).toList());
		return form;
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
