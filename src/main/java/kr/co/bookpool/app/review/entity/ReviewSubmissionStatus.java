package kr.co.bookpool.app.review.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/** 서평 제출 → 관리자 인증까지의 상태. */
public enum ReviewSubmissionStatus {
	SUBMITTED,
	APPROVED,
	REJECTED;

	@JsonCreator
	public static ReviewSubmissionStatus from(String value) {
		if (value == null || value.isBlank()) return SUBMITTED;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "SUBMITTED", "확인대기" -> SUBMITTED;
			case "APPROVED", "인증완료" -> APPROVED;
			case "REJECTED", "반려" -> REJECTED;
			default -> throw new IllegalArgumentException("지원하지 않는 서평 상태입니다.");
		};
	}
}
