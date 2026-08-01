package com.example.labmanagement.maintenance.persistence;

import com.example.labmanagement.maintenance.domain.TienDoBaoTri;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TienDoBaoTriRepository extends JpaRepository<TienDoBaoTri, Long> {

	@EntityGraph(attributePaths = "updatedBy")
	List<TienDoBaoTri> findAllByMaintenance_IdOrderByOccurredAtAscIdAsc(String maintenanceId);
}
