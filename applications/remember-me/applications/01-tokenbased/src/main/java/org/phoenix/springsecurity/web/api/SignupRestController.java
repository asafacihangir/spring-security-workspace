package org.phoenix.springsecurity.web.api;

import org.phoenix.springsecurity.domain.User;
import org.phoenix.springsecurity.service.UserService;
import org.phoenix.springsecurity.web.api.dto.SignupRequest;
import org.phoenix.springsecurity.web.api.dto.UserDto;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignupRestController {

	private final UserService userService;

	public SignupRestController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/api/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto signup(@Valid @RequestBody SignupRequest request) {
		User user = new User();
		user.setEmail(request.email());
		user.setFirstName(request.firstName());
		user.setLastName(request.lastName());
		user.setPassword(request.password());
		int id = userService.createUser(user);
		user.setId(id);
		return UserDto.from(user);
	}
}
