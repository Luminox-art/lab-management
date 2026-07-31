package com.example.labmanagement.common.clock;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

	public static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
