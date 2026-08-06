package com.example.labmanagement.catalog.repository;

import com.example.labmanagement.catalog.domain.TaiNguyen;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaiNguyenRepository extends JpaRepository<TaiNguyen, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"room", "device"})
	@Query("select resource from TaiNguyen resource where resource.id = :id")
	Optional<TaiNguyen> findByIdForUpdate(@Param("id") String id);

	Optional<TaiNguyen> findByRoom_Id(String roomId);

	Optional<TaiNguyen> findByDevice_Id(String deviceId);

	List<TaiNguyen> findAllByDevice_IdIn(Collection<String> deviceIds);

	long countByRoom_Id(String roomId);

	long countByDevice_Id(String deviceId);

	@Query(value = """
			select resource.* from TaiNguyen resource
			where resource.MaPhong = :roomId or resource.MaThietBi in (:deviceIds)
			order by resource.MaTaiNguyen
			for update
			""", nativeQuery = true)
	List<TaiNguyen> lockForScheduling(@Param("roomId") String roomId, @Param("deviceIds") Collection<String> deviceIds);
}
