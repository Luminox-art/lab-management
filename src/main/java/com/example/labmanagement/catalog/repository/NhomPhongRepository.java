package com.example.labmanagement.catalog.repository;

import com.example.labmanagement.catalog.domain.NhomPhong;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NhomPhongRepository extends JpaRepository<NhomPhong, String> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
}
