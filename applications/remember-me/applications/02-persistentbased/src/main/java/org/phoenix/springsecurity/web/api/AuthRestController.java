package org.phoenix.springsecurity.web.api;

import org.phoenix.springsecurity.security.CurrentUser;
import org.phoenix.springsecurity.web.api.dto.UserDto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

	private final CurrentUser currentUser;

	public AuthRestController(CurrentUser currentUser) {
		this.currentUser = currentUser;
	}

	@GetMapping("/me")
	public UserDto me() {
		return UserDto.from(currentUser.get());
	}
}
