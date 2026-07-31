package com.example.labmanagement.common.clock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TimeConfigurationTest {

	@Test
	void clockUsesUtc() {
		Clock clock = new TimeConfiguration().clock();

		assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
	}

	@Test
	void formatterUsesHoChiMinhTime() {
		VietnameseTimeFormatter formatter = new VietnameseTimeFormatter();

		assertThat(formatter.format(Instant.parse("2026-01-01T00:00:00Z"))).isEqualTo("01/01/2026 07:00");
	}
}
