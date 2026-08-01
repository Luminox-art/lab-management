package com.example.labmanagement.reporting.web;

import com.example.labmanagement.reporting.application.DashboardResponse;
import com.example.labmanagement.reporting.application.DashboardService;
import com.example.labmanagement.reporting.domain.DashboardGroup;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dashboard")
public class DashboardWebController {

	private final DashboardService dashboardService;

	public DashboardWebController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) DashboardGroup groupBy, Principal principal, Model model) {
		DashboardResponse dashboard = dashboardService.dashboard(principal.getName(), from, to, groupBy);
		model.addAttribute("dashboard", dashboard);
		model.addAttribute("groups", DashboardGroup.values());
		return "reporting/dashboard";
	}
}
