package com.example.labmanagement.registration.persistence;

import com.example.labmanagement.registration.domain.XuLyPhieu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XuLyPhieuRepository extends JpaRepository<XuLyPhieu, Long> {

	@Query("""
			select history from XuLyPhieu history
			join fetch history.handler handler
			where history.registration.id = :registrationId
			order by history.occurredAt, history.id
			""")
	List<XuLyPhieu> findAllByRegistrationId(@Param("registrationId") String registrationId);
}
