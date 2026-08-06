package com.example.labmanagement.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

	@Mock
	private NguoiDungRepository userRepository;

	@Test
	void onlyActiveAccountIsEnabledAndRoleIsMapped() {
		VaiTro role = new VaiTro("GV", "Giảng viên");
		NguoiDung user = new NguoiDung("GV900", "Giảng viên", "person@example.edu", "hash", null, role,
				NguoiDungTrangThai.CHO_DUYET);
		when(userRepository.findByEmailIgnoreCase("person@example.edu")).thenReturn(Optional.of(user));

		UserDetails details = new DatabaseUserDetailsService(userRepository).loadUserByUsername(" PERSON@EXAMPLE.EDU ");

		assertThat(details.isEnabled()).isFalse();
		assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_GV");
	}
}
