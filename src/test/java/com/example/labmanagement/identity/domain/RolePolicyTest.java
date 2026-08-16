package com.example.labmanagement.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RolePolicyTest {

	@Test
	void administratorAndManagerHaveManagementAuthorityButBusinessRolesDoNot() {
		assertThat(RolePolicy.isManager(RolePolicy.ADMIN)).isTrue();
		assertThat(RolePolicy.isManager(RolePolicy.MANAGER)).isTrue();
		assertThat(RolePolicy.isManager(RolePolicy.INSTRUCTOR)).isFalse();
		assertThat(RolePolicy.isManager(RolePolicy.STUDENT)).isFalse();
	}
}
