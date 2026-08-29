package kr.co.bookpool.app.campaign.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.campaign.dto.request.CampaignCreateRequest;
import kr.co.bookpool.app.campaign.dto.request.CampaignUpdateRequest;
import kr.co.bookpool.app.campaign.dto.request.PublishStatusUpdateRequest;
import kr.co.bookpool.app.campaign.dto.request.StatusUpdateRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Admin Campaign", description = "백오피스 캠페인 관리 API (ROLE_ADMIN 전용)")
public interface AdminCampaignControllerDocs {

	@Operation(
		summary = "캠페인 목록 조회 (관리자)",
		description = "최신 등록순으로 페이징 조회합니다. publishStatus로 검수 큐(DRAFT)와 게시된 공고를 나눠 볼 수 있습니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<CampaignResponse>> list(
		@Parameter(description = "게시 상태 필터 (DRAFT/PUBLISHED). 생략 시 전부") PublishStatus publishStatus,
		@Parameter(description = "제목/도서명/출판사 검색어") String query,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "캠페인 상세 조회 (관리자)",
		description = "조회수 증가 없이 단건을 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(
			responseCode = "404", description = "캠페인을 찾을 수 없음",
			content = @Content(examples = @ExampleObject(value = """
				{ "success": false, "code": "CP001", "message": "캠페인을 찾을 수 없습니다." }""")))
	})
	ApiResult<CampaignResponse> detail(Long id);

	@Operation(
		summary = "캠페인 등록 (관리자)",
		description = "새 캠페인을 등록합니다. status 미지정 시 OPEN으로 생성됩니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "등록 성공"),
		@ApiResponse(
			responseCode = "400", description = "입력값 검증 실패",
			content = @Content(examples = @ExampleObject(value = """
				{ "success": false, "code": "C001", "message": "잘못된 입력값입니다." }"""))),
		@ApiResponse(
			responseCode = "403", description = "관리자 권한 없음",
			content = @Content(examples = @ExampleObject(value = """
				{ "success": false, "code": "A004", "message": "접근 권한이 없습니다." }""")))
	})
	ApiResult<CampaignResponse> create(CampaignCreateRequest request);

	@Operation(
		summary = "캠페인 수정 (관리자)",
		description = "기존 캠페인을 전체 수정합니다. (조회수/생성시각은 보존)",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
		@ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음")
	})
	ApiResult<CampaignResponse> update(Long id, CampaignUpdateRequest request);

	@Operation(
		summary = "게시 상태 변경 (관리자)",
		description = "검수 대기(DRAFT) ↔ 게시(PUBLISHED)를 전환합니다. 수집된 공고를 검수 후 공개할 때 씁니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "변경 성공"),
		@ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음")
	})
	ApiResult<CampaignResponse> changePublishStatus(Long id, PublishStatusUpdateRequest request);

	@Operation(
		summary = "모집 상태 변경 (관리자)",
		description = "모집중(OPEN) ↔ 마감(CLOSED)을 수동으로 전환합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "변경 성공"),
		@ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음")
	})
	ApiResult<CampaignResponse> changeStatus(Long id, StatusUpdateRequest request);

	@Operation(
		summary = "캠페인 삭제 (관리자)",
		description = "캠페인을 삭제합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "404", description = "캠페인을 찾을 수 없음")
	})
	ApiResult<Void> delete(Long id);
}
