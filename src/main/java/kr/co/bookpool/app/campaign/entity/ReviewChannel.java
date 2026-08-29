package kr.co.bookpool.app.campaign.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 서평을 올려야 하는 채널.
 * 공고에 명시하지 않으면 참여자와 분쟁이 잦은 항목이라 별도 값으로 관리한다.
 */
public enum ReviewChannel {

	BLOG("블로그"),
	INSTAGRAM("인스타그램"),
	YES24("예스24"),
	ALADIN("알라딘"),
	KYOBO("교보문고"),
	BRUNCH("브런치"),
	THREADS("스레드"),
	ETC("기타");

	private final String label;

	ReviewChannel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@JsonCreator
	public static ReviewChannel from(String value) {
		if (value == null || value.isBlank()) return null;
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		for (ReviewChannel channel : values()) {
			if (channel.name().equals(normalized) || channel.label.equals(value.trim())) {
				return channel;
			}
		}
		throw new IllegalArgumentException("지원하지 않는 서평 채널입니다.");
	}
}
