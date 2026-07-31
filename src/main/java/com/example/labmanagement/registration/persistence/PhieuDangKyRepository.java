package com.example.labmanagement.registration.persistence;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
			where (:type is null or registration.type = :type)
			and (:status is null or registration.status = :status)
			""", countQuery = """
			select count(registration) from PhieuDangKy registration
			where (:type is null or registration.type = :type)
			and (:status is null or registration.status = :status)
			""")
	Page<PhieuDangKy> findQueueByStatus(@Param("type") LoaiPhieu type, @Param("status") PhieuDangKyTrangThai status,
			Pageable pageable);

	@EntityGraph(attributePaths = {"creator", "creator.role", "room"})
	@Query("select registration from PhieuDangKy registration where registration.id = :id")
	Optional<PhieuDangKy> findDetailById(@Param("id") String id);
}
