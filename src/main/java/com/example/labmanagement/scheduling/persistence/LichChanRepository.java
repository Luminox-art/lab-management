package com.example.labmanagement.scheduling.persistence;

import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichChanRepository extends JpaRepository<LichChan, Long> {

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
