package kr.co.bookpool.common.response;

import java.util.List;

import org.springframework.validation.BindingResult;

public record FieldError(String field, String message) {

	public static List<FieldError> from(BindingResult bindingResult) {
		return bindingResult.getFieldErrors().stream()
			.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
	}
}