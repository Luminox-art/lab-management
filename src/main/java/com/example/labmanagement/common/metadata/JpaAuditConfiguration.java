package com.example.labmanagement.common.metadata;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
@Profile({"local", "test", "prod"})
public class JpaAuditConfiguration {

	@Bean
	DateTimeProvider utcDateTimeProvider(Clock clock) {
		return () -> Optional.of(clock.instant());
	}
}
