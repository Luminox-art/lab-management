package com.example.labmanagement.catalog.persistence;

import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhongRepository extends JpaRepository<Phong, String> {

	@Query(value = """
			select p from Phong p join fetch p.group g
			where (:groupId is null or g.id = :groupId)
			and (:status is null or p.status = :status)
			and (:keyword is null or lower(p.id) like lower(concat('%', :keyword, '%'))
				or lower(p.name) like lower(concat('%', :keyword, '%'))
				or lower(p.location) like lower(concat('%', :keyword, '%')))
			""", countQuery = """
			select count(p) from Phong p join p.group g
			where (:groupId is null or g.id = :groupId)
			and (:status is null or p.status = :status)
			and (:keyword is null or lower(p.id) like lower(concat('%', :keyword, '%'))
				or lower(p.name) like lower(concat('%', :keyword, '%'))
				or lower(p.location) like lower(concat('%', :keyword, '%')))
			""")
	Page<Phong> search(@Param("groupId") String groupId, @Param("status") PhongTrangThai status,
			@Param("keyword") String keyword, Pageable pageable);

	List<Phong> findAllByStatusNotOrderByNameAsc(PhongTrangThai status);

	List<Phong> findAllByOrderByNameAsc();

	long countByGroup_Id(String groupId);
}
