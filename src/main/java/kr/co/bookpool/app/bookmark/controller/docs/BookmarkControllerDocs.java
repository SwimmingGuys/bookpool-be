package kr.co.bookpool.app.bookmark.controller.docs;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.bookmark.dto.request.BookmarkRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Bookmark", description = "즐겨찾기 API")
public interface BookmarkControllerDocs {

	@Operation(
		summary = "즐겨찾기 캠페인 ID 목록 조회",
		description = "로그인한 회원이 즐겨찾기한 캠페인 ID 목록을 조회합니다. 목록 화면에서 즐겨찾기 여부 비교용입니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(
		responseCode = "200", description = "조회 성공",
		content = @Content(examples = @ExampleObject(value = """
			{
			  "success": true,
			  "code": "SUCCESS",
			  "message": "요청에 성공했습니다.",
			  "data": [3, 7, 12]
			}""")))
	ApiResult<List<Long>> getBookmarkedCampaignIds(Long memberId);

	@Operation(
		summary = "즐겨찾기 캠페인 목록 조회",
		description = "로그인한 회원이 즐겨찾기한 캠페인을 최신순으로 페이징 조회합니다. (마이페이지용)",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<CampaignResponse>> getBookmarkedCampaigns(
		Long memberId,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "즐겨찾기 추가",
		description = "캠페인을 즐겨찾기에 추가합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "추가 성공"),
		@ApiResponse(
			responseCode = "409", description = "이미 즐겨찾기한 캠페인",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "B001",
				  "message": "이미 즐겨찾기한 공고입니다."
				}"""))),
		@ApiResponse(
			responseCode = "404", description = "캠페인을 찾을 수 없음",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "CP001",
				  "message": "캠페인을 찾을 수 없습니다."
				}""")))
	})
	ApiResult<Void> add(Long memberId, BookmarkRequest request);

	@Operation(
		summary = "즐겨찾기 삭제",
		description = "캠페인을 즐겨찾기에서 삭제합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(
			responseCode = "404", description = "즐겨찾기를 찾을 수 없음",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "B002",
				  "message": "즐겨찾기를 찾을 수 없습니다."
				}""")))
	})
	ApiResult<Void> remove(Long memberId, Long campaignId);
}
