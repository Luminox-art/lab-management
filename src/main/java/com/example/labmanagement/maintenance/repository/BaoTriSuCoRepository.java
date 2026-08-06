package com.example.labmanagement.maintenance.repository;

import com.example.labmanagement.maintenance.domain.BaoTriSuCo;
import com.example.labmanagement.maintenance.domain.BaoTriSuCoId;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaoTriSuCoRepository extends JpaRepository<BaoTriSuCo, BaoTriSuCoId> {

	boolean existsByIncident_Id(String incidentId);

	@EntityGraph(attributePaths = "incident")
	Optional<BaoTriSuCo> findByMaintenance_Id(String maintenanceId);
}
