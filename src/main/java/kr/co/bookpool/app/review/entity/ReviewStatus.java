package kr.co.bookpool.app.review.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/** 공고 상세에 노출할지 여부. 관리자가 정한다. */
public enum ReviewStatus {
	VISIBLE,
	HIDDEN;

	@JsonCreator
	public static ReviewStatus from(String value) {
		if (value == null || value.isBlank()) return VISIBLE;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "VISIBLE", "노출" -> VISIBLE;
			case "HIDDEN", "숨김" -> HIDDEN;
			default -> throw new IllegalArgumentException("지원하지 않는 노출 상태입니다.");
		};
	}
}
