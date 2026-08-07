package com.example.labmanagement.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.dto.AdminUserUpdateRequest;
import com.example.labmanagement.identity.dto.PasswordChangeRequest;
import com.example.labmanagement.identity.dto.ProfileUpdateRequest;
import com.example.labmanagement.identity.dto.RegistrationRequest;
import com.example.labmanagement.identity.dto.RegistrationRole;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.identity.repository.VaiTroRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

	@Mock
	private NguoiDungRepository userRepository;

	@Mock
	private VaiTroRepository roleRepository;

	private BCryptPasswordEncoder passwordEncoder;
	private IdentityService identityService;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder(4);
		identityService = new IdentityService(userRepository, roleRepository, passwordEncoder);
	}

	@Test
	void registrationNormalizesInputHashesPasswordAndCreatesPendingUser() {
		VaiTro role = new VaiTro("GV", "Giảng viên");
		when(roleRepository.findById("GV")).thenReturn(Optional.of(role));
		when(userRepository.saveAndFlush(any(NguoiDung.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserProfileResponse response = identityService.register(new RegistrationRequest("  GV900  ", "  Nguyễn Văn A  ",
				"  Person@Example.EDU ", "strong-password", "  Khoa CNTT  ", RegistrationRole.GV));

		ArgumentCaptor<NguoiDung> captor = ArgumentCaptor.forClass(NguoiDung.class);
		verify(userRepository).saveAndFlush(captor.capture());
		NguoiDung saved = captor.getValue();
		assertThat(saved.getId()).isEqualTo("GV900");
		assertThat(saved.getFullName()).isEqualTo("Nguyễn Văn A");
		assertThat(saved.getEmail()).isEqualTo("person@example.edu");
		assertThat(saved.getClassOrUnit()).isEqualTo("Khoa CNTT");
		assertThat(saved.getStatus()).isEqualTo(NguoiDungTrangThai.CHO_DUYET);
		assertThat(passwordEncoder.matches("strong-password", saved.getPasswordHash())).isTrue();
		assertThat(response.roleId()).isEqualTo("GV");
	}

	@Test
	void registrationRejectsDuplicateIdentifierWithoutHashingOrSaving() {
		when(userRepository.existsById("SV001")).thenReturn(true);

		assertThatThrownBy(() -> identityService.register(new RegistrationRequest(" SV001 ", "Sinh viên",
				"new@example.edu", "strong-password", null, RegistrationRole.SV)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
		verify(userRepository, never()).saveAndFlush(any());
	}

	@Test
	void ownProfileUpdateCannotChangeRoleOrStatus() {
		VaiTro role = new VaiTro("SV", "Sinh viên");
		NguoiDung user = user("SV900", "old@example.edu", "old-password", role, NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findByEmailIgnoreCase("old@example.edu")).thenReturn(Optional.of(user));

		UserProfileResponse response = identityService.updateProfile("old@example.edu",
				new ProfileUpdateRequest("Tên mới", "NEW@example.edu", "CNTT01"));

		assertThat(response.email()).isEqualTo("new@example.edu");
		assertThat(user.getRole()).isSameAs(role);
		assertThat(user.getStatus()).isEqualTo(NguoiDungTrangThai.HOAT_DONG);
		verify(userRepository).flush();
	}

	@Test
	void passwordChangeRequiresCurrentPasswordAndStoresNewBcryptHash() {
		VaiTro role = new VaiTro("GV", "Giảng viên");
		NguoiDung user = user("GV900", "gv900@example.edu", "old-password", role, NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findByEmailIgnoreCase("gv900@example.edu")).thenReturn(Optional.of(user));

		identityService.changePassword("gv900@example.edu", new PasswordChangeRequest("old-password", "new-password"));

		assertThat(passwordEncoder.matches("new-password", user.getPasswordHash())).isTrue();
		verify(userRepository).flush();
	}

	@Test
	void administratorUpdateRejectsStaleVersion() {
		VaiTro role = new VaiTro("SV", "Sinh viên");
		NguoiDung user = user("SV900", "sv900@example.edu", "password", role, NguoiDungTrangThai.CHO_DUYET);
		when(userRepository.findById("SV900")).thenReturn(Optional.of(user));

		AdminUserUpdateRequest request = new AdminUserUpdateRequest("Sinh viên", "sv900@example.edu", null, "SV",
				NguoiDungTrangThai.HOAT_DONG, 1L);

		assertThatThrownBy(() -> identityService.updateUser("SV900", request)).isInstanceOfSatisfying(
				ApiException.class,
				exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
		verify(userRepository, never()).flush();
	}

	@Test
	void approvesPendingUserAndActivatesAccount() {
		VaiTro role = new VaiTro("SV", "Sinh viên");
		NguoiDung user = user("SV900", "sv900@example.edu", "password", role, NguoiDungTrangThai.CHO_DUYET);
		when(userRepository.findById("SV900")).thenReturn(Optional.of(user));

		UserProfileResponse response = identityService.approveUser("SV900", 0L);

		assertThat(response.status()).isEqualTo(NguoiDungTrangThai.HOAT_DONG);
		assertThat(user.getStatus()).isEqualTo(NguoiDungTrangThai.HOAT_DONG);
		verify(userRepository).flush();
	}

	private NguoiDung user(String id, String email, String password, VaiTro role, NguoiDungTrangThai status) {
		return new NguoiDung(id, "Người dùng", email, passwordEncoder.encode(password), null, role, status);
	}
}
