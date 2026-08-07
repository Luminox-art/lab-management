package com.example.labmanagement.identity.domain;

import com.example.labmanagement.common.metadata.AuditedVersionedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "NguoiDung")
public class NguoiDung extends AuditedVersionedEntity {

	@Id
	@Column(name = "MaNguoiDung", nullable = false, length = 50)
	private String id;

	@Column(name = "HoTen", nullable = false, length = 150)
	private String fullName;

	@Column(name = "Email", nullable = false, unique = true, length = 254)
	private String email;

	@Column(name = "MatKhau", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "LopDonVi", length = 150)
	private String classOrUnit;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaVaiTro", nullable = false)
	private VaiTro role;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private NguoiDungTrangThai status;

	protected NguoiDung() {
	}

	public NguoiDung(String id, String fullName, String email, String passwordHash, String classOrUnit, VaiTro role,
			NguoiDungTrangThai status) {
		this.id = id;
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.classOrUnit = classOrUnit;
		this.role = role;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getClassOrUnit() {
		return classOrUnit;
	}

	public VaiTro getRole() {
		return role;
	}

	public NguoiDungTrangThai getStatus() {
		return status;
	}

	public void updateProfile(String fullName, String email, String classOrUnit) {
		this.fullName = fullName;
		this.email = email;
		this.classOrUnit = classOrUnit;
	}

	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void activate() {
		this.status = NguoiDungTrangThai.HOAT_DONG;
	}

	public void updateByAdministrator(String fullName, String email, String classOrUnit, VaiTro role,
			NguoiDungTrangThai status) {
		this.fullName = fullName;
		this.email = email;
		this.classOrUnit = classOrUnit;
		this.role = role;
		this.status = status;
	}
}
