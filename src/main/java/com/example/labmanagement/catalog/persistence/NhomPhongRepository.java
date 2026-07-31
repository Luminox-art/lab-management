package com.example.labmanagement.catalog.persistence;

import com.example.labmanagement.catalog.domain.NhomPhong;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NhomPhongRepository extends JpaRepository<NhomPhong, String> {
}
