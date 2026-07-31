package com.example.labmanagement.catalog.persistence;

import com.example.labmanagement.catalog.domain.TaiNguyen;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaiNguyenRepository extends JpaRepository<TaiNguyen, String> {

	Optional<TaiNguyen> findByRoom_Id(String roomId);

	Optional<TaiNguyen> findByDevice_Id(String deviceId);

	long countByRoom_Id(String roomId);

	long countByDevice_Id(String deviceId);
}
