package kr.co.bookpool.app.campaign.dto.request;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * from~to 날짜 범위를 어떤 날짜에 적용할지.
 * 캘린더가 보고 있는 달의 공고만 받아갈 때 쓴다.
 */
public enum DateBasis {
	RECRUIT_START("recruitStartDate"),
	RECRUIT_END("deadlineAt"),
	ANNOUNCEMENT("announcementDate");

	private final String field;

	DateBasis(String field) {
		this.field = field;
	}

	public String getField() {
		return field;
	}

	@JsonCreator
	public static DateBasis from(String value) {
		if (value == null || value.isBlank()) return RECRUIT_END;
		return switch (value.trim().toUpperCase(Locale.ROOT).replace("-", "_")) {
			case "RECRUIT_START", "RECRUITSTART" -> RECRUIT_START;
			case "RECRUIT_END", "RECRUITEND", "DEADLINE" -> RECRUIT_END;
			case "ANNOUNCEMENT" -> ANNOUNCEMENT;
			default -> throw new IllegalArgumentException("지원하지 않는 날짜 기준입니다.");
		};
	}
}
