package com.example.labmanagement.identity.persistence;

import com.example.labmanagement.identity.domain.NguoiDung;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, String> {

	Optional<NguoiDung> findByEmail(String email);
}
