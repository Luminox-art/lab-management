package com.example.labmanagement.registration.persistence;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhieuDangKyRepository extends JpaRepository<PhieuDangKy, String> {

	@EntityGraph(attributePaths = {"creator", "room"})
	@Query(value = """
			select registration from PhieuDangKy registration
			where registration.creator.id = :creatorId
			and (:type is null or registration.type = :type)
			and (:status is null or registration.status = :status)
			""", countQuery = """
			select count(registration) from PhieuDangKy registration
			where registration.creator.id = :creatorId
			and (:type is null or registration.type = :type)
			and (:status is null or registration.status = :status)
			""")
	Page<PhieuDangKy> findOwnedByCreatorId(@Param("creatorId") String creatorId, @Param("type") LoaiPhieu type,
			@Param("status") PhieuDangKyTrangThai status, Pageable pageable);

	@EntityGraph(attributePaths = {"creator", "room"})
	@Query(value = """
			select distinct registration from PhieuDangKy registration
			left join PhieuHuongDan supervision on supervision.registration = registration
			where (registration.creator.id = :instructorId or supervision.instructor.id = :instructorId)
			and (:type is null or registration.type = :type)
			and (:status is null or registration.status = :status)
			""", countQuery = """
			select count(distinct registration) from PhieuDangKy registration
			left join PhieuHuongDan supervision on supervision.registration = registration
			where (registration.creator.id = :instructorId or supervision.instructor.id = :instructorId)
			and (:type is null or registration.type = :type)
			and (:status is null or registration.status = :status)
			""")
	Page<PhieuDangKy> findAccessibleToInstructor(@Param("instructorId") String instructorId,
			@Param("type") LoaiPhieu type, @Param("status") PhieuDangKyTrangThai status, Pageable pageable);

	@EntityGraph(attributePaths = {"creator", "room"})
	@Query(value = """
			select registration from PhieuDangKy registration
			join registration.creator creator
			join registration.room room
			where (:type is null or registration.type = :type)
			and registration.status = :status
			and (:roomId is null or room.id = :roomId)
			and (:date is null or registration.startDate <= :date and registration.endDate >= :date)
			and (:creator is null or lower(creator.id) like lower(concat('%', :creator, '%'))
				or lower(creator.fullName) like lower(concat('%', :creator, '%')))
			""", countQuery = """
			select count(registration) from PhieuDangKy registration
			join registration.creator creator
			join registration.room room
			where (:type is null or registration.type = :type)
			and registration.status = :status
			and (:roomId is null or room.id = :roomId)
			and (:date is null or registration.startDate <= :date and registration.endDate >= :date)
			and (:creator is null or lower(creator.id) like lower(concat('%', :creator, '%'))
				or lower(creator.fullName) like lower(concat('%', :creator, '%')))
			""")
	Page<PhieuDangKy> findQueue(@Param("type") LoaiPhieu type, @Param("status") PhieuDangKyTrangThai status,
			@Param("roomId") String roomId, @Param("date") LocalDate date, @Param("creator") String creator,
			Pageable pageable);

	@EntityGraph(attributePaths = {"creator", "creator.role", "room"})
	@Query("select registration from PhieuDangKy registration where registration.id = :id")
	Optional<PhieuDangKy> findDetailById(@Param("id") String id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select registration from PhieuDangKy registration where registration.id = :id")
	Optional<PhieuDangKy> findDetailByIdForUpdate(@Param("id") String id);
}
