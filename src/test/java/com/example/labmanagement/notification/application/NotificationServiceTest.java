package com.example.labmanagement.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.persistence.NguoiDungRepository;
import com.example.labmanagement.notification.domain.NotificationType;
import com.example.labmanagement.notification.persistence.NotificationProjectionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	private static final String EMAIL = "gv-notification@lab.local";
	@Mock
	private NguoiDungRepository userRepository;
	@Mock
	private NotificationProjectionRepository projectionRepository;
	private NotificationService service;

	@BeforeEach
	void setUp() {
		service = new NotificationService(userRepository, projectionRepository,
				Clock.fixed(Instant.parse("2035-01-15T02:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void returnsOnlyRepositoryScopedNotificationsInStableNewestFirstPages() {
		when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(activeUser()));
		when(projectionRepository.findAccessible("GV-NOTIFICATION", LocalDate.of(2035, 1, 15),
				LocalDate.of(2035, 1, 22)))
				.thenReturn(List.of(notification("OLD", "2035-01-15T09:00:00+07:00"),
						notification("NEW", "2035-01-16T09:00:00+07:00")));

		var result = service.notifications(EMAIL, true, 0, 1);

		assertThat(result.getContent()).extracting(NotificationResponse::id).containsExactly("NEW");
		assertThat(result.getTotalElements()).isEqualTo(2);
	}

	@Test
	void rejectsInvalidPageBeforeQueryingProjection() {
		when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(activeUser()));

		assertThatThrownBy(() -> service.notifications(EMAIL, false, -1, 20)).isInstanceOfSatisfying(ApiException.class,
				exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	private NguoiDung activeUser() {
		return new NguoiDung("GV-NOTIFICATION", "Giảng viên", EMAIL, "hash", null, new VaiTro("GV", "Giảng viên"),
				NguoiDungTrangThai.HOAT_DONG);
	}

	private NotificationResponse notification(String id, String occurredAt) {
		return new NotificationResponse(id, NotificationType.SU_CO, "Sự cố", "Nội dung",
				OffsetDateTime.parse(occurredAt), "/incidents/SC-1", true);
	}
}
