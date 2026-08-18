package org.phoenix.springsecurity.web.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record SignupRequest(
		@NotEmpty(message = "First Name is required")
		String firstName,
		@NotEmpty(message = "Last Name is required")
		String lastName,
		@NotEmpty(message = "Email is required")
		@Email(message = "Please provide a valid email address")
		String email,
		@NotEmpty(message = "Password is required")
		String password) {
}
