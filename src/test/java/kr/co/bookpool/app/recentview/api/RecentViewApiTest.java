package kr.co.bookpool.app.recentview.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.app.recentview.repository.RecentViewRepository;
import kr.co.bookpool.common.response.ApiResult;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecentViewApiTest {

	private static final String PASSWORD = "password1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CampaignRepository campaignRepository;

	@Autowired
	private RecentViewRepository recentViewRepository;

	@Autowired
	private EmailVerificationStore emailVerificationStore;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		recentViewRepository.deleteAll();
		campaignRepository.deleteAll();
		memberRepository.deleteAll();
		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
	}

	@Test
	@DisplayName("캠페인 열람을 기록하면 ids 목록에 캠페인 ID가 반영된다")
	void mark_thenIds() {
		// given
		String token = tokenFor("rv-a@bookpool.kr");
		Long campaignId = saveCampaign("클린 아키텍처");

		// when
		ResponseEntity<ApiResult<Void>> marked = mark(token, campaignId);
		List<Long> ids = ids(token);

		// then
		assertThat(marked.getStatusCode()).isEqualTo(OK);
		assertThat(ids).containsExactly(campaignId);
		assertThat(recentViewRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("같은 캠페인을 다시 열람해도 중복 생성 없이 1건으로 유지된다 (멱등)")
	void mark_isIdempotent() {
		// given
		String token = tokenFor("rv-a@bookpool.kr");
		Long campaignId = saveCampaign("클린 아키텍처");

		// when
		mark(token, campaignId);
		mark(token, campaignId);

		// then
		assertThat(ids(token)).containsExactly(campaignId);
		assertThat(recentViewRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("여러 캠페인을 열람하면 가장 최근 열람한 순으로 반환된다")
	void ids_newestFirst() {
		// given
		String token = tokenFor("rv-a@bookpool.kr");
		Long first = saveCampaign("첫 번째");
		Long second = saveCampaign("두 번째");

		// when - first 먼저, second 나중, 그 뒤 first 재열람 → first가 가장 최근
		mark(token, first);
		mark(token, second);
		mark(token, first);

		// then
		assertThat(ids(token)).containsExactly(first, second);
	}

	@Test
	@DisplayName("최근 본 캠페인 목록은 캠페인 정보를 담아 반환한다")
	void list_returnsCampaigns() {
		// given
		String token = tokenFor("rv-a@bookpool.kr");
		Long campaignId = saveCampaign("클린 아키텍처");
		mark(token, campaignId);

		// when
		ResponseEntity<ApiResult<PageResponse<CampaignResponse>>> response = restClient.get()
			.uri("/me/recent-views")
			.header("Authorization", "Bearer " + token)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		List<CampaignResponse> content = response.getBody().data().content();
		assertThat(content).hasSize(1);
		assertThat(content.get(0).bookTitle()).isEqualTo("클린 아키텍처");
	}

	@Test
	@DisplayName("존재하지 않는 캠페인을 열람 기록하면 404와 CP001을 반환한다")
	void mark_nonexistentCampaign() {
		// given
		String token = tokenFor("rv-a@bookpool.kr");

		// when
		ResponseEntity<ApiResult<Void>> response = mark(token, 999999L);

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("다른 회원의 최근 본 공고는 내 목록에 보이지 않는다")
	void recentView_isolatedPerMember() {
		// given
		String tokenA = tokenFor("rv-a@bookpool.kr");
		String tokenB = tokenFor("rv-b@bookpool.kr");
		Long campaignId = saveCampaign("클린 아키텍처");
		mark(tokenA, campaignId);

		// when
		List<Long> bIds = ids(tokenB);

		// then
		assertThat(bIds).isEmpty();
	}

	@Test
	@DisplayName("토큰 없이 열람 기록을 호출하면 401과 A002를 반환한다")
	void mark_unauthorized() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/me/recent-views")
			.contentType(APPLICATION_JSON)
			.body(Map.of("campaignId", 1))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	// ---------- helpers ----------

	private List<Long> ids(String token) {
		ResponseEntity<ApiResult<List<Long>>> response = restClient.get()
			.uri("/me/recent-views/ids")
			.header("Authorization", "Bearer " + token)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		return response.getBody().data();
	}

	private ResponseEntity<ApiResult<Void>> mark(String token, Long campaignId) {
		return restClient.post()
			.uri("/me/recent-views")
			.header("Authorization", "Bearer " + token)
			.contentType(APPLICATION_JSON)
			.body(Map.of("campaignId", campaignId))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	private Long saveCampaign(String bookTitle) {
		Campaign campaign = Campaign.create(
			"제목 - " + bookTitle, bookTitle, "인사이트",
			CampaignCategory.IT, CampaignType.REVIEWER,
			"https://apply.example.com", null, "설명",
			LocalDate.now(), LocalDateTime.now().plusDays(7), LocalDate.now().plusDays(10),
			CampaignStatus.OPEN
		);
		return campaignRepository.save(campaign).getId();
	}

	private String tokenFor(String email) {
		emailVerificationStore.markVerified(email, Duration.ofMinutes(30));
		restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(new SignUpRequest(email, "북풀러", PASSWORD, true))
			.retrieve()
			.toBodilessEntity();

		ResponseEntity<ApiResult<LoginResponse>> login = restClient.post()
			.uri("/login")
			.contentType(APPLICATION_JSON)
			.body(new LoginRequest(email, PASSWORD))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		return login.getBody().data().accessToken();
	}
}
