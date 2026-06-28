package kr.co.bookpool.common.exception;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.bookpool.common.discord.DiscordAlertNotifier;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

	private final DiscordAlertNotifier discordAlertNotifier;

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
		ErrorCode errorCode = e.getErrorCode();
		log.warn("BusinessException: {} - {}", errorCode.getCode(), e.getMessage());
		alert(errorCode, errorCode.getMessage(), request, null);
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResult.error(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResult<List<FieldError>>> handleValidationException(
		MethodArgumentNotValidException e, HttpServletRequest request) {
		ErrorCode errorCode = INVALID_INPUT_VALUE;
		List<FieldError> fieldErrors = FieldError.from(e.getBindingResult());
		alert(errorCode, errorCode.getMessage(), request, null);
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResult.error(errorCode, fieldErrors));
	}

	// 잘못된 JSON 본문/역직렬화 실패(예: 지원하지 않는 enum 값)는 입력값 오류(400)로 처리한다.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResult<Void>> handleNotReadable(HttpMessageNotReadableException e) {
		log.warn("Malformed request body: {}", e.getMessage());
		return ResponseEntity
			.status(INVALID_INPUT_VALUE.getStatus())
			.body(ApiResult.error(INVALID_INPUT_VALUE));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResult<Void>> handleDataIntegrityViolationException(
		DataIntegrityViolationException e, HttpServletRequest request) {
		return DbConstraint.resolve(e.getMostSpecificCause().getMessage())
			.map(errorCode -> { // 매칭 : 해당 상태코드(409 등)
				log.warn("DataIntegrityViolationException mapped to {}: {}", errorCode.getCode(), e.getMessage());
				alert(errorCode, errorCode.getMessage(), request, null);
				return ResponseEntity
					.status(errorCode.getStatus())
					.body(ApiResult.error(errorCode));
			})
			.orElseGet(() -> { // 알 수 없는 위반 : error 로깅 + 500
				log.error("Unhandled data integrity violation", e);
				alert(INTERNAL_SERVER_ERROR, e.getMessage(), request, e);
				return ResponseEntity
					.status(INTERNAL_SERVER_ERROR.getStatus())
					.body(ApiResult.error(INTERNAL_SERVER_ERROR));
			});
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResult<Void>> handleException(Exception e, HttpServletRequest request) {
		ErrorCode errorCode = INTERNAL_SERVER_ERROR;
		log.error("Unhandled exception", e);
		alert(errorCode, e.getMessage(), request, e);
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResult.error(errorCode));
	}

	/**
	 * 에러 응답을 Discord로 알린다. (설정/임계값에 따라 무시될 수 있으며, 알림 실패는 응답에 영향을 주지 않는다)
	 */
	private void alert(ErrorCode errorCode, String message, HttpServletRequest request, Throwable throwable) {
		discordAlertNotifier.notifyError(
			errorCode.getStatus().value(),
			errorCode.getCode(),
			message,
			request.getMethod(),
			request.getRequestURI(),
			throwable
		);
	}
}
