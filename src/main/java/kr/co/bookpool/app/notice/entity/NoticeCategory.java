package kr.co.bookpool.app.notice.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NoticeCategory {
	UPDATE,
	POLICY,
	MAINTENANCE,
	EVENT,
	GENERAL;

	@JsonCreator
	public static NoticeCategory from(String value) {
		if (value == null || value.isBlank()) return GENERAL;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "UPDATE", "업데이트" -> UPDATE;
			case "POLICY", "정책" -> POLICY;
			case "MAINTENANCE", "점검" -> MAINTENANCE;
			case "EVENT", "이벤트" -> EVENT;
			case "GENERAL", "공지" -> GENERAL;
			default -> throw new IllegalArgumentException("지원하지 않는 공지 카테고리입니다.");
		};
	}
}
