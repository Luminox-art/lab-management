package com.example.labmanagement.incident.repository;

import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuCoRepository extends JpaRepository<SuCo, String> {

	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "session", "session.schedule",
			"session.schedule.registration", "reporter", "handler"})
	@Query("select incident from SuCo incident where incident.id = :id")
	Optional<SuCo> findDetailById(@Param("id") String id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "session", "session.schedule",
			"session.schedule.registration", "reporter", "handler"})
	@Query("select incident from SuCo incident where incident.id = :id")
	Optional<SuCo> findDetailByIdForUpdate(@Param("id") String id);

	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "session", "session.schedule",
			"session.schedule.registration", "reporter", "handler"})
	@Query(value = """
			select incident from SuCo incident
			left join incident.session session
			left join session.schedule schedule
			left join schedule.registration registration
			where (:status is null or incident.status = :status)
			and (:severity is null or incident.severity = :severity)
			and (:resourceId is null or incident.resource.id = :resourceId)
			and (:sessionId is null or session.id = :sessionId)
			and (:keyword is null
			     or lower(incident.id) like lower(concat('%', :keyword, '%'))
			     or lower(incident.description) like lower(concat('%', :keyword, '%'))
			     or lower(incident.resource.id) like lower(concat('%', :keyword, '%')))
			and (:manager = true
			     or incident.reporter.id = :actorId
			     or registration.creator.id = :actorId
			     or exists (select supervision.id from PhieuHuongDan supervision
			                where supervision.registration = registration
			                and supervision.instructor.id = :actorId))
			""", countQuery = """
			select count(incident) from SuCo incident
			left join incident.session session
			left join session.schedule schedule
			left join schedule.registration registration
			where (:status is null or incident.status = :status)
			and (:severity is null or incident.severity = :severity)
			and (:resourceId is null or incident.resource.id = :resourceId)
			and (:sessionId is null or session.id = :sessionId)
			and (:keyword is null
			     or lower(incident.id) like lower(concat('%', :keyword, '%'))
			     or lower(incident.description) like lower(concat('%', :keyword, '%'))
			     or lower(incident.resource.id) like lower(concat('%', :keyword, '%')))
			and (:manager = true
			     or incident.reporter.id = :actorId
			     or registration.creator.id = :actorId
			     or exists (select supervision.id from PhieuHuongDan supervision
			                where supervision.registration = registration
			                and supervision.instructor.id = :actorId))
			""")
	Page<SuCo> search(@Param("actorId") String actorId, @Param("manager") boolean manager,
			@Param("status") SuCoTrangThai status, @Param("severity") MucDoSuCo severity,
			@Param("resourceId") String resourceId, @Param("sessionId") Long sessionId,
			@Param("keyword") String keyword, Pageable pageable);
}
