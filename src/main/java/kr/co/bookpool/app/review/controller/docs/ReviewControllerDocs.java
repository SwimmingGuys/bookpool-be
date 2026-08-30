package kr.co.bookpool.app.review.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.review.dto.request.ReviewRequest;
import kr.co.bookpool.app.review.dto.response.ReviewResponse;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@Tag(name = "Review", description = "서평 API")
public interface ReviewControllerDocs {

	@Operation(
		summary = "공고별 서평 목록",
		description = "관리자가 인증(APPROVED)하고 숨기지 않은(VISIBLE) 서평만 반환합니다. 비로그인도 조회할 수 있습니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<ReviewResponse>> listByCampaign(
		Long memberId,
		@Parameter(description = "공고 ID", required = true) Long campaignId,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "내 서평 목록",
		description = "확인 대기·반려된 서평도 본인은 볼 수 있습니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<ReviewResponse>> listMine(Long memberId, int page, int size);

	@Operation(
		summary = "서평 제출",
		description = "원문 링크와 함께 서평을 제출합니다. 한 공고에 한 번만 제출할 수 있습니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "제출 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
		@ApiResponse(responseCode = "404", description = "공고를 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 이 공고에 서평을 등록함 (R002)")
	})
	ApiResult<ReviewResponse> submit(Long memberId, ReviewRequest request);

	@Operation(
		summary = "내 서평 수정",
		description = "내용을 고치면 다시 확인 대기 상태가 됩니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "403", description = "본인 서평이 아님 (R003)"),
		@ApiResponse(responseCode = "404", description = "서평을 찾을 수 없음 (R001)")
	})
	ApiResult<ReviewResponse> update(Long memberId, Long id, ReviewRequest request);

	@Operation(
		summary = "내 서평 삭제",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "403", description = "본인 서평이 아님 (R003)"),
		@ApiResponse(responseCode = "404", description = "서평을 찾을 수 없음 (R001)")
	})
	ApiResult<Void> delete(Long memberId, Long id);
}
