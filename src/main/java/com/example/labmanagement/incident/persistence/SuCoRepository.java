package com.example.labmanagement.incident.persistence;

import com.example.labmanagement.incident.domain.SuCo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuCoRepository extends JpaRepository<SuCo, String> {
}
