package com.example.labmanagement.usage.repository;

import com.example.labmanagement.usage.domain.PhienSuDungThietBi;
import com.example.labmanagement.usage.domain.PhienSuDungThietBiId;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhienSuDungThietBiRepository extends JpaRepository<PhienSuDungThietBi, PhienSuDungThietBiId> {

	@Query("""
			select item from PhienSuDungThietBi item
			join fetch item.device device
			join fetch device.type type
			where item.session.id = :sessionId
			order by device.id
			""")
	List<PhienSuDungThietBi> findAllBySessionId(@Param("sessionId") Long sessionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select item from PhienSuDungThietBi item
			join fetch item.device device
			join fetch device.type type
			where item.session.id = :sessionId
			order by device.id
			""")
	List<PhienSuDungThietBi> findAllBySessionIdForUpdate(@Param("sessionId") Long sessionId);
}
