package kr.co.bookpool.app.campaign.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CampaignType {
	REVIEWER,
	BETA_READER;

	@JsonCreator
	public static CampaignType from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("공고 유형을 입력해주세요.");
		}

		String normalizedValue = value.trim()
			.toUpperCase(Locale.ROOT)
			.replace("-", "_")
			.replace(" ", "_");

		return switch (normalizedValue) {
			case "REVIEWER", "서평단" -> REVIEWER;
			case "BETA_READER", "BETAREADER", "베타리더" -> BETA_READER;
			default -> throw new IllegalArgumentException("지원하지 않는 공고 유형입니다.");
		};
	}
}
