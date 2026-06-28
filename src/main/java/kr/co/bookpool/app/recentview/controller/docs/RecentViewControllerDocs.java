package kr.co.bookpool.app.recentview.controller.docs;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.recentview.dto.request.RecentViewRequest;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Recent View", description = "최근 본 공고 API")
public interface RecentViewControllerDocs {

	@Operation(
		summary = "최근 본 캠페인 ID 목록",
		description = "최근 열람한 캠페인 ID를 최신순으로 반환합니다(최대 50개).",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<List<Long>> ids(Long memberId, @Parameter(description = "최대 개수(기본 50, 상한 50)") int limit);

	@Operation(
		summary = "최근 본 캠페인 목록",
		description = "최근 열람한 캠페인을 최신순으로 페이징 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<CampaignResponse>> list(
		Long memberId,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "캠페인 열람 기록",
		description = "캠페인 상세 열람을 기록합니다. 이미 기록돼 있으면 열람 시각만 갱신합니다(멱등).",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "기록 성공")
	ApiResult<Void> mark(Long memberId, RecentViewRequest request);
}
