package com.example.labmanagement.common.metadata;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class VersionedEntity {

	@Version
	@Column(name = "VersionNo", nullable = false)
	private long version;

	public long getVersion() {
		return version;
	}
}
