package com.example.labmanagement.catalog.persistence;

import com.example.labmanagement.catalog.domain.ThietBi;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThietBiRepository extends JpaRepository<ThietBi, String> {

	Optional<ThietBi> findBySerialNumber(String serialNumber);
}
