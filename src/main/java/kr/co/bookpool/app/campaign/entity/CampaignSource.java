package kr.co.bookpool.app.campaign.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 공고를 어디서 가져왔는지.
 * 지금은 전부 MANUAL이지만, 수집기가 붙으면 그대로 확장된다.
 */
public enum CampaignSource {

	MANUAL("직접 등록"),
	INSTAGRAM("인스타그램"),
	BLOG("블로그"),
	PUBLISHER("출판사"),
	ETC("기타");

	private final String label;

	CampaignSource(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@JsonCreator
	public static CampaignSource from(String value) {
		if (value == null || value.isBlank()) return MANUAL;
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		for (CampaignSource source : values()) {
			if (source.name().equals(normalized)) return source;
		}
		throw new IllegalArgumentException("지원하지 않는 수집 출처입니다.");
	}
}
