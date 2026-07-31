package com.example.labmanagement.scheduling.persistence;

import com.example.labmanagement.scheduling.domain.TietHoc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TietHocRepository extends JpaRepository<TietHoc, Integer> {

	List<TietHoc> findAllByOrderByIdAsc();
}
