package com.example.labmanagement.notification.web;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.notification.application.NotificationPageMeta;
import com.example.labmanagement.notification.application.NotificationResponse;
import com.example.labmanagement.notification.application.NotificationService;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-33, FR-26, UC-22. */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationRestController {

	private final NotificationService notificationService;

	public NotificationRestController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	ApiResponse<List<NotificationResponse>> notifications(@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Principal principal) {
		Page<NotificationResponse> result = notificationService.notifications(principal.getName(), unreadOnly, page,
				size);
		return ApiResponse.of(result.getContent(), new NotificationPageMeta(result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages()));
	}
}
