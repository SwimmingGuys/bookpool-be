package kr.co.bookpool.app.notice.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.app.notice.dto.response.NoticeResponse;
import kr.co.bookpool.app.notice.entity.NoticeCategory;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Notice", description = "공지사항 API")
public interface NoticeControllerDocs {

	@Operation(summary = "공지사항 목록 조회", description = "카테고리로 필터링하며, 고정 공지를 먼저, 그다음 최신순으로 페이징 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "조회 성공",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": true,
				  "code": "SUCCESS",
				  "message": "요청에 성공했습니다.",
				  "data": {
				    "content": [
				      {
				        "id": "1",
				        "title": "개인정보 처리방침 개정 안내",
				        "content": "개인정보 처리방침이 일부 개정되었습니다.",
				        "category": "policy",
				        "author": "운영팀",
				        "isPinned": true,
				        "createdAt": "2026-06-26T10:00:00"
				      }
				    ],
				    "page": 0,
				    "size": 20,
				    "totalElements": 1,
				    "totalPages": 1,
				    "hasNext": false
				  }
				}""")))
	})
	ApiResult<PageResponse<NoticeResponse>> list(
		@Parameter(description = "공지 카테고리 (update/policy/maintenance/event/general)") NoticeCategory category,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(summary = "공지사항 상세 조회", description = "공지사항 ID로 단건을 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "조회 성공",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": true,
				  "code": "SUCCESS",
				  "message": "요청에 성공했습니다.",
				  "data": {
				    "id": "1",
				    "title": "개인정보 처리방침 개정 안내",
				    "content": "개인정보 처리방침이 일부 개정되었습니다.",
				    "category": "policy",
				    "author": "운영팀",
				    "isPinned": true,
				    "createdAt": "2026-06-26T10:00:00"
				  }
				}"""))),
		@ApiResponse(
			responseCode = "404", description = "공지사항을 찾을 수 없음",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "N001",
				  "message": "공지사항을 찾을 수 없습니다."
				}""")))
	})
	ApiResult<NoticeResponse> detail(Long id);
}
