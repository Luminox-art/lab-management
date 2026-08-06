package com.example.labmanagement.registration.repository;

import com.example.labmanagement.registration.domain.PhieuGiangDay;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhieuGiangDayRepository extends JpaRepository<PhieuGiangDay, String> {

	@Query("select teaching from PhieuGiangDay teaching where teaching.registration.id = :registrationId")
	Optional<PhieuGiangDay> findByRegistrationId(@Param("registrationId") String registrationId);

}
