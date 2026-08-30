package kr.co.bookpool.app.campaign.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 게시 상태.
 * 수집기가 가져온 공고는 DRAFT(검수 대기)로 들어와 관리자 검수를 거쳐 PUBLISHED가 된다.
 * 공개 API는 PUBLISHED만 노출한다.
 */
public enum PublishStatus {

	DRAFT("검수 대기"),
	PUBLISHED("게시됨");

	private final String label;

	PublishStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@JsonCreator
	public static PublishStatus from(String value) {
		if (value == null || value.isBlank()) return PUBLISHED;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "DRAFT", "검수대기" -> DRAFT;
			case "PUBLISHED", "게시됨", "게시" -> PUBLISHED;
			default -> throw new IllegalArgumentException("지원하지 않는 게시 상태입니다.");
		};
	}
}
