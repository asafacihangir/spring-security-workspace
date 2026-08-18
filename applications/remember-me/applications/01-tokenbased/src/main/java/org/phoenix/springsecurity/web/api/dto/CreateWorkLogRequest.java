package org.phoenix.springsecurity.web.api.dto;

import jakarta.validation.constraints.NotEmpty;

public record CreateWorkLogRequest(
		@NotEmpty(message = "Explanation is required")
		String explanation) {
}
