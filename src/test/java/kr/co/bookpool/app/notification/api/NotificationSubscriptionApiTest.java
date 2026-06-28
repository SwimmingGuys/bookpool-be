package kr.co.bookpool.app.notification.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

import java.time.Duration;
import java.util.HashMap;
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
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.app.notification.dto.NotificationSubscriptionDto;
import kr.co.bookpool.app.notification.repository.NotificationSubscriptionRepository;
import kr.co.bookpool.common.response.ApiResult;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationSubscriptionApiTest {

	private static final String PASSWORD = "password1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private NotificationSubscriptionRepository subscriptionRepository;

	@Autowired
	private EmailVerificationStore emailVerificationStore;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		subscriptionRepository.deleteAll();
		memberRepository.deleteAll();
		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
	}

	@Test
	@DisplayName("구독 설정을 한 번도 저장하지 않으면 빈 목록을 반환한다")
	void get_emptyByDefault() {
		// given
		String token = tokenFor("noti-a@bookpool.kr");

		// when
		ResponseEntity<ApiResult<NotificationSubscriptionDto>> response = get(token);

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		NotificationSubscriptionDto data = response.getBody().data();
		assertThat(data.types()).isEmpty();
		assertThat(data.categories()).isEmpty();
		assertThat(data.publishers()).isEmpty();
	}

	@Test
	@DisplayName("구독 설정을 저장하면 저장한 값이 그대로 조회된다 (JSON round-trip)")
	void save_thenGet_roundTrip() {
		// given
		String token = tokenFor("noti-a@bookpool.kr");
		NotificationSubscriptionDto request = new NotificationSubscriptionDto(
			List.of("Reviewer", "Beta Reader"),
			List.of("IT/개발", "소설"),
			List.of("인사이트", "한빛미디어")
		);

		// when
		ResponseEntity<ApiResult<NotificationSubscriptionDto>> saved = put(token, request);
		NotificationSubscriptionDto fetched = get(token).getBody().data();

		// then
		assertThat(saved.getStatusCode()).isEqualTo(OK);
		assertThat(fetched.types()).containsExactly("Reviewer", "Beta Reader");
		assertThat(fetched.categories()).containsExactly("IT/개발", "소설");
		assertThat(fetched.publishers()).containsExactly("인사이트", "한빛미디어");
	}

	@Test
	@DisplayName("구독 설정을 두 번 저장해도 회원당 1건으로 갱신된다 (업서트)")
	void save_isUpsert() {
		// given
		String token = tokenFor("noti-a@bookpool.kr");
		put(token, new NotificationSubscriptionDto(List.of("Reviewer"), List.of("IT/개발"), List.of()));

		// when - 다시 저장(갱신)
		put(token, new NotificationSubscriptionDto(List.of("Beta Reader"), List.of(), List.of("인사이트")));
		NotificationSubscriptionDto fetched = get(token).getBody().data();

		// then - 마지막 저장값으로 갱신, 중복 생성 없음
		assertThat(fetched.types()).containsExactly("Beta Reader");
		assertThat(fetched.categories()).isEmpty();
		assertThat(fetched.publishers()).containsExactly("인사이트");
		assertThat(subscriptionRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("다른 회원의 구독 설정은 서로 격리된다")
	void subscription_isolatedPerMember() {
		// given
		String tokenA = tokenFor("noti-a@bookpool.kr");
		String tokenB = tokenFor("noti-b@bookpool.kr");
		put(tokenA, new NotificationSubscriptionDto(List.of("Reviewer"), List.of("IT/개발"), List.of()));

		// when
		NotificationSubscriptionDto bData = get(tokenB).getBody().data();

		// then - B는 비어 있음
		assertThat(bData.types()).isEmpty();
		assertThat(bData.categories()).isEmpty();
		assertThat(bData.publishers()).isEmpty();
	}

	@Test
	@DisplayName("목록 필드 중 하나라도 null이면 400을 반환한다")
	void save_nullField_returns400() {
		// given
		String token = tokenFor("noti-a@bookpool.kr");
		Map<String, Object> body = new HashMap<>();
		body.put("types", null);
		body.put("categories", List.of());
		body.put("publishers", List.of());

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.put()
			.uri("/me/notification-subscription")
			.header("Authorization", "Bearer " + token)
			.contentType(APPLICATION_JSON)
			.body(body)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody().code()).isEqualTo("C001");
	}

	@Test
	@DisplayName("토큰 없이 구독 설정을 조회하면 401과 A002를 반환한다")
	void get_unauthorized() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/me/notification-subscription")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	// ---------- helpers ----------

	private ResponseEntity<ApiResult<NotificationSubscriptionDto>> get(String token) {
		return restClient.get()
			.uri("/me/notification-subscription")
			.header("Authorization", "Bearer " + token)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	private ResponseEntity<ApiResult<NotificationSubscriptionDto>> put(String token, NotificationSubscriptionDto request) {
		return restClient.put()
			.uri("/me/notification-subscription")
			.header("Authorization", "Bearer " + token)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
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
