package com.example.labmanagement.catalog.persistence;

import com.example.labmanagement.catalog.domain.Phong;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhongRepository extends JpaRepository<Phong, String> {
}
