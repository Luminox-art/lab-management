package com.example.labmanagement.registration.persistence;

import com.example.labmanagement.registration.domain.PhieuHuongDan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhieuHuongDanRepository extends JpaRepository<PhieuHuongDan, String> {

	@Query("""
			select supervision from PhieuHuongDan supervision
			join fetch supervision.instructor instructor
			join fetch instructor.role role
			where supervision.registration.id = :registrationId
			""")
	Optional<PhieuHuongDan> findByRegistrationId(@Param("registrationId") String registrationId);

	boolean existsByRegistration_IdAndInstructor_Id(String registrationId, String instructorId);

}
