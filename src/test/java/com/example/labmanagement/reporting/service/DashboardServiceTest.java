package com.example.labmanagement.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.reporting.domain.DashboardGroup;
import com.example.labmanagement.reporting.dto.DashboardFrequencyResponse;
import com.example.labmanagement.reporting.dto.DashboardResponse;
import com.example.labmanagement.reporting.dto.DashboardSeverityResponse;
import com.example.labmanagement.reporting.repository.DashboardCounts;
import com.example.labmanagement.reporting.repository.DashboardQueryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private static final String EMAIL = "cb-dashboard@lab.local";
	@Mock
	private NguoiDungRepository userRepository;
	@Mock
	private DashboardQueryRepository queryRepository;
	private DashboardService service;

	@BeforeEach
	void setUp() {
		service = new DashboardService(userRepository, queryRepository,
				Clock.fixed(Instant.parse("2035-01-15T02:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void countsActualSessionsAndCalculatesCompletionAndAbsenceSeparately() {
		LocalDate from = LocalDate.of(2035, 1, 1);
		LocalDate to = LocalDate.of(2035, 1, 15);
		when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(manager()));
		when(queryRepository.counts(from, to)).thenReturn(new DashboardCounts(8, 6, 2, 2, 3, 1));
		when(queryRepository.frequencies(from, to, DashboardGroup.PHONG))
				.thenReturn(List.of(new DashboardFrequencyResponse(DashboardGroup.PHONG, "P1", "Lab 1", 4, 0),
						new DashboardFrequencyResponse(DashboardGroup.PHONG, "P2", "Lab 2", 2, 0)));
		when(queryRepository.incidentSeverities(from, to)).thenReturn(Map.of(MucDoSuCo.CAO, 3L));
		when(queryRepository.activeMaintenances(to)).thenReturn(List.of());

		DashboardResponse result = service.dashboard(EMAIL, from, to, DashboardGroup.PHONG);

		assertThat(result.summary().completionRate()).isEqualTo(75.0);
		assertThat(result.summary().absenceRate()).isEqualTo(20.0);
		assertThat(result.frequencies()).extracting(DashboardFrequencyResponse::relativePercent).containsExactly(100,
				50);
		assertThat(result.incidentSeverities()).extracting(DashboardSeverityResponse::count).containsExactly(0L, 0L, 3L,
				0L);
	}

	@Test
	void rejectsNonManagerAndReversedDateRange() {
		NguoiDung teacher = new NguoiDung("GV-DASH", "Giảng viên", EMAIL, "hash", null, new VaiTro("GV", "Giảng viên"),
				NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher))
				.thenReturn(Optional.of(manager()));

		assertThatThrownBy(() -> service.dashboard(EMAIL, null, null, null)).isInstanceOfSatisfying(ApiException.class,
				exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
		assertThatThrownBy(() -> service.dashboard(EMAIL, LocalDate.of(2035, 2, 1), LocalDate.of(2035, 1, 1), null))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	private NguoiDung manager() {
		return new NguoiDung("CB-DASH", "Cán bộ", EMAIL, "hash", null, new VaiTro("CBQL", "Cán bộ quản lý"),
				NguoiDungTrangThai.HOAT_DONG);
	}
}
