package com.example.labmanagement.usage.service;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.repository.LichDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyRepository;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionGenerationService {

	private final PhieuDangKyRepository registrationRepository;
	private final LichDangKyRepository scheduleRepository;
	private final PhienSuDungRepository sessionRepository;

	public SessionGenerationService(PhieuDangKyRepository registrationRepository,
			LichDangKyRepository scheduleRepository, PhienSuDungRepository sessionRepository) {
		this.registrationRepository = registrationRepository;
		this.scheduleRepository = scheduleRepository;
		this.sessionRepository = sessionRepository;
	}

	@Transactional
	public int generateForRegistration(String registrationId) {
		PhieuDangKy registration = registrationRepository.findDetailByIdForUpdate(registrationId).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy phiếu đăng ký."));
		if (registration.getStatus() != PhieuDangKyTrangThai.DA_DUYET
				&& registration.getStatus() != PhieuDangKyTrangThai.DANG_SU_DUNG) {
			throw new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT,
					"Chỉ có thể sinh phiên cho phiếu đã được duyệt.");
		}

		List<LichDangKy> schedules = scheduleRepository.findAllByRegistrationId(registration.getId());
		Set<SessionKey> existing = new HashSet<>();
		for (PhienSuDung session : sessionRepository.findAllByRegistrationId(registration.getId())) {
			existing.add(new SessionKey(session.getSchedule().getId(), session.getUsageDate()));
		}

		List<PhienSuDung> missing = new ArrayList<>();
		for (LichDangKy schedule : schedules) {
			for (LocalDate date : ScheduleDateCalculator.datesForSystemDay(registration.getStartDate(),
					registration.getEndDate(), schedule.getDayOfWeek())) {
				SessionKey key = new SessionKey(schedule.getId(), date);
				if (existing.add(key)) {
					missing.add(
							new PhienSuDung(schedule, date, PhienSuDungTrangThai.CHUA_BAT_DAU, null, null, null, null));
				}
			}
		}
		if (!missing.isEmpty()) {
			sessionRepository.saveAll(missing);
		}
		return missing.size();
	}

	private record SessionKey(Long scheduleId, LocalDate usageDate) {
	}
}
