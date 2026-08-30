package kr.co.bookpool.app.review.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.entity.ReviewChannel;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.review.dto.request.ReviewDecisionRequest;
import kr.co.bookpool.app.review.dto.request.ReviewRequest;
import kr.co.bookpool.app.review.dto.request.ReviewStatusRequest;
import kr.co.bookpool.app.review.dto.response.ReviewResponse;
import kr.co.bookpool.app.review.entity.ReviewStatus;
import kr.co.bookpool.app.review.entity.ReviewSubmissionStatus;
import kr.co.bookpool.app.review.repository.ReviewRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewApiTest {

	private static final String ADMIN_EMAIL = "review-admin@bookpool.kr";
	private static final String ADMIN_PASSWORD = "admin1234";
	private static final String USER_EMAIL = "review-user@bookpool.kr";
	private static final String USER_PASSWORD = "user1234";
	private static final String OTHER_EMAIL = "review-other@bookpool.kr";
	private static final String OTHER_PASSWORD = "other1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CampaignRepository campaignRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private RestClient restClient;
	private String adminToken;
	private String userToken;
	private String otherToken;
	private Long campaignId;

	@BeforeEach
	void setUp() {
		reviewRepository.deleteAll();
		campaignRepository.deleteAll();
		memberRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		memberRepository.save(
			Member.createAdmin(ADMIN_EMAIL, "서평관리자", passwordEncoder.encode(ADMIN_PASSWORD)));
		adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

		memberRepository.save(
			Member.create(USER_EMAIL, "리뷰어", passwordEncoder.encode(USER_PASSWORD), false));
		userToken = login(USER_EMAIL, USER_PASSWORD);

		memberRepository.save(
			Member.create(OTHER_EMAIL, "다른사람", passwordEncoder.encode(OTHER_PASSWORD), false));
		otherToken = login(OTHER_EMAIL, OTHER_PASSWORD);

		campaignId = campaignRepository.save(Campaign.builder()
			.title("서평 대상 공고")
			.bookTitle("클린 아키텍처")
			.publisherName("인사이트")
			.category(CampaignCategory.IT)
			.type(CampaignType.REVIEWER)
			.applyUrl("https://apply.example.com")
			.deadlineAt(LocalDateTime.now().plusDays(7))
			.announcementDate(LocalDate.now().plusDays(14))
			.status(CampaignStatus.OPEN)
			.build()).getId();
	}

	@Test
	@DisplayName("서평을 제출하면 확인 대기 상태로 저장되고, 인증 전에는 공개 목록에 안 보인다")
	void submit_thenHiddenUntilApproved() {
		ReviewResponse submitted = submit(userToken, 5, "실무에 바로 적용할 수 있었습니다.");

		assertThat(submitted.submissionStatus()).isEqualTo(ReviewSubmissionStatus.SUBMITTED.name());
		assertThat(submitted.status()).isEqualTo(ReviewStatus.VISIBLE.name());
		assertThat(submitted.authorNickname()).isEqualTo("리뷰어");
		assertThat(submitted.isMine()).isTrue();

		// 인증 전에는 공개 목록에 없다
		assertThat(publicList(null).content()).isEmpty();
	}

	@Test
	@DisplayName("관리자가 인증하면 공개 목록에 노출되고, 비로그인도 볼 수 있다")
	void approve_thenVisibleToAnonymous() {
		String reviewId = submit(userToken, 4, "구성이 깔끔합니다.").id();

		decide(reviewId, ReviewSubmissionStatus.APPROVED, null);

		PageResponse<ReviewResponse> anonymous = publicList(null);
		assertThat(anonymous.content()).extracting(ReviewResponse::content)
			.containsExactly("구성이 깔끔합니다.");
		// 비로그인 조회에서는 isMine이 false
		assertThat(anonymous.content().get(0).isMine()).isFalse();

		// 작성자 본인이 조회하면 isMine이 true
		assertThat(publicList(userToken).content().get(0).isMine()).isTrue();
	}

	@Test
	@DisplayName("반려하면 사유가 함께 저장되고, 사유 없이 반려하면 400을 반환한다")
	void reject() {
		String reviewId = submit(userToken, 3, "링크 확인 부탁드려요.").id();

		// 사유 없이 반려 → 400
		ResponseEntity<ApiResult<Void>> noReason = restClient.post()
			.uri("/admin/reviews/" + reviewId + "/decision")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new ReviewDecisionRequest(ReviewSubmissionStatus.REJECTED, "  "))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(noReason.getStatusCode()).isEqualTo(BAD_REQUEST);

		// 사유와 함께 반려
		ReviewResponse rejected = decide(reviewId, ReviewSubmissionStatus.REJECTED, "링크가 열리지 않습니다.");
		assertThat(rejected.submissionStatus()).isEqualTo(ReviewSubmissionStatus.REJECTED.name());
		assertThat(rejected.rejectReason()).isEqualTo("링크가 열리지 않습니다.");

		// 반려된 서평은 공개 목록에 없지만 본인의 '내 서평'에는 사유와 함께 보인다
		assertThat(publicList(null).content()).isEmpty();
		assertThat(myReviews().content()).singleElement()
			.extracting(ReviewResponse::rejectReason)
			.isEqualTo("링크가 열리지 않습니다.");
	}

	@Test
	@DisplayName("관리자가 숨기면 인증된 서평이라도 공개 목록에서 빠진다")
	void hide() {
		String reviewId = submit(userToken, 5, "좋았습니다.").id();
		decide(reviewId, ReviewSubmissionStatus.APPROVED, null);
		assertThat(publicList(null).content()).hasSize(1);

		ResponseEntity<ApiResult<ReviewResponse>> hidden = restClient.patch()
			.uri("/admin/reviews/" + reviewId + "/status")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new ReviewStatusRequest(ReviewStatus.HIDDEN))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(hidden.getStatusCode()).isEqualTo(OK);
		assertThat(publicList(null).content()).isEmpty();
	}

	@Test
	@DisplayName("같은 공고에 두 번 제출하면 409 R002를 반환한다")
	void submit_duplicate() {
		submit(userToken, 5, "첫 번째 서평");

		ResponseEntity<ApiResult<Void>> second = restClient.post()
			.uri("/reviews")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ReviewRequest(campaignId, 4, "두 번째 서평", ReviewChannel.BLOG, "https://blog.example.com/2"))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(second.getStatusCode()).isEqualTo(CONFLICT);
		assertThat(second.getBody().code()).isEqualTo("R002");
	}

	@Test
	@DisplayName("남의 서평을 수정하면 403 R003을 반환한다")
	void update_forbiddenForOthers() {
		String reviewId = submit(userToken, 5, "내 서평").id();

		ResponseEntity<ApiResult<Void>> response = restClient.put()
			.uri("/reviews/" + reviewId)
			.header("Authorization", "Bearer " + otherToken)
			.contentType(APPLICATION_JSON)
			.body(new ReviewRequest(null, 1, "남의 서평 고치기", ReviewChannel.BLOG, "https://blog.example.com/x"))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("R003");
	}

	@Test
	@DisplayName("인증된 서평을 본인이 수정하면 다시 확인 대기로 돌아간다")
	void update_resetsToSubmitted() {
		String reviewId = submit(userToken, 5, "처음 내용").id();
		decide(reviewId, ReviewSubmissionStatus.APPROVED, null);

		ResponseEntity<ApiResult<ReviewResponse>> updated = restClient.put()
			.uri("/reviews/" + reviewId)
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ReviewRequest(null, 4, "고친 내용", ReviewChannel.YES24, "https://yes24.example.com/1"))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(updated.getStatusCode()).isEqualTo(OK);
		assertThat(updated.getBody().data().content()).isEqualTo("고친 내용");
		assertThat(updated.getBody().data().submissionStatus())
			.isEqualTo(ReviewSubmissionStatus.SUBMITTED.name());
		// 다시 확인 대기이므로 공개 목록에서 빠진다
		assertThat(publicList(null).content()).isEmpty();
	}

	@Test
	@DisplayName("비로그인 상태로 서평을 제출하면 401 A002를 반환한다")
	void submit_unauthorized() {
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/reviews")
			.contentType(APPLICATION_JSON)
			.body(new ReviewRequest(campaignId, 5, "익명 서평", ReviewChannel.BLOG, "https://blog.example.com/a"))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	@Test
	@DisplayName("USER 토큰으로 서평을 인증하려 하면 403 A004를 반환한다")
	void decide_forbiddenForUser() {
		String reviewId = submit(userToken, 5, "내 서평").id();

		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/admin/reviews/" + reviewId + "/decision")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ReviewDecisionRequest(ReviewSubmissionStatus.APPROVED, null))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("A004");
	}

	// ---------- helpers ----------

	private ReviewResponse submit(String token, int rating, String content) {
		return restClient.post()
			.uri("/reviews")
			.header("Authorization", "Bearer " + token)
			.contentType(APPLICATION_JSON)
			.body(new ReviewRequest(campaignId, rating, content, ReviewChannel.BLOG,
				"https://blog.example.com/" + content.hashCode()))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<ReviewResponse>>() {
			})
			.getBody()
			.data();
	}

	private ReviewResponse decide(String reviewId, ReviewSubmissionStatus status, String reason) {
		return restClient.post()
			.uri("/admin/reviews/" + reviewId + "/decision")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new ReviewDecisionRequest(status, reason))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<ReviewResponse>>() {
			})
			.getBody()
			.data();
	}

	private PageResponse<ReviewResponse> publicList(String token) {
		RestClient.RequestHeadersSpec<?> spec = restClient.get()
			.uri("/reviews?campaignId=" + campaignId);
		if (token != null) {
			spec = spec.header("Authorization", "Bearer " + token);
		}
		return spec.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<PageResponse<ReviewResponse>>>() {
			})
			.getBody()
			.data();
	}

	private PageResponse<ReviewResponse> myReviews() {
		return restClient.get()
			.uri("/reviews/me")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<PageResponse<ReviewResponse>>>() {
			})
			.getBody()
			.data();
	}

	private String login(String email, String password) {
		return restClient.post()
			.uri("/login")
			.contentType(APPLICATION_JSON)
			.body(new LoginRequest(email, password))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<LoginResponse>>() {
			})
			.getBody()
			.data()
			.accessToken();
	}
}
