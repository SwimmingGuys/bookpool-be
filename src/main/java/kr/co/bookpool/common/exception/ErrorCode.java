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
	MEMBER_NOT_FOUND(NOT_FOUND, "M002", "회원을 찾을 수 없습니다."),
	EMAIL_NOT_VERIFIED(BAD_REQUEST, "M003", "이메일 인증이 완료되지 않았습니다."),
	INVALID_VERIFICATION_CODE(BAD_REQUEST, "M004", "인증 코드가 올바르지 않거나 만료되었습니다."),
	INVALID_CURRENT_PASSWORD(BAD_REQUEST, "M005", "현재 비밀번호가 일치하지 않습니다."),
	PASSWORD_RESET_NOT_VERIFIED(BAD_REQUEST, "M006", "비밀번호 재설정 이메일 인증이 완료되지 않았습니다."),
	PASSWORD_MISMATCH(BAD_REQUEST, "M005", "현재 비밀번호가 일치하지 않습니다."),

	// Auth
	LOGIN_FAILED(UNAUTHORIZED, "A001", "이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_TOKEN(UNAUTHORIZED, "A002", "유효하지 않은 인증 정보입니다."),
	INVALID_REFRESH_TOKEN(UNAUTHORIZED, "A003", "유효하지 않은 리프레시 토큰입니다. 다시 로그인해 주세요."),

	// Campaign
	CAMPAIGN_NOT_FOUND(NOT_FOUND, "CP001", "캠페인을 찾을 수 없습니다."),

	// Notice
	NOTICE_NOT_FOUND(NOT_FOUND, "N001", "공지사항을 찾을 수 없습니다."),

	// Inquiry
	INQUIRY_NOT_FOUND(NOT_FOUND, "I001", "문의를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
