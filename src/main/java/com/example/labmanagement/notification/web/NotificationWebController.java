package com.example.labmanagement.notification.web;

import com.example.labmanagement.notification.application.NotificationResponse;
import com.example.labmanagement.notification.application.NotificationService;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notifications")
public class NotificationWebController {

	private final NotificationService notificationService;

	public NotificationWebController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	String notifications(@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Principal principal, Model model) {
		Page<NotificationResponse> result = notificationService.notifications(principal.getName(), unreadOnly, page,
				size);
		model.addAttribute("result", result);
		model.addAttribute("unreadOnly", unreadOnly);
		return "notification/list";
	}
}
