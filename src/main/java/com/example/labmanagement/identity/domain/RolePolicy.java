package com.example.labmanagement.identity.domain;

public final class RolePolicy {

	public static final String ADMIN = "ADMIN";
	public static final String MANAGER = "CBQL";
	public static final String INSTRUCTOR = "GV";
	public static final String STUDENT = "SV";

	private RolePolicy() {
	}

	public static boolean isAdministrator(String roleId) {
		return ADMIN.equals(roleId);
	}

	public static boolean isManager(String roleId) {
		return MANAGER.equals(roleId) || isAdministrator(roleId);
	}
}
