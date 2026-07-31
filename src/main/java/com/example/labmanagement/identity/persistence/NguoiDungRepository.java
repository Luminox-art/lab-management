package com.example.labmanagement.identity.persistence;

import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, String> {

	@EntityGraph(attributePaths = "role")
	Optional<NguoiDung> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, String id);

	@Query("""
			select user from NguoiDung user
			where (:status is null or user.status = :status)
			  and (:roleId is null or user.role.id = :roleId)
			  and (:keyword is null
			       or lower(user.id) like lower(concat('%', :keyword, '%'))
			       or lower(user.fullName) like lower(concat('%', :keyword, '%'))
			       or lower(user.email) like lower(concat('%', :keyword, '%')))
			""")
	Page<NguoiDung> search(@Param("status") NguoiDungTrangThai status, @Param("roleId") String roleId,
			@Param("keyword") String keyword, Pageable pageable);
}
