package kr.co.bookpool.app.campaign.entity;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 공고 카테고리.
 *
 * <p>프론트의 {@code CATEGORIES}(9종)와 1:1로 대응한다. 라벨을 enum이 직접 들고 있어
 * 입력 파싱({@link #from})과 출력 라벨({@link #getLabel})이 어긋날 수 없다.
 *
 * <p>이전에는 4종(기획/디자인·자기계발·예술/디자인·학습/교육)이 모두 {@code ETC}로 저장돼
 * 다시 읽으면 '기타'가 되는 손실이 있었다.
 */
public enum CampaignCategory {

	IT("IT/개발"),
	NOVEL("소설"),
	ECONOMY("경제"),
	ESSAY("에세이"),
	PLANNING_DESIGN("기획/디자인"),
	SELF_DEVELOPMENT("자기계발"),
	HUMANITY("인문/사회"),
	ART_DESIGN("예술/디자인"),
	EDUCATION("학습/교육"),
	ETC("기타"),

	/**
	 * 이 enum이 세분화되기 전에 저장된 값. 새로 쓰지 않는다.
	 * 남겨두지 않으면 기존 행을 읽을 때 역직렬화가 깨진다.
	 */
	@Deprecated
	SCIENCE("예술/디자인");

	private static final Map<String, CampaignCategory> BY_LABEL = Stream.of(values())
		.collect(Collectors.toMap(
			category -> normalize(category.label),
			category -> category,
			// 라벨이 겹치면(SCIENCE/ART_DESIGN) 새 값을 남긴다.
			(current, duplicate) -> current
		));

	private final String label;

	CampaignCategory(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * enum 이름('IT')과 한국어 라벨('IT/개발') 양쪽을 받는다.
	 * 프론트는 라벨을 그대로 보내고, 관리 도구나 테스트는 enum 이름을 쓴다.
	 */
	@JsonCreator
	public static CampaignCategory from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("카테고리를 입력해주세요.");
		}

		String normalized = normalize(value);

		CampaignCategory byLabel = BY_LABEL.get(normalized);
		if (byLabel != null) {
			return byLabel == SCIENCE ? ART_DESIGN : byLabel;
		}

		for (CampaignCategory category : values()) {
			if (normalize(category.name()).equals(normalized)) {
				return category;
			}
		}

		// 라벨이 바뀌기 전에 쓰이던 표기
		return switch (normalized) {
			case "개발" -> IT;
			case "인문" -> HUMANITY;
			case "과학" -> ART_DESIGN;
			case "디자인" -> ART_DESIGN;
			case "교육" -> EDUCATION;
			default -> throw new IllegalArgumentException("지원하지 않는 카테고리입니다.");
		};
	}

	// 공백과 대소문자 차이를 무시하고 비교한다. ('IT / 개발' == 'it/개발')
	private static String normalize(String value) {
		return value.trim().toUpperCase(Locale.ROOT).replace(" ", "");
	}
}
