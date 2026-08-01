package com.example.labmanagement.scheduling.persistence;

import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichChanRepository extends JpaRepository<LichChan, Long> {

	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "period", "creator"})
	List<LichChan> findAllByOrderByStartDateDescIdDesc();

	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "period", "creator"})
	Optional<LichChan> findDetailById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select blocked from LichChan blocked where blocked.id = :id")
	Optional<LichChan> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select blocked from LichChan blocked
			join fetch blocked.resource resource
			left join fetch blocked.period period
			where resource.id in :resourceIds
			and blocked.status = :status
			and blocked.startDate <= :to
			and blocked.endDate >= :from
			order by blocked.startDate, blocked.id
			""")
	List<LichChan> findCandidates(@Param("resourceIds") Collection<String> resourceIds,
			@Param("status") LichChanTrangThai status, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
