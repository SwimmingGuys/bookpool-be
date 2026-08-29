package kr.co.bookpool.app.notification.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.campaign.dto.request.CampaignCreateRequest;
import kr.co.bookpool.app.campaign.dto.request.PublishStatusUpdateRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.notification.dto.NotificationSubscriptionDto;
import kr.co.bookpool.app.notification.dto.response.NotificationResponse;
import kr.co.bookpool.app.notification.repository.NotificationRepository;
import kr.co.bookpool.app.notification.repository.NotificationSubscriptionRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationApiTest {

	private static final String ADMIN_EMAIL = "noti-admin@bookpool.kr";
	private static final String ADMIN_PASSWORD = "admin1234";
	private static final String USER_EMAIL = "noti-user@bookpool.kr";
	private static final String USER_PASSWORD = "user1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CampaignRepository campaignRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationSubscriptionRepository subscriptionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private RestClient restClient;
	private String adminToken;
	private String userToken;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
		subscriptionRepository.deleteAll();
		campaignRepository.deleteAll();
		memberRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		memberRepository.save(
			Member.createAdmin(ADMIN_EMAIL, "알림관리자", passwordEncoder.encode(ADMIN_PASSWORD)));
		adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

		memberRepository.save(
			Member.create(USER_EMAIL, "구독자", passwordEncoder.encode(USER_PASSWORD), false));
		userToken = login(USER_EMAIL, USER_PASSWORD);
	}

	@Test
	@DisplayName("구독 조건에 맞는 공고가 게시되면 알림이 쌓인다")
	void fanout_onPublish() {
		subscribe(List.of(), List.of("IT/개발"), List.of());

		createCampaign("IT 서평단 모집", CampaignCategory.IT, PublishStatus.PUBLISHED);

		PageResponse<NotificationResponse> page = myNotifications();
		assertThat(page.content()).hasSize(1);
		NotificationResponse notification = page.content().get(0);
		assertThat(notification.kind()).isEqualTo("NEW_RECRUITMENT");
		assertThat(notification.campaignTitle()).isEqualTo("IT 서평단 모집");
		assertThat(notification.isRead()).isFalse();
	}

	@Test
	@DisplayName("구독 조건과 맞지 않으면 알림이 오지 않는다")
	void fanout_noMatch() {
		subscribe(List.of(), List.of("소설"), List.of());

		createCampaign("IT 서평단 모집", CampaignCategory.IT, PublishStatus.PUBLISHED);

		assertThat(myNotifications().content()).isEmpty();
	}

	@Test
	@DisplayName("구독 조건을 하나도 고르지 않았으면 알림을 보내지 않는다")
	void fanout_emptySubscription() {
		subscribe(List.of(), List.of(), List.of());

		createCampaign("IT 서평단 모집", CampaignCategory.IT, PublishStatus.PUBLISHED);

		assertThat(myNotifications().content()).isEmpty();
	}

	@Test
	@DisplayName("검수 대기로 등록하면 알림이 없다가, 게시하는 순간 한 번만 발송된다")
	void fanout_onlyWhenPublished() {
		subscribe(List.of(), List.of("IT/개발"), List.of());

		// 검수 대기로 등록 → 알림 없음
		String campaignId = createCampaign("검수 대기 공고", CampaignCategory.IT, PublishStatus.DRAFT).id();
		assertThat(myNotifications().content()).isEmpty();

		// 게시 → 알림 1건
		changePublishStatus(campaignId, PublishStatus.PUBLISHED);
		assertThat(myNotifications().content()).hasSize(1);

		// 다시 내렸다 올려도 이미 발송된 건 그대로, 새로 공개될 때만 추가된다
		changePublishStatus(campaignId, PublishStatus.DRAFT);
		assertThat(myNotifications().content()).hasSize(1);
		changePublishStatus(campaignId, PublishStatus.PUBLISHED);
		assertThat(myNotifications().content()).hasSize(2);
	}

	@Test
	@DisplayName("알림을 읽음 처리하면 isRead가 true가 되고, 모두 읽음도 동작한다")
	void markRead() {
		subscribe(List.of(), List.of("IT/개발"), List.of());
		createCampaign("첫 번째 공고", CampaignCategory.IT, PublishStatus.PUBLISHED);
		createCampaign("두 번째 공고", CampaignCategory.IT, PublishStatus.PUBLISHED);

		List<NotificationResponse> notifications = myNotifications().content();
		assertThat(notifications).hasSize(2);

		// 한 건 읽음
		ResponseEntity<ApiResult<Void>> read = restClient.post()
			.uri("/me/notifications/" + notifications.get(0).id() + "/read")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(read.getStatusCode()).isEqualTo(OK);
		assertThat(myNotifications().content())
			.filteredOn(NotificationResponse::isRead)
			.hasSize(1);

		// 모두 읽음
		restClient.post()
			.uri("/me/notifications/read-all")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<Void>>() {
			});
		assertThat(myNotifications().content()).allMatch(NotificationResponse::isRead);
	}

	@Test
	@DisplayName("남의 알림을 읽음 처리하려 하면 404 NT001을 반환한다")
	void markRead_notOwned() {
		subscribe(List.of(), List.of("IT/개발"), List.of());
		createCampaign("공고", CampaignCategory.IT, PublishStatus.PUBLISHED);
		String notificationId = myNotifications().content().get(0).id();

		// 관리자 토큰으로 남의 알림을 읽음 처리 시도
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/me/notifications/" + notificationId + "/read")
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("NT001");
	}

	@Test
	@DisplayName("비로그인 상태로 알림을 조회하면 401 A002를 반환한다")
	void listMine_unauthorized() {
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/me/notifications")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	// ---------- helpers ----------

	private void subscribe(List<String> types, List<String> categories, List<String> publishers) {
		restClient.put()
			.uri("/me/notification-subscription")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new NotificationSubscriptionDto(types, categories, publishers))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<NotificationSubscriptionDto>>() {
			});
	}

	private CampaignResponse createCampaign(String title, CampaignCategory category, PublishStatus publishStatus) {
		return restClient.post()
			.uri("/admin/campaigns")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new CampaignCreateRequest(
				title, title + " 도서", "테스트 출판사", category, CampaignType.REVIEWER,
				"https://apply.example.com", null, "설명",
				LocalDate.now(), LocalDateTime.now().plusDays(7), LocalDate.now().plusDays(14),
				CampaignStatus.OPEN,
				null, null, null, null, null, null, null, null, publishStatus))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<CampaignResponse>>() {
			})
			.getBody()
			.data();
	}

	private void changePublishStatus(String campaignId, PublishStatus publishStatus) {
		restClient.patch()
			.uri("/admin/campaigns/" + campaignId + "/publish-status")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new PublishStatusUpdateRequest(publishStatus))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<CampaignResponse>>() {
			});
	}

	private PageResponse<NotificationResponse> myNotifications() {
		return restClient.get()
			.uri("/me/notifications")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<PageResponse<NotificationResponse>>>() {
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
