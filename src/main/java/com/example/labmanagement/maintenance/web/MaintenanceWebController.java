package com.example.labmanagement.maintenance.web;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.maintenance.application.MaintenanceResponse;
import com.example.labmanagement.maintenance.application.MaintenanceService;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
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
@RequestMapping("/maintenances")
public class MaintenanceWebController {

	private final MaintenanceService maintenanceService;

	public MaintenanceWebController(MaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;
	}

	@GetMapping
	String maintenances(@RequestParam(required = false) BaoTriTrangThai status,
			@RequestParam(required = false) String resourceId, @RequestParam(required = false) String assigneeId,
			@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Principal principal, Model model) {
		Page<MaintenanceResponse> result = maintenanceService.search(principal.getName(), status, resourceId,
				assigneeId, keyword, page, size);
		model.addAttribute("result", result);
		model.addAttribute("statuses", BaoTriTrangThai.values());
		model.addAttribute("resources", maintenanceService.resourceOptions());
		model.addAttribute("assignees", maintenanceService.assigneeOptions());
		model.addAttribute("status", status);
		model.addAttribute("resourceId", resourceId);
		model.addAttribute("assigneeId", assigneeId);
		model.addAttribute("keyword", keyword);
		return "maintenance/list";
	}

	@GetMapping("/new")
	String newMaintenance(@RequestParam(required = false) String incidentId,
			@RequestParam(required = false) String resourceId, Model model) {
		MaintenanceForms.CreateForm form = new MaintenanceForms.CreateForm();
		form.setIncidentId(incidentId);
		form.setResourceId(resourceId);
		model.addAttribute("maintenanceForm", form);
		return createForm(model);
	}

	@PostMapping
	String create(@Valid @ModelAttribute("maintenanceForm") MaintenanceForms.CreateForm form,
			BindingResult bindingResult, Principal principal, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return createForm(model);
		}
		try {
			MaintenanceResponse maintenance = maintenanceService.create(principal.getName(), form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã tạo phiếu bảo trì và chặn tài nguyên.");
			return "redirect:/maintenances/" + maintenance.id();
		} catch (ApiException exception) {
			bindingResult.reject("maintenance.create", exception.getMessage());
			return createForm(model);
		}
	}

	@GetMapping("/{id}")
	String detail(@PathVariable String id, Principal principal, Model model) {
		prepareDetail(model, maintenanceService.get(principal.getName(), id));
		return "maintenance/detail";
	}

	@PostMapping("/{id}/update")
	String update(@PathVariable String id, @Valid @ModelAttribute("updateForm") MaintenanceForms.UpdateForm form,
			BindingResult bindingResult, Principal principal, Model model, RedirectAttributes redirectAttributes) {
		if (form.getStatus() == BaoTriTrangThai.HOAN_THANH) {
			if (form.getEndAt() == null) {
				bindingResult.rejectValue("endAt", "maintenance.end.required",
						"Hoàn thành bảo trì bắt buộc có thời điểm kết thúc.");
			}
			if (form.getResult() == null || form.getResult().isBlank()) {
				bindingResult.rejectValue("result", "maintenance.result.required",
						"Hoàn thành bảo trì bắt buộc có kết quả.");
			}
		}
		if (bindingResult.hasErrors()) {
			prepareDetail(model, maintenanceService.get(principal.getName(), id));
			model.addAttribute("updateForm", form);
			return "maintenance/detail";
		}
		try {
			maintenanceService.update(principal.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã ghi nhận tiến độ bảo trì.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/maintenances/" + id;
	}

	private String createForm(Model model) {
		model.addAttribute("resources", maintenanceService.resourceOptions());
		model.addAttribute("assignees", maintenanceService.assigneeOptions());
		return "maintenance/form";
	}

	private void prepareDetail(Model model, MaintenanceResponse maintenance) {
		boolean terminal = maintenance.status() == BaoTriTrangThai.HOAN_THANH
				|| maintenance.status() == BaoTriTrangThai.DA_HUY;
		model.addAttribute("maintenance", maintenance);
		model.addAttribute("terminal", terminal);
		model.addAttribute("nextStatuses", nextStatuses(maintenance.status()));
		if (!terminal && !model.containsAttribute("updateForm")) {
			model.addAttribute("updateForm", MaintenanceForms.UpdateForm.from(maintenance));
		}
	}

	private List<BaoTriTrangThai> nextStatuses(BaoTriTrangThai current) {
		return switch (current) {
			case CHO_XU_LY -> List.of(BaoTriTrangThai.DANG_BAO_TRI, BaoTriTrangThai.DA_HUY);
			case DANG_BAO_TRI ->
				List.of(BaoTriTrangThai.DANG_BAO_TRI, BaoTriTrangThai.HOAN_THANH, BaoTriTrangThai.DA_HUY);
			case HOAN_THANH, DA_HUY -> List.of();
		};
	}
}
