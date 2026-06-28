package kr.co.bookpool.app.campaign.dto.request;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SortKey {
	DEADLINE,
	POPULAR;

	@JsonCreator
	public static SortKey from(String value) {
		if (value == null || value.isBlank()) return DEADLINE;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "DEADLINE" -> DEADLINE;
			case "POPULAR" -> POPULAR;
			default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다.");
		};
	}
}
