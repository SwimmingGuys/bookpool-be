package kr.co.bookpool.app.campaign.dto.request;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DeadlineFilter {
	ALL,
	WEEK,
	IMMINENT;

	@JsonCreator
	public static DeadlineFilter from(String value) {
		if (value == null || value.isBlank()) return ALL;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "ALL" -> ALL;
			case "WEEK" -> WEEK;
			case "IMMINENT" -> IMMINENT;
			default -> throw new IllegalArgumentException("지원하지 않는 마감 필터입니다.");
		};
	}
}
