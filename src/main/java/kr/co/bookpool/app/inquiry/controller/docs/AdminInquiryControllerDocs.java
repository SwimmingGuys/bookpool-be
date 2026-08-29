package kr.co.bookpool.app.inquiry.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.inquiry.dto.request.AnswerInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.app.inquiry.entity.InquiryStatus;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@Tag(name = "Admin Inquiry", description = "백오피스 문의 관리 API (ROLE_ADMIN 전용)")
public interface AdminInquiryControllerDocs {

	@Operation(
		summary = "문의 목록 조회 (관리자)",
		description = "작성자 정보를 포함해 오래된 순으로 조회합니다. status로 미답변/답변완료를 나눠 볼 수 있습니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<InquiryResponse>> list(
		@Parameter(description = "상태 필터 (PENDING/ANSWERED). 생략 시 전부") InquiryStatus status,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "문의 답변 등록 (관리자)",
		description = "답변을 등록하거나 기존 답변을 수정합니다. 등록 시 상태가 ANSWERED로 바뀝니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "답변 성공"),
		@ApiResponse(responseCode = "400", description = "답변 내용 누락"),
		@ApiResponse(responseCode = "404", description = "문의를 찾을 수 없음")
	})
	ApiResult<InquiryResponse> answer(Long id, AnswerInquiryRequest request);
}
