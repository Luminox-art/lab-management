package com.example.labmanagement.usage.web;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.usage.application.UsageSessionResponse;
import com.example.labmanagement.usage.application.UsageSessionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
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
@RequestMapping("/sessions")
public class UsageSessionWebController {

	private final UsageSessionService sessionService;

	public UsageSessionWebController(UsageSessionService sessionService) {
		this.sessionService = sessionService;
	}

	@GetMapping
	String sessions(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			Principal principal, Model model) {
		model.addAttribute("sessions", sessionService.listAccessible(principal.getName(), from, to));
		model.addAttribute("from", from);
		model.addAttribute("to", to);
		return "usage/list";
	}

	@GetMapping("/{id}")
	String detail(@PathVariable Long id, Principal principal, Model model) {
		UsageSessionResponse session = sessionService.get(principal.getName(), id);
		prepareModel(model, session);
		return "usage/detail";
	}

	@PostMapping("/{id}/check-in")
	String checkIn(@PathVariable Long id, @Valid @ModelAttribute("checkInForm") UsageSessionForms.CheckInForm form,
			BindingResult bindingResult, Principal principal, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			prepareModel(model, sessionService.get(principal.getName(), id));
			model.addAttribute("checkInForm", form);
			return "usage/detail";
		}
		try {
			sessionService.checkIn(principal.getName(), id, form.toRequest());
			redirectAttributes.addFlashAttribute("success", "Đã check-in và ghi nhận bàn giao thiết bị.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/sessions/" + id;
	}

	@PostMapping("/{id}/check-out")
	String checkOut(@PathVariable Long id, @Valid @ModelAttribute("checkOutForm") UsageSessionForms.CheckOutForm form,
			BindingResult bindingResult, Principal principal, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			prepareModel(model, sessionService.get(principal.getName(), id));
			model.addAttribute("checkOutForm", form);
			return "usage/detail";
		}
		try {
			UsageSessionResponse result = sessionService.checkOut(principal.getName(), id, form.toRequest());
			String message = result.incidentIds().isEmpty()
					? "Đã check-out và xác nhận thu hồi thiết bị."
					: "Đã check-out và tạo " + result.incidentIds().size() + " sự cố.";
			redirectAttributes.addFlashAttribute("success", message);
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/sessions/" + id;
	}

	private void prepareModel(Model model, UsageSessionResponse session) {
		model.addAttribute("usageSession", session);
		model.addAttribute("severities", MucDoSuCo.values());
		if (!model.containsAttribute("checkInForm")) {
			model.addAttribute("checkInForm", UsageSessionForms.CheckInForm.from(session));
		}
		if (!model.containsAttribute("checkOutForm")) {
			model.addAttribute("checkOutForm", UsageSessionForms.CheckOutForm.from(session));
		}
	}
}
