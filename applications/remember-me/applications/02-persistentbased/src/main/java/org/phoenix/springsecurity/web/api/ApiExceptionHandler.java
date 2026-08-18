package org.phoenix.springsecurity.web.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.phoenix.springsecurity.service.DuplicateEmailException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.phoenix.springsecurity.web.api")
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return errors;
	}

	@ExceptionHandler(DuplicateEmailException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleDuplicateEmail(DuplicateEmailException ex) {
		return Map.of("email", ex.getMessage());
	}

	@ExceptionHandler(NoSuchElementException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public void handleNotFound(NoSuchElementException ex) {
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Void> handleResponseStatus(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode()).build();
	}
}
