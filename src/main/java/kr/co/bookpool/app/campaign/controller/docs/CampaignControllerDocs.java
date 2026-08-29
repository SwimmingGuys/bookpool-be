package kr.co.bookpool.app.campaign.controller.docs;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.campaign.dto.request.DateBasis;
import kr.co.bookpool.app.campaign.dto.request.DeadlineFilter;
import kr.co.bookpool.app.campaign.dto.request.SortKey;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.CategoryCountResponse;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Campaign", description = "캠페인(게시판) API")
public interface CampaignControllerDocs {

	@Operation(summary = "캠페인 목록 조회", description = "검색어/카테고리/유형/마감 필터와 정렬 기준으로 캠페인을 페이징 조회합니다.")
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
				        "badgeLabel": "Reviewer",
				        "daysRemaining": 5,
				        "title": "신간 서평단 모집",
				        "bookTitle": "클린 아키텍처",
				        "publisher": "인사이트",
				        "category": "IT/개발",
				        "viewCount": 120,
				        "status": "open",
				        "recruitStartDate": "2026-06-20",
				        "recruitEndDate": "2026-07-02",
				        "announcementDate": "2026-07-05",
				        "coverImage": "https://...",
				        "description": "..."
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
	ApiResult<PageResponse<CampaignResponse>> list(
		@Parameter(description = "제목/도서명/출판사 검색어") String query,
		@Parameter(description = "출판사 정확 일치 필터 (출판사 페이지)") String publisher,
		@Parameter(description = "카테고리 필터 (다중 선택 가능). enum 이름과 한국어 라벨 모두 허용") List<CampaignCategory> categories,
		@Parameter(description = "유형 필터 (REVIEWER/BETA_READER)") List<CampaignType> types,
		@Parameter(description = "마감 필터 (ALL/WEEK/IMMINENT). withinDays가 있으면 무시된다") DeadlineFilter deadline,
		@Parameter(description = "마감까지 남은 일수 상한. deadline보다 우선한다") Integer withinDays,
		@Parameter(description = "날짜 범위 시작 (yyyy-MM-dd). 캘린더가 보는 달 조회에 쓴다") LocalDate from,
		@Parameter(description = "날짜 범위 끝 (yyyy-MM-dd)") LocalDate to,
		@Parameter(description = "from~to를 적용할 날짜 (RECRUIT_START/RECRUIT_END/ANNOUNCEMENT)") DateBasis dateBasis,
		@Parameter(description = "정렬 기준 (DEADLINE/VIEWS/LATEST)") SortKey sort,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(summary = "캠페인 상세 조회", description = "캠페인 ID로 단건을 조회하며 조회수를 1 증가시킵니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(
			responseCode = "404", description = "캠페인을 찾을 수 없음",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "CP001",
				  "message": "캠페인을 찾을 수 없습니다."
				}""")))
	})
	ApiResult<CampaignResponse> detail(Long id);

	@Operation(summary = "출판사 목록 조회", description = "게시된 캠페인의 출판사명을 중복 없이 정렬하여 반환합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<List<String>> publishers();

	@Operation(
		summary = "카테고리별 모집중 건수",
		description = "홈 카테고리 타일용 집계. 게시되고 모집중인 공고만 센다. category는 한국어 라벨.")
	@ApiResponse(
		responseCode = "200", description = "조회 성공",
		content = @Content(examples = @ExampleObject(value = """
			{
			  "success": true,
			  "code": "SUCCESS",
			  "message": "요청에 성공했습니다.",
			  "data": [
			    { "category": "IT/개발", "count": 12 },
			    { "category": "소설", "count": 4 }
			  ]
			}""")))
	ApiResult<List<CategoryCountResponse>> categoryCounts();
}
