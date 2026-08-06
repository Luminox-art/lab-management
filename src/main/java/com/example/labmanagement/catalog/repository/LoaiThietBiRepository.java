package com.example.labmanagement.catalog.repository;

import com.example.labmanagement.catalog.domain.LoaiThietBi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoaiThietBiRepository extends JpaRepository<LoaiThietBi, String> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
}
