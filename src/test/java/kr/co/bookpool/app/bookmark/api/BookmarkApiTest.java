package kr.co.bookpool.app.bookmark.api;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
import kr.co.bookpool.app.bookmark.dto.request.BookmarkRequest;
import kr.co.bookpool.app.bookmark.repository.BookmarkRepository;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookmarkApiTest {

	private static final String PASSWORD = "password1234";

	@LocalServerPort
	private int port;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private CampaignRepository campaignRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EmailVerificationStore emailVerificationStore;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		// 격리: 자식(북마크) → 부모(회원/캠페인) 순으로 정리
		bookmarkRepository.deleteAll();
		memberRepository.deleteAll();
		campaignRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			// 4xx/5xx도 예외 없이 ResponseEntity로 받기 위해 기본 에러 처리를 no-op으로 대체
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
	}

	@Test
	@DisplayName("즐겨찾기를 추가하면 201을 반환하고 ids 목록에 캠페인 ID가 반영된다")
	void add_success() {
		// given
		String token = signUpAndLogin("add@bookpool.kr");
		Long campaignId = createCampaign("스프링 부트 입문서");

		// when
		ResponseEntity<ApiResult<Void>> response = addBookmark(token, campaignId);

		// then
		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().code()).isEqualTo("SUCCESS");
		assertThat(getBookmarkedIds(token)).containsExactly(campaignId);
	}

	@Test
	@DisplayName("존재하지 않는 캠페인을 즐겨찾기하면 404와 CP001을 반환한다")
	void add_campaignNotFound() {
		// given
		String token = signUpAndLogin("nocampaign@bookpool.kr");

		// when
		ResponseEntity<ApiResult<Void>> response = addBookmark(token, 999_999L);

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("이미 즐겨찾기한 캠페인을 다시 추가하면 409와 B001을 반환한다")
	void add_duplicate() {
		// given
		String token = signUpAndLogin("dup@bookpool.kr");
		Long campaignId = createCampaign("중복 테스트 도서");
		addBookmark(token, campaignId); // 최초 추가

		// when
		ResponseEntity<ApiResult<Void>> response = addBookmark(token, campaignId);

		// then
		assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("B001");
		assertThat(getBookmarkedIds(token)).containsExactly(campaignId);
	}

	@Test
	@DisplayName("campaignId가 없으면 400과 C001(필드 에러)을 반환한다")
	void add_validationFail() {
		// given
		String token = signUpAndLogin("invalid@bookpool.kr");

		// when - campaignId 누락(null)
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.post()
			.uri("/me/bookmarks")
			.header("Authorization", "Bearer " + token)
			.contentType(APPLICATION_JSON)
			.body(new BookmarkRequest(null))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("C001");
		assertThat(response.getBody().data())
			.extracting(FieldError::field)
			.contains("campaignId");
	}

	@Test
	@DisplayName("즐겨찾기를 삭제하면 200을 반환하고 ids 목록에서 제거된다")
	void remove_success() {
		// given
		String token = signUpAndLogin("remove@bookpool.kr");
		Long campaignId = createCampaign("삭제 대상 도서");
		addBookmark(token, campaignId);

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.delete()
			.uri("/me/bookmarks/{campaignId}", campaignId)
			.header("Authorization", "Bearer " + token)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(getBookmarkedIds(token)).isEmpty();
	}

	@Test
	@DisplayName("즐겨찾기하지 않은 캠페인을 삭제하면 404와 B002를 반환한다")
	void remove_notFound() {
		// given
		String token = signUpAndLogin("removenone@bookpool.kr");
		Long campaignId = createCampaign("즐겨찾기 안 한 도서");

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.delete()
			.uri("/me/bookmarks/{campaignId}", campaignId)
			.header("Authorization", "Bearer " + token)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("B002");
	}

	@Test
	@DisplayName("즐겨찾기한 캠페인 목록을 최신 추가순으로 페이지로 반환한다")
	void getBookmarkedCampaigns_latestFirst() {
		// given - 세 개를 순서대로 추가 (마지막에 추가한 것이 가장 최신)
		String token = signUpAndLogin("list@bookpool.kr");
		Long first = createCampaign("첫 번째 도서");
		Long second = createCampaign("두 번째 도서");
		Long third = createCampaign("세 번째 도서");
		addBookmark(token, first);
		addBookmark(token, second);
		addBookmark(token, third);

		// when
		ResponseEntity<ApiResult<PageResponse<CampaignResponse>>> response = restClient.get()
			.uri("/me/bookmarks?page=0&size=10")
			.header("Authorization", "Bearer " + token)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		PageResponse<CampaignResponse> page = response.getBody().data();
		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.content())
			.extracting(CampaignResponse::id)
			.containsExactly(
				String.valueOf(third),
				String.valueOf(second),
				String.valueOf(first)
			);
	}

	@Test
	@DisplayName("다른 회원의 즐겨찾기는 내 목록에 보이지 않는다(회원 간 격리)")
	void isolation_betweenMembers() {
		// given - 회원 A가 캠페인을 즐겨찾기, 회원 B는 아무것도 안 함
		String tokenA = signUpAndLogin("a@bookpool.kr");
		String tokenB = signUpAndLogin("b@bookpool.kr");
		Long campaignId = createCampaign("A만 즐겨찾기한 도서");
		addBookmark(tokenA, campaignId);

		// when
		List<Long> idsOfA = getBookmarkedIds(tokenA);
		List<Long> idsOfB = getBookmarkedIds(tokenB);

		// then
		assertThat(idsOfA).containsExactly(campaignId);
		assertThat(idsOfB).isEmpty();
	}

	@Test
	@DisplayName("토큰 없이 즐겨찾기 목록을 조회하면 401과 A002를 반환한다")
	void getBookmarkedIds_withoutToken() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/me/bookmarks/ids")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	@Test
	@DisplayName("토큰 없이 즐겨찾기를 추가하면 401과 A002를 반환한다")
	void add_withoutToken() {
		// given
		Long campaignId = createCampaign("비로그인 추가 시도 도서");

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/me/bookmarks")
			.contentType(APPLICATION_JSON)
			.body(new BookmarkRequest(campaignId))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	@Test
	@DisplayName("같은 캠페인을 동시에 즐겨찾기해도 한 건만 성공하고 나머지는 409가 된다(500이 아님)")
	void add_concurrentSameCampaign() throws InterruptedException {
		// given
		String token = signUpAndLogin("race@bookpool.kr");
		Long campaignId = createCampaign("동시성 테스트 도서");
		int threadCount = 16;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threadCount);

		AtomicInteger created = new AtomicInteger();
		AtomicInteger conflict = new AtomicInteger();
		AtomicInteger serverError = new AtomicInteger();

		// when - 모든 스레드가 동시에 같은 캠페인을 즐겨찾기
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					HttpStatusCode status = restClient.post()
						.uri("/me/bookmarks")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.body(new BookmarkRequest(campaignId))
						.retrieve()
						.toBodilessEntity()
						.getStatusCode();

					if (status.isSameCodeAs(CREATED)) {
						created.incrementAndGet();
					} else if (status.isSameCodeAs(CONFLICT)) {
						conflict.incrementAndGet();
					} else if (status.is5xxServerError()) {
						serverError.incrementAndGet();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		ready.await();
		start.countDown();
		done.await(10, SECONDS);
		executor.shutdownNow();

		// then - 정확히 한 건만 성공, 나머지는 409, 500은 없어야 함
		assertThat(created.get()).isEqualTo(1);
		assertThat(serverError.get()).isZero();
		assertThat(conflict.get()).isEqualTo(threadCount - 1);
		assertThat(getBookmarkedIds(token)).containsExactly(campaignId);
	}

	// --- helpers ---

	private String signUpAndLogin(String email) {
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

	private Long createCampaign(String title) {
		Campaign campaign = Campaign.create(
			title,
			title + " (원서)",
			"북풀출판사",
			CampaignCategory.IT,
			CampaignType.REVIEWER,
			"https://bookpool.kr/apply",
			"https://bookpool.kr/cover.png",
			"테스트용 캠페인 설명",
			LocalDate.now(),
			LocalDateTime.now().plusDays(7),
			LocalDate.now().plusDays(10),
			CampaignStatus.OPEN
		);
		return campaignRepository.save(campaign).getId();
	}

	private ResponseEntity<ApiResult<Void>> addBookmark(String token, Long campaignId) {
		return restClient.post()
			.uri("/me/bookmarks")
			.header("Authorization", "Bearer " + token)
			.contentType(APPLICATION_JSON)
			.body(new BookmarkRequest(campaignId))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	private List<Long> getBookmarkedIds(String token) {
		return restClient.get()
			.uri("/me/bookmarks/ids")
			.header("Authorization", "Bearer " + token)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<List<Long>>>() {
			})
			.getBody()
			.data();
	}
}
