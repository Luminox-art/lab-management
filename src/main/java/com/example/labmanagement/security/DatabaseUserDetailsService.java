package com.example.labmanagement.security;

import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.persistence.NguoiDungRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final NguoiDungRepository userRepository;

	public DatabaseUserDetailsService(NguoiDungRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		NguoiDung user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
				.orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ."));
		return User.withUsername(user.getEmail()).password(user.getPasswordHash()).roles(user.getRole().getId())
				.disabled(user.getStatus() != NguoiDungTrangThai.HOAT_DONG).build();
	}
}
