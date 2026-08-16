package com.example.labmanagement.reporting.service;

import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.RolePolicy;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.reporting.domain.DashboardGroup;
import com.example.labmanagement.reporting.dto.DashboardFrequencyResponse;
import com.example.labmanagement.reporting.dto.DashboardResponse;
import com.example.labmanagement.reporting.dto.DashboardSeverityResponse;
import com.example.labmanagement.reporting.dto.DashboardSummaryResponse;
import com.example.labmanagement.reporting.repository.DashboardCounts;
import com.example.labmanagement.reporting.repository.DashboardQueryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private final NguoiDungRepository userRepository;
	private final DashboardQueryRepository queryRepository;
	private final Clock clock;

	public DashboardService(NguoiDungRepository userRepository, DashboardQueryRepository queryRepository, Clock clock) {
		this.userRepository = userRepository;
		this.queryRepository = queryRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardResponse dashboard(String email, LocalDate requestedFrom, LocalDate requestedTo,
			DashboardGroup requestedGroup) {
		activeManager(email);
		LocalDate today = LocalDate.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE));
		LocalDate from = requestedFrom == null ? today.withDayOfMonth(1) : requestedFrom;
		LocalDate to = requestedTo == null ? today : requestedTo;
		DashboardGroup group = requestedGroup == null ? DashboardGroup.PHONG : requestedGroup;
		if (from.isAfter(to)) {
			throw validation("Ngày bắt đầu không được sau ngày kết thúc.");
		}

		DashboardCounts counts = queryRepository.counts(from, to);
		double completionRate = percentage(counts.completedSessions(), counts.actualSessions());
		double absenceRate = percentage(counts.absentSessions(), counts.actualSessions() + counts.absentSessions());
		DashboardSummaryResponse summary = new DashboardSummaryResponse(counts.actualSessions(),
				counts.completedSessions(), counts.inProgressSessions(), completionRate, counts.absentSessions(),
				absenceRate, counts.incidents(), counts.activeMaintenances());

		List<DashboardFrequencyResponse> rawFrequencies = queryRepository.frequencies(from, to, group);
		long maximum = rawFrequencies.stream().mapToLong(DashboardFrequencyResponse::count).max().orElse(0);
		List<DashboardFrequencyResponse> frequencies = rawFrequencies.stream()
				.map(item -> new DashboardFrequencyResponse(item.group(), item.id(), item.name(), item.count(),
						maximum == 0 ? 0 : (int) Math.round(item.count() * 100.0 / maximum)))
				.toList();

		Map<MucDoSuCo, Long> severityCounts = queryRepository.incidentSeverities(from, to);
		long severityTotal = severityCounts.values().stream().mapToLong(Long::longValue).sum();
		List<DashboardSeverityResponse> severities = Arrays.stream(MucDoSuCo.values())
				.map(severity -> new DashboardSeverityResponse(severity, severityCounts.getOrDefault(severity, 0L),
						severityTotal == 0
								? 0
								: (int) Math.round(severityCounts.getOrDefault(severity, 0L) * 100.0 / severityTotal)))
				.toList();

		return new DashboardResponse(from, to, group, summary, frequencies, severities,
				queryRepository.activeMaintenances(to));
	}

	private void activeManager(String email) {
		String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
		NguoiDung user = normalized == null ? null : userRepository.findByEmailIgnoreCase(normalized).orElse(null);
		if (user == null || user.getStatus() != NguoiDungTrangThai.HOAT_DONG
				|| !RolePolicy.isManager(user.getRole().getId())) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
					"Chỉ cán bộ quản lý đang hoạt động được xem dashboard.");
		}
	}

	private double percentage(long part, long total) {
		return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
	}

	private ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}
}
