package kr.co.bookpool.app.member.api;

import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

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

import java.time.Duration;

import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.response.SignUpResponse;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberApiTest {

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EmailVerificationStore emailVerificationStore;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			// 4xx/5xx도 예외 없이 ResponseEntity로 받기 위해 기본 에러 처리를 no-op으로 대체
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
	}

	@Test
	@DisplayName("회원가입에 성공하면 201과 가입 정보를 반환한다")
	void signUp_success() {
		// given - 이메일 인증을 마친 상태
		verifyEmail("test@bookpool.kr");
		SignUpRequest request = new SignUpRequest("test@bookpool.kr", "북풀러", "password1234", true);

		// when
		ResponseEntity<ApiResult<SignUpResponse>> response = restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().code()).isEqualTo("SUCCESS");
		assertThat(response.getBody().data().email()).isEqualTo("test@bookpool.kr");
		assertThat(response.getBody().data().nickname()).isEqualTo("북풀러");
		assertThat(memberRepository.existsByEmail("test@bookpool.kr")).isTrue();
	}

	@Test
	@DisplayName("이미 가입된 이메일로 가입하면 409와 에러 코드를 반환한다")
	void signUp_duplicateEmail() {
		// given
		verifyEmail("dup@bookpool.kr");
		SignUpRequest request = new SignUpRequest("dup@bookpool.kr", "닉네임", "password1234", true);
		signUp(request); // 최초 가입

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("M001");
	}

	@Test
	@DisplayName("입력값 검증에 실패하면 400과 필드 에러 목록을 반환한다")
	void signUp_validationFail() {
		// given - 잘못된 이메일 형식 + 짧은 비밀번호
		SignUpRequest request = new SignUpRequest("not-an-email", "닉네임", "short", true);

		// when
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
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
			.contains("email", "password");
	}

	@Test
	@DisplayName("동일 이메일로 동시에 가입을 시도하면 한 건만 성공하고 나머지는 409가 된다(500이 아님)")
	void signUp_concurrentSameEmail() throws InterruptedException {
		// given
		verifyEmail("race@bookpool.kr");
		int threadCount = 16;
		SignUpRequest request = new SignUpRequest("race@bookpool.kr", "닉네임", "password1234", true);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threadCount);

		AtomicInteger created = new AtomicInteger();
		AtomicInteger conflict = new AtomicInteger();
		AtomicInteger serverError = new AtomicInteger();

		// when - 모든 스레드가 동시에 같은 이메일로 가입 요청
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					HttpStatusCode status = restClient.post()
						.uri("/signup")
						.contentType(APPLICATION_JSON)
						.body(request)
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
		assertThat(conflict.get()).isEqualTo(threadCount - 1);
		assertThat(serverError.get()).isZero();
		assertThat(memberRepository.existsByEmail("race@bookpool.kr")).isTrue();
	}

	private void signUp(SignUpRequest request) {
		restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();
	}

	/** 회원가입 전제 조건인 '이메일 인증 완료' 상태를 만들어 둔다. */
	private void verifyEmail(String email) {
		emailVerificationStore.markVerified(email, Duration.ofMinutes(30));
	}
}
