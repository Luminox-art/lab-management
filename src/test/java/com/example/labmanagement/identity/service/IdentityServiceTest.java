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
import com.example.labmanagement.identity.domain.RolePolicy;
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
	private static final String MANAGER_EMAIL = "manager@example.edu";
	private static final String ADMIN_EMAIL = "admin@example.edu";

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
		stubActor("CB900", MANAGER_EMAIL, RolePolicy.MANAGER);
		VaiTro role = new VaiTro("SV", "Sinh viên");
		NguoiDung user = user("SV900", "sv900@example.edu", "password", role, NguoiDungTrangThai.CHO_DUYET);
		when(userRepository.findById("SV900")).thenReturn(Optional.of(user));

		AdminUserUpdateRequest request = new AdminUserUpdateRequest("Sinh viên", "sv900@example.edu", null, "SV",
				NguoiDungTrangThai.HOAT_DONG, 1L);

		assertThatThrownBy(() -> identityService.updateUser(MANAGER_EMAIL, "SV900", request)).isInstanceOfSatisfying(
				ApiException.class,
				exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
		verify(userRepository, never()).flush();
	}

	@Test
	void approvesPendingUserAndActivatesAccount() {
		stubActor("CB900", MANAGER_EMAIL, RolePolicy.MANAGER);
		VaiTro role = new VaiTro("SV", "Sinh viên");
		NguoiDung user = user("SV900", "sv900@example.edu", "password", role, NguoiDungTrangThai.CHO_DUYET);
		when(userRepository.findById("SV900")).thenReturn(Optional.of(user));

		UserProfileResponse response = identityService.approveUser(MANAGER_EMAIL, "SV900", 0L);

		assertThat(response.status()).isEqualTo(NguoiDungTrangThai.HOAT_DONG);
		assertThat(user.getStatus()).isEqualTo(NguoiDungTrangThai.HOAT_DONG);
		verify(userRepository).flush();
	}

	@Test
	void managerCanLockAndUnlockAccountWithCurrentVersion() {
		stubActor("CB900", MANAGER_EMAIL, RolePolicy.MANAGER);
		VaiTro role = new VaiTro("SV", "Sinh viên");
		NguoiDung user = user("SV900", "sv900@example.edu", "password", role, NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findById("SV900")).thenReturn(Optional.of(user));

		UserProfileResponse locked = identityService.changeUserStatus(MANAGER_EMAIL, "SV900",
				NguoiDungTrangThai.BI_KHOA, 0L);
		assertThat(locked.status()).isEqualTo(NguoiDungTrangThai.BI_KHOA);
		assertThat(user.getStatus()).isEqualTo(NguoiDungTrangThai.BI_KHOA);

		UserProfileResponse unlocked = identityService.changeUserStatus(MANAGER_EMAIL, "SV900",
				NguoiDungTrangThai.HOAT_DONG, 0L);
		assertThat(unlocked.status()).isEqualTo(NguoiDungTrangThai.HOAT_DONG);
		verify(userRepository, org.mockito.Mockito.times(2)).flush();
	}

	@Test
	void statusChangeRejectsStaleVersion() {
		stubActor("CB900", MANAGER_EMAIL, RolePolicy.MANAGER);
		VaiTro role = new VaiTro("SV", "Sinh viên");
		NguoiDung user = user("SV900", "sv900@example.edu", "password", role, NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findById("SV900")).thenReturn(Optional.of(user));

		assertThatThrownBy(
				() -> identityService.changeUserStatus(MANAGER_EMAIL, "SV900", NguoiDungTrangThai.BI_KHOA, 1L))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
		verify(userRepository, never()).flush();
	}

	@Test
	void managerCannotSeeAdministratorAccount() {
		stubActor("CB900", MANAGER_EMAIL, RolePolicy.MANAGER);
		NguoiDung administrator = user("ADMIN", ADMIN_EMAIL, "password",
				new VaiTro(RolePolicy.ADMIN, "Quản trị hệ thống"), NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findById("ADMIN")).thenReturn(Optional.of(administrator));

		assertThatThrownBy(() -> identityService.getUser(MANAGER_EMAIL, "ADMIN"))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
					assertThat(exception.getStatus().value()).isEqualTo(404);
				});
	}

	@Test
	void managerCannotModifyAnotherManagerOrPromoteAUser() {
		stubActor("CB900", MANAGER_EMAIL, RolePolicy.MANAGER);
		NguoiDung otherManager = user("CB901", "other-manager@example.edu", "password",
				new VaiTro(RolePolicy.MANAGER, "Cán bộ quản lý"), NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findById("CB901")).thenReturn(Optional.of(otherManager));
		AdminUserUpdateRequest managerUpdate = new AdminUserUpdateRequest("Cán bộ", "other-manager@example.edu", null,
				RolePolicy.STUDENT, NguoiDungTrangThai.HOAT_DONG, 0L);

		assertThatThrownBy(() -> identityService.updateUser(MANAGER_EMAIL, "CB901", managerUpdate))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

		NguoiDung student = user("SV901", "student@example.edu", "password",
				new VaiTro(RolePolicy.STUDENT, "Sinh viên"), NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findById("SV901")).thenReturn(Optional.of(student));
		AdminUserUpdateRequest promotion = new AdminUserUpdateRequest("Sinh viên", "student@example.edu", null,
				RolePolicy.MANAGER, NguoiDungTrangThai.HOAT_DONG, 0L);
		assertThatThrownBy(() -> identityService.updateUser(MANAGER_EMAIL, "SV901", promotion)).isInstanceOfSatisfying(
				ApiException.class, exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
	}

	@Test
	void administratorCanManageManagerButCannotLockItself() {
		NguoiDung administrator = stubActor("ADMIN", ADMIN_EMAIL, RolePolicy.ADMIN);
		NguoiDung manager = user("CB901", "other-manager@example.edu", "password",
				new VaiTro(RolePolicy.MANAGER, "Cán bộ quản lý"), NguoiDungTrangThai.HOAT_DONG);
		VaiTro instructorRole = new VaiTro(RolePolicy.INSTRUCTOR, "Giảng viên");
		when(userRepository.findById("CB901")).thenReturn(Optional.of(manager));
		when(roleRepository.findById(RolePolicy.INSTRUCTOR)).thenReturn(Optional.of(instructorRole));

		UserProfileResponse updated = identityService.updateUser(ADMIN_EMAIL, "CB901",
				new AdminUserUpdateRequest("Giảng viên", "other-manager@example.edu", null, RolePolicy.INSTRUCTOR,
						NguoiDungTrangThai.HOAT_DONG, 0L));
		assertThat(updated.roleId()).isEqualTo(RolePolicy.INSTRUCTOR);

		when(userRepository.findById("ADMIN")).thenReturn(Optional.of(administrator));
		assertThatThrownBy(() -> identityService.changeUserStatus(ADMIN_EMAIL, "ADMIN", NguoiDungTrangThai.BI_KHOA, 0L))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
	}

	private NguoiDung stubActor(String id, String email, String roleId) {
		NguoiDung actor = user(id, email, "password", new VaiTro(roleId, roleId), NguoiDungTrangThai.HOAT_DONG);
		when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(actor));
		return actor;
	}

	private NguoiDung user(String id, String email, String password, VaiTro role, NguoiDungTrangThai status) {
		return new NguoiDung(id, "Người dùng", email, passwordEncoder.encode(password), null, role, status);
	}
}
