package com.example.labmanagement.maintenance.persistence;

import com.example.labmanagement.maintenance.domain.BaoTri;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaoTriRepository extends JpaRepository<BaoTri, String> {
}
