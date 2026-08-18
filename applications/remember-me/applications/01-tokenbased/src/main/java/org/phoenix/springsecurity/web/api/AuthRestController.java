package org.phoenix.springsecurity.web.api;

import org.phoenix.springsecurity.service.UserContext;
import org.phoenix.springsecurity.web.api.dto.UserDto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

	private final UserContext userContext;

	public AuthRestController(UserContext userContext) {
		this.userContext = userContext;
	}

	@GetMapping("/me")
	public UserDto me() {
		return UserDto.from(userContext.getCurrentUser());
	}
}
