package com.example.labmanagement.identity.service;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.dto.AdminUserUpdateRequest;
import com.example.labmanagement.identity.dto.PasswordChangeRequest;
import com.example.labmanagement.identity.dto.ProfileUpdateRequest;
import com.example.labmanagement.identity.dto.RegistrationRequest;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.identity.repository.VaiTroRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

	private final NguoiDungRepository userRepository;
	private final VaiTroRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public IdentityService(NguoiDungRepository userRepository, VaiTroRepository roleRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserProfileResponse register(RegistrationRequest request) {
		String identifier = request.identifier().trim();
		String email = normalizeEmail(request.email());
		if (userRepository.existsById(identifier) || userRepository.existsByEmailIgnoreCase(email)) {
			throw conflict("Mã định danh hoặc email đã được sử dụng.");
		}
		VaiTro role = findRole(request.role().name());
		NguoiDung user = new NguoiDung(identifier, request.fullName().trim(), email,
				passwordEncoder.encode(request.password()), normalizeOptional(request.organization()), role,
				NguoiDungTrangThai.CHO_DUYET);
		try {
			return toResponse(userRepository.saveAndFlush(user));
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Mã định danh hoặc email đã được sử dụng.");
		}
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getProfile(String email) {
		return toResponse(findUserByEmail(email));
	}

	@Transactional
	public UserProfileResponse updateProfile(String currentEmail, ProfileUpdateRequest request) {
		NguoiDung user = findUserByEmail(currentEmail);
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
			throw conflict("Email đã được sử dụng.");
		}
		user.updateProfile(request.fullName().trim(), email, normalizeOptional(request.classOrUnit()));
		return flushAndMap(user, "Email đã được sử dụng.");
	}

	@Transactional
	public void changePassword(String currentEmail, PasswordChangeRequest request) {
		NguoiDung user = findUserByEmail(currentEmail);
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY,
					"Mật khẩu hiện tại không đúng.");
		}
		user.changePassword(passwordEncoder.encode(request.newPassword()));
		userRepository.flush();
	}

	@Transactional(readOnly = true)
	public Page<UserProfileResponse> searchUsers(NguoiDungTrangThai status, String roleId, String keyword, int page,
			int size) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
				Sort.by(Sort.Direction.ASC, "id"));
		return userRepository.search(status, normalizeFilter(roleId), normalizeFilter(keyword), pageable)
				.map(this::toResponse);
	}

	@Transactional
	public UserProfileResponse updateUser(String id, AdminUserUpdateRequest request) {
		NguoiDung user = findUserById(id);
		if (user.getVersion() != request.version()) {
			throw conflict("Dữ liệu tài khoản đã được cập nhật bởi yêu cầu khác.");
		}
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
			throw conflict("Email đã được sử dụng.");
		}
		VaiTro role = findRole(request.roleId());
		user.updateByAdministrator(request.fullName().trim(), email, normalizeOptional(request.classOrUnit()), role,
				request.status());
		return flushAndMap(user, "Email đã được sử dụng.");
	}

	private UserProfileResponse flushAndMap(NguoiDung user, String conflictMessage) {
		try {
			userRepository.flush();
			return toResponse(user);
		} catch (DataIntegrityViolationException exception) {
			throw conflict(conflictMessage);
		}
	}

	private NguoiDung findUserByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
	}

	private NguoiDung findUserById(String id) {
		return userRepository.findById(id.trim()).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
	}

	private VaiTro findRole(String roleId) {
		return roleRepository.findById(roleId.trim().toUpperCase(Locale.ROOT)).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy vai trò."));
	}

	private UserProfileResponse toResponse(NguoiDung user) {
		return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getClassOrUnit(),
				user.getRole().getId(), user.getStatus(), user.getVersion(), user.getCreatedAt(), user.getUpdatedAt());
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String normalizeFilter(String value) {
		return normalizeOptional(value);
	}

	private ApiException conflict(String message) {
		return new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT, message);
	}
}
