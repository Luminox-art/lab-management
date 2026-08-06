package com.example.labmanagement.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import com.example.labmanagement.scheduling.dto.AvailabilityConflictType;
import com.example.labmanagement.scheduling.dto.AvailabilityResponse;
import com.example.labmanagement.scheduling.dto.CalendarEventType;
import com.example.labmanagement.scheduling.repository.TietHocRepository;
import com.example.labmanagement.scheduling.service.SchedulingService;
import java.time.LocalDate;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** MySQL acceptance for FR-08/09, UC-06/07, API-15/16 and TC-APR/REG. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
@Transactional
class SchedulingMySqlIntegrationTest {

	@Autowired
	private SchedulingService schedulingService;

	@Autowired
	private TietHocRepository periodRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void seedContainsAllSeventeenOrderedPeriods() {
		assertThat(periodRepository.findAllByOrderByIdAsc()).hasSize(17).extracting(period -> period.getId())
				.containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 17).boxed().toList());
	}

	@Test
	void approvedRoomAndAllocatedDeviceSchedulesProduceConcreteConflicts() {
		OccupiedSlot roomSlot = occupiedRoomSlot();
		AvailabilityResponse roomResult = schedulingService.checkAvailability(roomSlot.roomId(), List.of(),
				roomSlot.from(), roomSlot.to(), roomSlot.dayOfWeek(), roomSlot.periodId());
		assertThat(roomResult.conflicts()).anySatisfy(conflict -> {
			assertThat(conflict.type()).isEqualTo(AvailabilityConflictType.ROOM_REGISTRATION);
			assertThat(conflict.resourceId()).isEqualTo(roomSlot.roomId());
			assertThat(ScheduleDateCalculator.toSystemDay(conflict.date())).isEqualTo(roomSlot.dayOfWeek());
		});

		AllocatedDeviceSlot deviceSlot = allocatedDeviceSlot();
		AvailabilityResponse deviceResult = schedulingService.checkAvailability(null, List.of(deviceSlot.deviceId()),
				deviceSlot.from(), deviceSlot.to(), deviceSlot.dayOfWeek(), deviceSlot.periodId());
		assertThat(deviceResult.conflicts()).anySatisfy(conflict -> {
			assertThat(conflict.type()).isEqualTo(AvailabilityConflictType.DEVICE_REGISTRATION);
			assertThat(conflict.resourceId()).isEqualTo(deviceSlot.deviceId());
			assertThat(ScheduleDateCalculator.toSystemDay(conflict.date())).isEqualTo(deviceSlot.dayOfWeek());
		});
	}

	@Test
	void effectiveNullDayAndNullPeriodBlockAppliesToEverySlot() {
		AllDayBlock block = allDayRoomBlock();
		int requestedDay = ScheduleDateCalculator.toSystemDay(block.from());
		AvailabilityResponse result = schedulingService.checkAvailability(block.roomId(), List.of(), block.from(),
				block.to(), requestedDay, 1);

		assertThat(result.conflicts())
				.filteredOn(conflict -> conflict.type() == AvailabilityConflictType.BLOCKED_SCHEDULE).isNotEmpty()
				.allSatisfy(conflict -> assertThat(conflict.message()).startsWith("Lịch chặn cả ngày"));
	}

	@Test
	void calendarDistinguishesInUseStateAndDoesNotExposeRegistrationDetails() throws Exception {
		OccupiedSlot slot = occupiedRoomSlot();
		LocalDate occurrence = ScheduleDateCalculator.datesForSystemDay(slot.from(), slot.to(), slot.dayOfWeek())
				.getFirst();
		jdbcTemplate.update("UPDATE PhieuDangKy SET TrangThai = 'DANG_SU_DUNG' WHERE MaPhieu = ?",
				slot.registrationId());

		assertThat(schedulingService.roomCalendar(slot.roomId(), occurrence, occurrence).events())
				.anySatisfy(event -> assertThat(event.type()).isEqualTo(CalendarEventType.IN_USE_REGISTRATION));

		mockMvc.perform(get("/api/v1/rooms/{id}/calendar", slot.roomId()).with(user("student").roles("SV"))
				.param("from", occurrence.toString()).param("to", occurrence.toString())).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.events[*].type").value(Matchers.hasItem("IN_USE_REGISTRATION")))
				.andExpect(content().string(Matchers.not(Matchers.containsString(slot.registrationId()))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("Mục đích sử dụng"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("email"))));
	}

	private OccupiedSlot occupiedRoomSlot() {
		return jdbcTemplate.queryForObject("""
				SELECT p.MaPhieu, p.MaPhong, l.Thu, l.MaTiet, p.NgayBatDau, p.NgayKetThuc
				FROM PhieuDangKy p
				JOIN LichDangKy l ON l.MaPhieu = p.MaPhieu
				WHERE p.TrangThai = 'DA_DUYET'
				ORDER BY p.MaPhieu, l.MaLich
				LIMIT 1
				""",
				(resultSet, rowNumber) -> new OccupiedSlot(resultSet.getString("MaPhieu"),
						resultSet.getString("MaPhong"), resultSet.getInt("Thu"), resultSet.getInt("MaTiet"),
						resultSet.getObject("NgayBatDau", LocalDate.class),
						resultSet.getObject("NgayKetThuc", LocalDate.class)));
	}

	private AllocatedDeviceSlot allocatedDeviceSlot() {
		return jdbcTemplate.queryForObject("""
				SELECT a.MaThietBi, l.Thu, l.MaTiet, p.NgayBatDau, p.NgayKetThuc
				FROM PhieuDangKyThietBi a
				JOIN PhieuDangKy p ON p.MaPhieu = a.MaPhieu
				JOIN LichDangKy l ON l.MaPhieu = p.MaPhieu
				WHERE a.DaPhanBo = TRUE AND p.TrangThai = 'DA_DUYET'
				ORDER BY p.MaPhieu, a.MaThietBi, l.MaLich
				LIMIT 1
				""", (resultSet, rowNumber) -> new AllocatedDeviceSlot(resultSet.getString("MaThietBi"),
				resultSet.getInt("Thu"), resultSet.getInt("MaTiet"), resultSet.getObject("NgayBatDau", LocalDate.class),
				resultSet.getObject("NgayKetThuc", LocalDate.class)));
	}

	private AllDayBlock allDayRoomBlock() {
		return jdbcTemplate.queryForObject("""
				SELECT resource.MaPhong, blocked.NgayBatDau, blocked.NgayKetThuc
				FROM LichChan blocked
				JOIN TaiNguyen resource ON resource.MaTaiNguyen = blocked.MaTaiNguyen
				WHERE blocked.TrangThai = 'HIEU_LUC'
				  AND blocked.Thu IS NULL
				  AND blocked.MaTiet IS NULL
				  AND resource.LoaiTaiNguyen = 'PHONG'
				ORDER BY blocked.MaLichChan
				LIMIT 1
				""",
				(resultSet, rowNumber) -> new AllDayBlock(resultSet.getString("MaPhong"),
						resultSet.getObject("NgayBatDau", LocalDate.class),
						resultSet.getObject("NgayKetThuc", LocalDate.class)));
	}

	private record OccupiedSlot(String registrationId, String roomId, int dayOfWeek, int periodId, LocalDate from,
			LocalDate to) {
	}

	private record AllocatedDeviceSlot(String deviceId, int dayOfWeek, int periodId, LocalDate from, LocalDate to) {
	}

	private record AllDayBlock(String roomId, LocalDate from, LocalDate to) {
	}
}
