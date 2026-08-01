package com.example.labmanagement.notification.application;

import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.persistence.NguoiDungRepository;
import com.example.labmanagement.notification.persistence.NotificationProjectionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private static final int MAX_PAGE_SIZE = 100;
	private final NguoiDungRepository userRepository;
	private final NotificationProjectionRepository projectionRepository;
	private final Clock clock;

	public NotificationService(NguoiDungRepository userRepository,
			NotificationProjectionRepository projectionRepository, Clock clock) {
		this.userRepository = userRepository;
		this.projectionRepository = projectionRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> notifications(String email, boolean unreadOnly, int page, int size) {
		NguoiDung user = activeUser(email);
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw validation("Trang phải không âm và kích thước trang phải từ 1 đến 100.");
		}
		LocalDate today = LocalDate.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE));
		List<NotificationResponse> accessible = projectionRepository
				.findAccessible(user.getId(), today, today.plusDays(7)).stream()
				.filter(item -> !unreadOnly || item.unread())
				.sorted(Comparator.comparing(NotificationResponse::occurredAt).reversed()
						.thenComparing(NotificationResponse::id, Comparator.reverseOrder()))
				.toList();
		int fromIndex = Math.min(page * size, accessible.size());
		int toIndex = Math.min(fromIndex + size, accessible.size());
		return new PageImpl<>(accessible.subList(fromIndex, toIndex), PageRequest.of(page, size), accessible.size());
	}

	private NguoiDung activeUser(String email) {
		String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
		NguoiDung user = normalized == null ? null : userRepository.findByEmailIgnoreCase(normalized).orElse(null);
		if (user == null || user.getStatus() != NguoiDungTrangThai.HOAT_DONG) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
					"Chỉ tài khoản đang hoạt động được xem thông báo.");
		}
		return user;
	}

	private ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}
}
