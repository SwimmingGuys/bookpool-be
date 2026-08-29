package kr.co.bookpool.app.campaign.dto.request;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SortKey {
	/** 마감 임박순 */
	DEADLINE,
	/** 조회순 */
	VIEWS,
	/** 최신 등록순 */
	LATEST;

	@JsonCreator
	public static SortKey from(String value) {
		if (value == null || value.isBlank()) return DEADLINE;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "DEADLINE" -> DEADLINE;
			// POPULAR는 VIEWS로 이름이 바뀌기 전 표기
			case "VIEWS", "POPULAR" -> VIEWS;
			case "LATEST" -> LATEST;
			default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다.");
		};
	}
}
