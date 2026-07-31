package com.example.labmanagement.catalog.persistence;

import com.example.labmanagement.catalog.domain.TaiNguyen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaiNguyenRepository extends JpaRepository<TaiNguyen, String> {
}
