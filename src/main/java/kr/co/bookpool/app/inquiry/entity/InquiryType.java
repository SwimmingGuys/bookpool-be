package kr.co.bookpool.app.inquiry.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum InquiryType {
	REQUEST,
	INQUIRY;

	@JsonCreator
	public static InquiryType from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("문의 유형을 입력해주세요.");
		}
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "REQUEST", "요청" -> REQUEST;
			case "INQUIRY", "문의" -> INQUIRY;
			default -> throw new IllegalArgumentException("지원하지 않는 문의 유형입니다.");
		};
	}
}
