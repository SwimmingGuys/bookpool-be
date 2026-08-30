package kr.co.bookpool.app.notice.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.notice.dto.request.NoticeRequest;
import kr.co.bookpool.app.notice.dto.response.NoticeResponse;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@Tag(name = "Admin Notice", description = "백오피스 공지사항 관리 API (ROLE_ADMIN 전용)")
public interface AdminNoticeControllerDocs {

	@Operation(
		summary = "공지 목록 조회 (관리자)",
		description = "고정 공지를 먼저, 그다음 최신순으로 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<NoticeResponse>> list(
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "공지 등록 (관리자)",
		description = "로그인한 관리자를 작성자로 공지를 등록합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "등록 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
		@ApiResponse(responseCode = "403", description = "관리자 권한 없음")
	})
	ApiResult<NoticeResponse> create(Long adminId, NoticeRequest request);

	@Operation(
		summary = "공지 수정 (관리자)",
		description = "작성자와 생성시각은 보존하고 본문만 수정합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "404", description = "공지를 찾을 수 없음")
	})
	ApiResult<NoticeResponse> update(Long id, NoticeRequest request);

	@Operation(
		summary = "공지 삭제 (관리자)",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "404", description = "공지를 찾을 수 없음")
	})
	ApiResult<Void> delete(Long id);
}
