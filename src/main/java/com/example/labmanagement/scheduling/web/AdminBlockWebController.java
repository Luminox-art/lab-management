package com.example.labmanagement.scheduling.web;

import com.example.labmanagement.catalog.application.CatalogService;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.scheduling.application.AdminBlockCreationResponse;
import com.example.labmanagement.scheduling.application.AdminBlockService;
import com.example.labmanagement.scheduling.application.SchedulingService;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin-blocks")
public class AdminBlockWebController {

	private final AdminBlockService adminBlockService;
	private final CatalogService catalogService;
	private final SchedulingService schedulingService;

	public AdminBlockWebController(AdminBlockService adminBlockService, CatalogService catalogService,
			SchedulingService schedulingService) {
		this.adminBlockService = adminBlockService;
		this.catalogService = catalogService;
		this.schedulingService = schedulingService;
	}

	@GetMapping
	String page(Principal principal, Model model) {
		if (!model.containsAttribute("adminBlockForm")) {
			AdminBlockForm form = new AdminBlockForm();
			form.setStartDate(LocalDate.now());
			form.setEndDate(LocalDate.now());
			model.addAttribute("adminBlockForm", form);
		}
		preparePage(principal.getName(), model);
		return "schedule/admin-blocks";
	}

	@PostMapping
	String create(@Valid @ModelAttribute("adminBlockForm") AdminBlockForm form, BindingResult bindingResult,
			Principal principal, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			preparePage(principal.getName(), model);
			return "schedule/admin-blocks";
		}
		try {
			AdminBlockCreationResponse result = adminBlockService.create(principal.getName(), form.toRequest());
			redirectAttributes.addFlashAttribute("success",
					"Đã tạo " + result.blocks().size() + " lịch chặn hiệu lực.");
			return "redirect:/admin-blocks";
		} catch (ApiException exception) {
			bindingResult.reject("adminBlock.create", exception.getMessage());
			preparePage(principal.getName(), model);
			return "schedule/admin-blocks";
		}
	}

	@PostMapping("/{id}/cancel")
	String cancel(@PathVariable long id, Principal principal, RedirectAttributes redirectAttributes) {
		try {
			adminBlockService.cancel(principal.getName(), id);
			redirectAttributes.addFlashAttribute("success", "Đã hủy lịch chặn.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/admin-blocks";
	}

	private void preparePage(String actorEmail, Model model) {
		model.addAttribute("blocks", adminBlockService.findAll(actorEmail));
		model.addAttribute("rooms", catalogService.roomsForFilter());
		model.addAttribute("devices", catalogService.selectableDevices());
		model.addAttribute("periods", schedulingService.periods());
		model.addAttribute("dayOptions", dayOptions());
	}

	private List<DayOption> dayOptions() {
		List<DayOption> options = new ArrayList<>();
		for (int day = ScheduleDateCalculator.MONDAY; day <= ScheduleDateCalculator.SUNDAY; day++) {
			options.add(new DayOption(day, ScheduleDateCalculator.systemDayLabel(day)));
		}
		return List.copyOf(options);
	}

	public record DayOption(int value, String label) {
	}
}
