package kr.co.bookpool.common.exception;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("BusinessException: {} - {}", errorCode.getCode(), e.getMessage());
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResult.error(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResult<List<FieldError>>> handleValidationException(MethodArgumentNotValidException e) {
		ErrorCode errorCode = INVALID_INPUT_VALUE;
		List<FieldError> fieldErrors = FieldError.from(e.getBindingResult());
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResult.error(errorCode, fieldErrors));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResult<Void>> handleException(Exception e) {
		ErrorCode errorCode = INTERNAL_SERVER_ERROR;
		log.error("Unhandled exception", e);
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResult.error(errorCode));
	}
}
