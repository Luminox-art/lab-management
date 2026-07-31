package com.example.labmanagement.registration.persistence;

import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichDangKyRepository extends JpaRepository<LichDangKy, Long> {

	@Query("""
			select schedule from LichDangKy schedule
			join fetch schedule.registration registration
			join fetch registration.room room
			join fetch schedule.period period
			where room.id = :roomId
			and registration.status in :statuses
			and registration.startDate <= :to
			and registration.endDate >= :from
			order by schedule.dayOfWeek, period.id
			""")
	List<LichDangKy> findRoomCandidates(@Param("roomId") String roomId,
			@Param("statuses") Collection<PhieuDangKyTrangThai> statuses, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query("""
			select schedule from LichDangKy schedule
			join fetch schedule.registration registration
			join fetch schedule.period period
			where registration.id in :registrationIds
			and schedule.dayOfWeek = :dayOfWeek
			and period.id = :periodId
			order by registration.id
			""")
	List<LichDangKy> findSlotCandidates(@Param("registrationIds") Collection<String> registrationIds,
			@Param("dayOfWeek") int dayOfWeek, @Param("periodId") int periodId);
}
