package com.example.labmanagement.common.clock;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class VietnameseTimeFormatter {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter
			.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN")).withZone(TimeConfiguration.DISPLAY_ZONE);

	public String format(Instant instant) {
		return FORMATTER.format(instant);
	}
}
