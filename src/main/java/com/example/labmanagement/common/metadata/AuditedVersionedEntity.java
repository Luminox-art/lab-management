package com.example.labmanagement.common.metadata;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditedVersionedEntity extends VersionedEntity {

	@CreatedDate
	@Column(name = "NgayTao", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "NgayCapNhat", nullable = false)
	private Instant updatedAt;

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	protected void markUpdatedAt(Instant instant) {
		this.updatedAt = instant;
	}
}
