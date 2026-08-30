package kr.co.bookpool.app.campaign.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/** 참여자에게 제공되는 도서 형태. */
public enum BookFormat {

	PAPER("종이책"),
	EBOOK("eBook"),
	BOTH("종이책 + eBook");

	private final String label;

	BookFormat(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@JsonCreator
	public static BookFormat from(String value) {
		if (value == null || value.isBlank()) return null;
		return switch (value.trim().toUpperCase(Locale.ROOT)) {
			case "PAPER", "종이책" -> PAPER;
			case "EBOOK", "E-BOOK", "전자책" -> EBOOK;
			case "BOTH", "둘다" -> BOTH;
			default -> throw new IllegalArgumentException("지원하지 않는 도서 형태입니다.");
		};
	}
}
