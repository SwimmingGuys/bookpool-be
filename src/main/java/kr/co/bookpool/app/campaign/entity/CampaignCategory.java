package kr.co.bookpool.app.campaign.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CampaignCategory {
	IT,
	ECONOMY,
	NOVEL,
	ESSAY,
	HUMANITY,
	SCIENCE,
	ETC;

	@JsonCreator
	public static CampaignCategory from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("카테고리를 입력해주세요.");
		}

		String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
		return switch (normalizedValue) {
			case "IT", "IT/개발", "개발" -> IT;
			case "ECONOMY", "경제" -> ECONOMY;
			case "NOVEL", "소설" -> NOVEL;
			case "ESSAY", "에세이" -> ESSAY;
			case "HUMANITY", "인문", "인문/사회" -> HUMANITY;
			case "SCIENCE", "과학" -> SCIENCE;
			case "ETC", "기타", "기획/디자인", "자기계발", "예술/디자인", "학습/교육" -> ETC;
			default -> throw new IllegalArgumentException("지원하지 않는 카테고리입니다.");
		};
	}
}
