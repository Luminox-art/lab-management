package com.example.labmanagement.scheduling.persistence;

import com.example.labmanagement.scheduling.domain.TietHoc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TietHocRepository extends JpaRepository<TietHoc, Integer> {
}
