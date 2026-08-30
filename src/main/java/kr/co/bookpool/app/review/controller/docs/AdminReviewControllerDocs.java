package kr.co.bookpool.app.review.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.review.dto.request.ReviewDecisionRequest;
import kr.co.bookpool.app.review.dto.request.ReviewStatusRequest;
import kr.co.bookpool.app.review.dto.response.ReviewResponse;
import kr.co.bookpool.app.review.entity.ReviewSubmissionStatus;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@Tag(name = "Admin Review", description = "백오피스 서평 인증 API (ROLE_ADMIN 전용)")
public interface AdminReviewControllerDocs {

	@Operation(
		summary = "서평 목록 조회 (관리자)",
		description = "확인해야 할 것이 위로 오도록 오래된 순으로 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<ReviewResponse>> list(
		@Parameter(description = "공고 ID 필터") Long campaignId,
		@Parameter(description = "제출 상태 필터 (SUBMITTED/APPROVED/REJECTED)") ReviewSubmissionStatus submissionStatus,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "서평 인증/반려 (관리자)",
		description = "APPROVED로 인증하거나 REJECTED로 반려합니다. 반려는 rejectReason이 필요합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "처리 성공"),
		@ApiResponse(responseCode = "400", description = "반려 사유 누락 등 검증 실패"),
		@ApiResponse(responseCode = "404", description = "서평을 찾을 수 없음 (R001)")
	})
	ApiResult<ReviewResponse> decide(Long id, ReviewDecisionRequest request);

	@Operation(
		summary = "서평 노출 상태 변경 (관리자)",
		description = "공고 상세에 노출(VISIBLE)할지 숨김(HIDDEN)일지 정합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "변경 성공"),
		@ApiResponse(responseCode = "404", description = "서평을 찾을 수 없음 (R001)")
	})
	ApiResult<ReviewResponse> changeStatus(Long id, ReviewStatusRequest request);

	@Operation(
		summary = "서평 삭제 (관리자)",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "404", description = "서평을 찾을 수 없음 (R001)")
	})
	ApiResult<Void> delete(Long id);
}
