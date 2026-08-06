package com.example.labmanagement.registration.repository;

import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBiId;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhieuDangKyThietBiRepository extends JpaRepository<PhieuDangKyThietBi, PhieuDangKyThietBiId> {

	@Query("""
			select allocation from PhieuDangKyThietBi allocation
			join fetch allocation.device device
			join fetch device.type type
			left join fetch device.room room
			where allocation.registration.id = :registrationId
			order by device.name, device.id
			""")
	List<PhieuDangKyThietBi> findAllByRegistrationId(@Param("registrationId") String registrationId);

	@Query("""
			select allocation from PhieuDangKyThietBi allocation
			join fetch allocation.device device
			join fetch device.type type
			left join fetch device.room room
			where allocation.registration.id = :registrationId
			and allocation.allocated = true
			order by device.id
			""")
	List<PhieuDangKyThietBi> findAllocatedByRegistrationId(@Param("registrationId") String registrationId);

	@Query("""
			select allocation.device.id from PhieuDangKyThietBi allocation
			where allocation.registration.id = :registrationId
			order by allocation.device.id
			""")
	List<String> findRequestedDeviceIds(@Param("registrationId") String registrationId);

	@Query("""
			select allocation from PhieuDangKyThietBi allocation
			join fetch allocation.registration registration
			join fetch allocation.device device
			where device.id in :deviceIds
			and allocation.allocated = true
			and registration.status in :statuses
			and registration.startDate <= :to
			and registration.endDate >= :from
			order by registration.id, device.id
			""")
	List<PhieuDangKyThietBi> findAllocatedCandidates(@Param("deviceIds") Collection<String> deviceIds,
			@Param("statuses") Collection<PhieuDangKyTrangThai> statuses, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	boolean existsByDevice_IdAndAllocatedTrueAndRegistration_StatusIn(String deviceId,
			Collection<PhieuDangKyTrangThai> statuses);

	boolean existsByRegistration_IdAndDevice_IdAndAllocatedTrue(String registrationId, String deviceId);
}
