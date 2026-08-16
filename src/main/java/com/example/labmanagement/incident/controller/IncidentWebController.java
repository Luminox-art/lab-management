package com.example.labmanagement.incident.controller;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import com.example.labmanagement.incident.dto.IncidentForms;
import com.example.labmanagement.incident.dto.IncidentResponse;
import com.example.labmanagement.incident.service.IncidentService;
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
@RequestMapping("/incidents")
public class IncidentWebController {

	private final IncidentService incidentService;

	public IncidentWebController(IncidentService incidentService) {
		this.incidentService = incidentService;
	}

	@GetMapping
	String incidents(@RequestParam(required = false) SuCoTrangThai status,
			@RequestParam(required = false) MucDoSuCo severity, @RequestParam(required = false) String resourceId,
			@RequestParam(required = false) Long sessionId, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Authentication authentication, Model model) {
		Page<IncidentResponse> result = incidentService.search(authentication.getName(), status, severity, resourceId,
				sessionId, keyword, page, size);
		model.addAttribute("result", result);
		model.addAttribute("statuses", SuCoTrangThai.values());
		model.addAttribute("severities", MucDoSuCo.values());
		model.addAttribute("status", status);
		model.addAttribute("severity", severity);
		model.addAttribute("resourceId", resourceId);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("keyword", keyword);
		return "incident/list";
	}

	@GetMapping("/new")
	String newIncident(@RequestParam(required = false) Long sessionId, Model model) {
		IncidentForms.ReportForm form = new IncidentForms.ReportForm();
		form.setSessionId(sessionId);
		model.addAttribute("incidentForm", form);
		return reportForm(model);
	}

	@PostMapping
	String report(@Valid @ModelAttribute("incidentForm") IncidentForms.ReportForm form, BindingResult bindingResult,
			Principal principal, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return reportForm(model);
		}
		try {
			IncidentResponse incident = incidentService.report(principal.getName(), form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã ghi nhận sự cố và thông báo cho người liên quan.");
			return "redirect:/incidents/" + incident.id();
		} catch (ApiException exception) {
			bindingResult.reject("incident.report", exception.getMessage());
			return reportForm(model);
		}
	}

	@GetMapping("/{id}")
	String detail(@PathVariable String id, Authentication authentication, Model model) {
		IncidentResponse incident = incidentService.get(authentication.getName(), id);
		prepareDetail(model, authentication, incident);
		return "incident/detail";
	}

	@PostMapping("/{id}/update")
	String update(@PathVariable String id, @Valid @ModelAttribute("updateForm") IncidentForms.UpdateForm form,
			BindingResult bindingResult, Authentication authentication, Model model,
			RedirectAttributes redirectAttributes) {
		if ((form.getStatus() == SuCoTrangThai.DANG_XU_LY || form.getStatus() == SuCoTrangThai.DA_XU_LY)
				&& (form.getHandlerId() == null || form.getHandlerId().isBlank())) {
			bindingResult.rejectValue("handlerId", "incident.handler.required", "Vui lòng chọn người xử lý.");
		}
		if (form.getStatus() == SuCoTrangThai.DA_XU_LY && (form.getResult() == null || form.getResult().isBlank())) {
			bindingResult.rejectValue("result", "incident.result.required",
					"Hoàn thành sự cố bắt buộc phải có kết quả xử lý.");
		}
		if (bindingResult.hasErrors()) {
			prepareDetail(model, authentication, incidentService.get(authentication.getName(), id));
			model.addAttribute("updateForm", form);
			return "incident/detail";
		}
		try {
			incidentService.update(authentication.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã cập nhật xử lý sự cố.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/incidents/" + id;
	}

	private String reportForm(Model model) {
		model.addAttribute("resources", incidentService.resourceOptions());
		model.addAttribute("severities", MucDoSuCo.values());
		return "incident/form";
	}

	private void prepareDetail(Model model, Authentication authentication, IncidentResponse incident) {
		boolean manager = hasRole(authentication, "CBQL") || hasRole(authentication, "ADMIN");
		model.addAttribute("incident", incident);
		model.addAttribute("manager", manager);
		model.addAttribute("terminal",
				incident.status() == SuCoTrangThai.DA_XU_LY || incident.status() == SuCoTrangThai.DA_HUY);
		if (manager) {
			model.addAttribute("handlers", incidentService.handlerOptions());
			model.addAttribute("nextStatuses", nextStatuses(incident.status()));
			if (!model.containsAttribute("updateForm")) {
				model.addAttribute("updateForm", IncidentForms.UpdateForm.from(incident));
			}
		}
	}

	private List<SuCoTrangThai> nextStatuses(SuCoTrangThai current) {
		return switch (current) {
			case MOI -> List.of(SuCoTrangThai.DANG_XU_LY, SuCoTrangThai.DA_HUY);
			case DANG_XU_LY -> List.of(SuCoTrangThai.DANG_XU_LY, SuCoTrangThai.DA_XU_LY, SuCoTrangThai.DA_HUY);
			case DA_XU_LY, DA_HUY -> List.of();
		};
	}

	private boolean hasRole(Authentication authentication, String role) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
	}
}
