package com.example.labmanagement.maintenance.repository;

import com.example.labmanagement.maintenance.domain.BaoTri;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BaoTriRepository extends JpaRepository<BaoTri, String> {

	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "assignee"})
	@Query("select maintenance from BaoTri maintenance where maintenance.id = :id")
	Optional<BaoTri> findDetailById(@Param("id") String id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "assignee"})
	@Query("select maintenance from BaoTri maintenance where maintenance.id = :id")
	Optional<BaoTri> findDetailByIdForUpdate(@Param("id") String id);

	@EntityGraph(attributePaths = {"resource", "resource.room", "resource.device", "assignee"})
	@Query(value = """
			select maintenance from BaoTri maintenance
			where (:status is null or maintenance.status = :status)
			and (:resourceId is null or maintenance.resource.id = :resourceId)
			and (:assigneeId is null or maintenance.assignee.id = :assigneeId)
			and (:keyword is null
			     or lower(maintenance.id) like lower(concat('%', :keyword, '%'))
			     or lower(maintenance.content) like lower(concat('%', :keyword, '%'))
			     or lower(maintenance.resource.id) like lower(concat('%', :keyword, '%')))
			""", countQuery = """
			select count(maintenance) from BaoTri maintenance
			where (:status is null or maintenance.status = :status)
			and (:resourceId is null or maintenance.resource.id = :resourceId)
			and (:assigneeId is null or maintenance.assignee.id = :assigneeId)
			and (:keyword is null
			     or lower(maintenance.id) like lower(concat('%', :keyword, '%'))
			     or lower(maintenance.content) like lower(concat('%', :keyword, '%'))
			     or lower(maintenance.resource.id) like lower(concat('%', :keyword, '%')))
			""")
	Page<BaoTri> search(@Param("status") BaoTriTrangThai status, @Param("resourceId") String resourceId,
			@Param("assigneeId") String assigneeId, @Param("keyword") String keyword, Pageable pageable);

	boolean existsByResource_IdAndStatusIn(String resourceId, Collection<BaoTriTrangThai> statuses);
}
