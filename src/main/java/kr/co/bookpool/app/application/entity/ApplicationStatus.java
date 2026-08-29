package kr.co.bookpool.app.application.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 신청 상태.
 *
 * <p>실제 신청은 출판사 폼 등 외부 페이지에서 일어나므로 서비스가 결과를 알 수 없다.
 * 사용자가 직접 표시하는 자기 신고 값이다.
 */
public enum ApplicationStatus {

	/** 신청함 (결과 대기) */
	APPLIED("신청함"),
	/** 당첨 */
	SELECTED("당첨"),
	/** 미당첨 */
	NOT_SELECTED("미당첨");

	private final String label;

	ApplicationStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@JsonCreator
	public static ApplicationStatus from(String value) {
		if (value == null || value.isBlank()) return APPLIED;
		return switch (value.trim().toUpperCase(Locale.ROOT).replace("-", "_")) {
			case "APPLIED", "신청함" -> APPLIED;
			case "SELECTED", "당첨" -> SELECTED;
			case "NOT_SELECTED", "미당첨" -> NOT_SELECTED;
			default -> throw new IllegalArgumentException("지원하지 않는 신청 상태입니다.");
		};
	}
}
