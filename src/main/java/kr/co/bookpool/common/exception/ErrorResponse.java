package kr.co.bookpool.common.exception;

import java.util.List;

import org.springframework.validation.BindingResult;

public record ErrorResponse(
	int status,
	String code,
	String message,
	List<FieldError> errors
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(
			errorCode.getStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			List.of()
		);
	}

	public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
		return new ErrorResponse(
			errorCode.getStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			FieldError.from(bindingResult)
		);
	}

	public record FieldError(String field, String message) {

		public static List<FieldError> from(BindingResult bindingResult) {
			return bindingResult.getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		}
	}
}