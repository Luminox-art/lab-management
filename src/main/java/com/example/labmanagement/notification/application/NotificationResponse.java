package com.example.labmanagement.notification.application;

import com.example.labmanagement.notification.domain.NotificationType;
import java.time.OffsetDateTime;

public record NotificationResponse(String id, NotificationType type, String title, String content,
		OffsetDateTime occurredAt, String targetUrl, boolean unread) {
}
