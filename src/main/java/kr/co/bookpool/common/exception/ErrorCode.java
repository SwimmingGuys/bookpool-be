package kr.co.bookpool.common.exception;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// Common
	INVALID_INPUT_VALUE(BAD_REQUEST, "C001", "잘못된 입력값입니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 오류가 발생했습니다."),

	// Member
	DUPLICATE_EMAIL(CONFLICT, "M001", "이미 사용 중인 이메일입니다."),
	MEMBER_NOT_FOUND(NOT_FOUND, "M002", "회원을 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
