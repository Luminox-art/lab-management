package com.example.labmanagement.catalog.persistence;

import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ThietBiRepository extends JpaRepository<ThietBi, String> {

	Optional<ThietBi> findBySerialNumber(String serialNumber);

	boolean existsBySerialNumberAndIdNot(String serialNumber, String id);

	@Query(value = """
			select d from ThietBi d join fetch d.type t left join fetch d.room r
			where (:typeId is null or t.id = :typeId)
			and (:roomId is null or r.id = :roomId)
			and (:status is null or d.status = :status)
			and (:keyword is null or lower(d.id) like lower(concat('%', :keyword, '%'))
				or lower(d.name) like lower(concat('%', :keyword, '%'))
				or lower(coalesce(d.serialNumber, '')) like lower(concat('%', :keyword, '%')))
			""", countQuery = """
			select count(d) from ThietBi d join d.type t left join d.room r
			where (:typeId is null or t.id = :typeId)
			and (:roomId is null or r.id = :roomId)
			and (:status is null or d.status = :status)
			and (:keyword is null or lower(d.id) like lower(concat('%', :keyword, '%'))
				or lower(d.name) like lower(concat('%', :keyword, '%'))
				or lower(coalesce(d.serialNumber, '')) like lower(concat('%', :keyword, '%')))
			""")
	Page<ThietBi> search(@Param("typeId") String typeId, @Param("roomId") String roomId,
			@Param("status") ThietBiTrangThai status, @Param("keyword") String keyword, Pageable pageable);

	List<ThietBi> findAllByStatusNotOrderByNameAsc(ThietBiTrangThai status);

	@EntityGraph(attributePaths = {"type", "room"})
	List<ThietBi> findAllByStatusInOrderByNameAsc(Collection<ThietBiTrangThai> statuses);

	long countByType_Id(String typeId);

	long countByRoom_Id(String roomId);
}
