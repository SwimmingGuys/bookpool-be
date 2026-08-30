package kr.co.bookpool.app.campaign.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.CategoryCountResponse;
import kr.co.bookpool.app.campaign.entity.BookFormat;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignSource;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import kr.co.bookpool.app.campaign.entity.ReviewChannel;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

/**
 * 공개 공고 조회 API. 프론트 보드/캘린더/출판사 페이지가 보내는 파라미터를 그대로 검증한다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CampaignSearchApiTest {

	@LocalServerPort
	private int port;

	@Autowired
	private CampaignRepository campaignRepository;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		campaignRepository.deleteAll();
		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
	}

	@Test
	@DisplayName("세분화된 카테고리는 저장 후 다시 읽어도 같은 라벨로 돌아온다")
	void category_roundTrip() {
		// given - 예전에는 ETC로 뭉개져 '기타'로 돌아오던 카테고리들
		save(campaign("자기계발 공고").category(CampaignCategory.SELF_DEVELOPMENT));
		save(campaign("학습 공고").category(CampaignCategory.EDUCATION));
		save(campaign("기획 공고").category(CampaignCategory.PLANNING_DESIGN));

		// when
		List<CampaignResponse> content = list("?sort=LATEST").content();

		// then
		assertThat(content).extracting(CampaignResponse::category)
			.containsExactlyInAnyOrder("SELF_DEVELOPMENT", "EDUCATION", "PLANNING_DESIGN");
	}

	@Test
	@DisplayName("한국어 라벨로도 카테고리를 필터링할 수 있다")
	void category_filterByKoreanLabel() {
		save(campaign("자기계발 공고").category(CampaignCategory.SELF_DEVELOPMENT));
		save(campaign("소설 공고").category(CampaignCategory.NOVEL));

		List<CampaignResponse> content = list("?categories=자기계발").content();

		assertThat(content).extracting(CampaignResponse::title).containsExactly("자기계발 공고");
	}

	@Test
	@DisplayName("검수 대기 공고는 공개 목록과 상세에서 보이지 않는다")
	void draft_hiddenFromPublic() {
		// given
		Campaign draft = save(campaign("검수 대기 공고").publishStatus(PublishStatus.DRAFT));
		save(campaign("게시된 공고"));

		// when - 목록
		PageResponse<CampaignResponse> page = list("");

		// then - 게시된 것만 보인다
		assertThat(page.content()).extracting(CampaignResponse::title).containsExactly("게시된 공고");

		// when - 상세를 직접 열어도
		ResponseEntity<ApiResult<CampaignResponse>> detail = restClient.get()
			.uri("/campaigns/" + draft.getId())
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then - 404
		assertThat(detail.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(detail.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("withinDays로 마감까지 남은 일수를 제한한다")
	void filter_withinDays() {
		save(campaign("이틀 뒤 마감").deadlineAt(LocalDateTime.now().plusDays(2)));
		save(campaign("열흘 뒤 마감").deadlineAt(LocalDateTime.now().plusDays(10)));

		List<CampaignResponse> content = list("?withinDays=3").content();

		assertThat(content).extracting(CampaignResponse::title).containsExactly("이틀 뒤 마감");
	}

	@Test
	@DisplayName("from~to와 dateBasis로 캘린더가 보는 달의 공고만 받아온다")
	void filter_dateRangeByBasis() {
		// given - 발표일이 각각 다른 달
		save(campaign("이번 달 발표")
			.announcementDate(LocalDate.of(2026, 8, 15))
			.deadlineAt(LocalDateTime.of(2026, 8, 10, 23, 59)));
		save(campaign("다음 달 발표")
			.announcementDate(LocalDate.of(2026, 9, 15))
			.deadlineAt(LocalDateTime.of(2026, 9, 10, 23, 59)));

		// when - 발표일 기준 8월
		List<CampaignResponse> content =
			list("?from=2026-08-01&to=2026-08-31&dateBasis=ANNOUNCEMENT").content();

		// then
		assertThat(content).extracting(CampaignResponse::title).containsExactly("이번 달 발표");
	}

	@Test
	@DisplayName("마감일 기준 범위는 그 날 23:59에 마감하는 공고도 포함한다")
	void filter_dateRangeIncludesEndOfDay() {
		save(campaign("말일 마감").deadlineAt(LocalDateTime.of(2026, 8, 31, 23, 59)));

		List<CampaignResponse> content =
			list("?from=2026-08-01&to=2026-08-31&dateBasis=RECRUIT_END").content();

		assertThat(content).extracting(CampaignResponse::title).containsExactly("말일 마감");
	}

	@Test
	@DisplayName("publisher로 해당 출판사의 공고만 조회한다")
	void filter_publisher() {
		save(campaign("한빛 공고").publisherName("한빛미디어"));
		save(campaign("인사이트 공고").publisherName("인사이트"));

		List<CampaignResponse> content = list("?publisher=한빛미디어").content();

		assertThat(content).extracting(CampaignResponse::title).containsExactly("한빛 공고");
	}

	@Test
	@DisplayName("sort=VIEWS는 조회순, sort=LATEST는 최신 등록순으로 정렬한다")
	void sort_viewsAndLatest() {
		Campaign first = save(campaign("먼저 등록").deadlineAt(LocalDateTime.now().plusDays(9)));
		save(campaign("나중 등록").deadlineAt(LocalDateTime.now().plusDays(3)));
		// 상세 조회로 조회수를 올린다 (실제 경로와 동일)
		restClient.get()
			.uri("/campaigns/" + first.getId())
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<CampaignResponse>>() {
			});

		assertThat(list("?sort=VIEWS").content())
			.extracting(CampaignResponse::title)
			.startsWith("먼저 등록");

		assertThat(list("?sort=LATEST").content())
			.extracting(CampaignResponse::title)
			.startsWith("나중 등록");

		// 기본값은 마감 임박순
		assertThat(list("").content())
			.extracting(CampaignResponse::title)
			.startsWith("나중 등록");
	}

	@Test
	@DisplayName("카테고리별 모집중 건수는 게시되고 모집중인 공고만 센다")
	void categoryCounts() {
		save(campaign("IT 공고 1").category(CampaignCategory.IT));
		save(campaign("IT 공고 2").category(CampaignCategory.IT));
		save(campaign("마감된 IT 공고").category(CampaignCategory.IT).status(CampaignStatus.CLOSED));
		save(campaign("검수 대기 IT 공고").category(CampaignCategory.IT)
			.publishStatus(PublishStatus.DRAFT));
		save(campaign("소설 공고").category(CampaignCategory.NOVEL));

		ResponseEntity<ApiResult<List<CategoryCountResponse>>> response = restClient.get()
			.uri("/campaigns/category-counts")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody().data())
			.containsExactlyInAnyOrder(
				new CategoryCountResponse("IT", 2),
				new CategoryCountResponse("NOVEL", 1)
			);
	}

	@Test
	@DisplayName("출판사 목록에는 검수 대기 공고의 출판사가 섞이지 않는다")
	void publishers_excludeDraft() {
		save(campaign("게시된 공고").publisherName("게시 출판사"));
		save(campaign("검수 대기 공고").publisherName("숨은 출판사")
			.publishStatus(PublishStatus.DRAFT));

		ResponseEntity<ApiResult<List<String>>> response = restClient.get()
			.uri("/campaigns/publishers")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getBody().data()).containsExactly("게시 출판사");
	}

	@Test
	@DisplayName("모집 조건과 신청 링크가 응답에 그대로 담긴다")
	void response_containsConditions() {
		save(campaign("조건 있는 공고")
			.applyUrl("https://forms.gle/abc")
			.capacity(15)
			.bookFormat(BookFormat.BOTH)
			.reviewChannels(List.of(ReviewChannel.BLOG, ReviewChannel.YES24))
			.reviewDueDate(LocalDate.of(2026, 9, 30))
			.requirements("블로그 운영 3개월 이상"));

		CampaignResponse found = list("").content().get(0);

		assertThat(found.applyUrl()).isEqualTo("https://forms.gle/abc");
		assertThat(found.capacity()).isEqualTo(15);
		assertThat(found.bookFormat()).isEqualTo("BOTH");
		assertThat(found.reviewChannels()).containsExactly("BLOG", "YES24");
		assertThat(found.reviewDueDate()).isEqualTo(LocalDate.of(2026, 9, 30));
		assertThat(found.requirements()).isEqualTo("블로그 운영 3개월 이상");
		assertThat(found.source()).isEqualTo("MANUAL");
		assertThat(found.publishStatus()).isEqualTo("PUBLISHED");
	}

	// ---------- helpers ----------

	private PageResponse<CampaignResponse> list(String queryString) {
		ResponseEntity<ApiResult<PageResponse<CampaignResponse>>> response = restClient.get()
			.uri("/campaigns" + queryString)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(response.getStatusCode()).isEqualTo(OK);
		return response.getBody().data();
	}

	private Campaign save(Campaign.CampaignBuilder builder) {
		return campaignRepository.save(builder.build());
	}

	private Campaign.CampaignBuilder campaign(String title) {
		return Campaign.builder()
			.title(title)
			.bookTitle(title + " 도서")
			.publisherName("테스트 출판사")
			.category(CampaignCategory.IT)
			.type(CampaignType.REVIEWER)
			.applyUrl("https://apply.example.com")
			.deadlineAt(LocalDateTime.now().plusDays(7))
			.announcementDate(LocalDate.now().plusDays(14))
			.status(CampaignStatus.OPEN)
			.source(CampaignSource.MANUAL)
			.publishStatus(PublishStatus.PUBLISHED);
	}
}
