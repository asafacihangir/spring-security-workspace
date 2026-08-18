package org.phoenix.springsecurity.domain;

public enum Role {

	USER, ADMIN;

	public String authority() {
		return "ROLE_" + name();
	}

}
