package kr.co.bookpool.app.inquiry.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.inquiry.dto.request.CreateInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@Tag(name = "Inquiry", description = "문의 API")
public interface InquiryControllerDocs {

	@Operation(
		summary = "내 문의 목록 조회",
		description = "로그인한 회원이 등록한 문의를 최신순으로 페이징 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<InquiryResponse>> listMine(
		Long memberId,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "내 문의 단건 조회",
		description = "본인이 등록한 문의 1건을 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(
			responseCode = "404", description = "문의를 찾을 수 없음(타인의 문의 포함)",
			content = @Content(examples = @ExampleObject(value = """
				{ "success": false, "code": "I001", "message": "문의를 찾을 수 없습니다." }""")))
	})
	ApiResult<InquiryResponse> detail(Long memberId, Long id);

	@Operation(
		summary = "문의 등록",
		description = "문의/요청을 등록합니다. (type: inquiry|request)",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "등록 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패")
	})
	ApiResult<InquiryResponse> create(Long memberId, CreateInquiryRequest request);
}
