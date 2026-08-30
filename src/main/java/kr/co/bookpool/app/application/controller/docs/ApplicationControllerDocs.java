package kr.co.bookpool.app.application.controller.docs;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.application.dto.request.ApplicationRequest;
import kr.co.bookpool.app.application.dto.request.ApplicationStatusRequest;
import kr.co.bookpool.app.application.dto.response.ApplicationResponse;
import kr.co.bookpool.app.application.entity.ApplicationStatus;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@Tag(name = "Application", description = "신청 표시 API (자기 신고)")
public interface ApplicationControllerDocs {

	@Operation(
		summary = "신청한 공고 ID 목록",
		description = "공고 카드·상세에서 '신청함' 표시를 그리기 위한 ID만 반환합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<List<Long>> getAppliedCampaignIds(Long memberId);

	@Operation(
		summary = "신청한 공고 목록",
		description = "발표일·서평 제출 기한을 함께 보여줄 수 있도록 공고 정보를 포함해 반환합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<ApplicationResponse>> list(
		Long memberId,
		@Parameter(description = "상태 필터 (APPLIED/SELECTED/NOT_SELECTED). 생략 시 전부") ApplicationStatus status,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "신청 표시",
		description = "실제 신청은 외부 폼에서 이뤄지므로, 사용자가 직접 '신청함'을 남깁니다. "
			+ "status를 생략하면 APPLIED로 시작합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "표시 성공"),
		@ApiResponse(responseCode = "404", description = "공고를 찾을 수 없음 (CP001)"),
		@ApiResponse(responseCode = "409", description = "이미 신청 표시한 공고 (AP002)")
	})
	ApiResult<ApplicationResponse> apply(Long memberId, ApplicationRequest request);

	@Operation(
		summary = "신청 상태 변경",
		description = "발표 결과를 사용자가 직접 표시합니다(당첨/미당첨).",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "변경 성공"),
		@ApiResponse(responseCode = "404", description = "신청 기록을 찾을 수 없음 (AP001)")
	})
	ApiResult<ApplicationResponse> changeStatus(Long memberId, Long campaignId, ApplicationStatusRequest request);

	@Operation(
		summary = "신청 표시 해제",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "해제 성공"),
		@ApiResponse(responseCode = "404", description = "신청 기록을 찾을 수 없음 (AP001)")
	})
	ApiResult<Void> cancel(Long memberId, Long campaignId);
}
