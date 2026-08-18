package org.phoenix.springsecurity.web.api.dto;

import java.util.List;

import org.phoenix.springsecurity.domain.User;

public record UserDto(Integer id, String firstName, String lastName, String email, List<String> roles) {

	public static UserDto from(User user) {
		// frontend sözleşmesi: roles alanı "ROLE_*" string listesi bekliyor
		List<String> roleNames = user.getRole() == null ? List.of() : List.of(user.getRole().authority());
		return new UserDto(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), roleNames);
	}
}
