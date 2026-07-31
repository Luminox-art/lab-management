package com.example.labmanagement.usage.persistence;

import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhienSuDungRepository extends JpaRepository<PhienSuDung, Long> {

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
