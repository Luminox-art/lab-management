package com.example.labmanagement.usage.persistence;

import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhienSuDungRepository extends JpaRepository<PhienSuDung, Long> {

	@EntityGraph(attributePaths = {"schedule", "schedule.registration", "schedule.registration.creator",
			"schedule.registration.room", "schedule.period", "checkedInBy", "checkedOutBy"})
	@Query("select session from PhienSuDung session where session.id = :id")
	Optional<PhienSuDung> findDetailById(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select session from PhienSuDung session
			join fetch session.schedule schedule
			join fetch schedule.registration registration
			join fetch registration.creator creator
			join fetch registration.room room
			join fetch schedule.period period
			where session.id = :id
			""")
	Optional<PhienSuDung> findDetailByIdForUpdate(@Param("id") Long id);

	@Query("""
			select session from PhienSuDung session
			join fetch session.schedule schedule
			join fetch schedule.registration registration
			join fetch registration.creator creator
			join fetch registration.room room
			join fetch schedule.period period
			where registration.id = :registrationId
			order by session.usageDate, period.startTime, session.id
			""")
	List<PhienSuDung> findAllByRegistrationId(@Param("registrationId") String registrationId);

	@Query("""
			select session from PhienSuDung session
			join fetch session.schedule schedule
			join fetch schedule.registration registration
			join fetch registration.creator creator
			join fetch registration.room room
			join fetch schedule.period period
			where registration.creator.id = :creatorId
			and session.usageDate between :from and :to
			order by session.usageDate, period.startTime, session.id
			""")
	List<PhienSuDung> findAccessibleByCreatorId(@Param("creatorId") String creatorId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query("""
			select session from PhienSuDung session
			join fetch session.schedule schedule
			join fetch schedule.registration registration
			join fetch registration.creator creator
			join fetch registration.room room
			join fetch schedule.period period
			where session.usageDate between :from and :to
			order by session.usageDate, period.startTime, session.id
			""")
	List<PhienSuDung> findAccessibleToManager(@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select session from PhienSuDung session
			join fetch session.schedule schedule
			join fetch schedule.registration registration
			join fetch schedule.period period
			where session.status = :status and session.usageDate <= :today
			order by session.usageDate, session.id
			""")
	List<PhienSuDung> findOverdueCandidatesForUpdate(@Param("status") PhienSuDungTrangThai status,
			@Param("today") LocalDate today);

	boolean existsBySchedule_IdAndUsageDate(Long scheduleId, LocalDate usageDate);

	boolean existsBySchedule_Registration_IdAndStatus(String registrationId, PhienSuDungTrangThai status);

	boolean existsBySchedule_Registration_IdAndStatusIn(String registrationId,
			Collection<PhienSuDungTrangThai> statuses);

	long countBySchedule_Registration_IdAndStatus(String registrationId, PhienSuDungTrangThai status);

	default long countActualUsageByRegistrationId(String registrationId) {
		return countBySchedule_Registration_IdAndStatus(registrationId, PhienSuDungTrangThai.HOAN_THANH);
	}

	@Query("""
			select case when count(session) > 0 then true else false end from PhienSuDung session
			where session.schedule.registration.id = :registrationId
			and (session.checkedInAt is not null or session.status in :startedStatuses)
			""")
	boolean existsStartedByRegistrationId(@Param("registrationId") String registrationId,
			@Param("startedStatuses") Collection<PhienSuDungTrangThai> startedStatuses);

	default boolean existsStartedByRegistrationId(String registrationId) {
		return existsStartedByRegistrationId(registrationId, Set.of(PhienSuDungTrangThai.DANG_SU_DUNG,
				PhienSuDungTrangThai.HOAN_THANH, PhienSuDungTrangThai.VANG_MAT));
	}
}
