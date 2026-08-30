package kr.co.bookpool.app.campaign.dto.response;

/**
 * 카테고리별 모집중 건수.
 * category는 프론트가 그대로 키로 쓰는 한국어 라벨이다.
 */
public record CategoryCountResponse(
	String category,
	long count
) {
}
