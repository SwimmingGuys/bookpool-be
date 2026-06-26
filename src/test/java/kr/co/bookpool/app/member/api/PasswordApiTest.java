package kr.co.bookpool.app.member.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

import java.time.Duration;

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
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.PasswordChangeRequest;
import kr.co.bookpool.app.member.dto.request.PasswordResetRequest;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.security.RefreshTokenStore;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PasswordApiTest {

	private static final String EMAIL = "pw@bookpool.kr";
	private static final String PASSWORD = "password1234";
	private static final String NEW_PASSWORD = "newPassword1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EmailVerificationStore verificationStore;

	@Autowired
	private RefreshTokenStore refreshTokenStore;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		// 회원가입은 이메일 인증을 전제로 하므로 인증 완료 상태를 만든 뒤 가입한다
		verificationStore.markVerified(EMAIL, Duration.ofMinutes(30));
		signUp(new SignUpRequest(EMAIL, "북풀러", PASSWORD, true));
		// 가입 시 소비되지 않고 남는 인증 상태를 정리해 재설정 테스트에 영향을 주지 않게 한다
		verificationStore.deleteVerified(EMAIL);
	}

	// --- PATCH /api/me/password ---

	@Test
	@DisplayName("로그인 상태에서 현재 비밀번호로 변경하면 200을 반환하고 새 비밀번호로 로그인된다")
	void changePassword_success() {
		// given
		String accessToken = accessToken();

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me/password")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(APPLICATION_JSON)
			.body(new PasswordChangeRequest(PASSWORD, NEW_PASSWORD))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(login(new LoginRequest(EMAIL, NEW_PASSWORD)).getStatusCode()).isEqualTo(OK);
	}

	@Test
	@DisplayName("현재 비밀번호가 틀리면 400과 M005를 반환한다")
	void changePassword_wrongCurrentPassword() {
		// given
		String accessToken = accessToken();

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me/password")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(APPLICATION_JSON)
			.body(new PasswordChangeRequest("wrongpassword", NEW_PASSWORD))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("M005");
	}

	@Test
	@DisplayName("토큰 없이 비밀번호 변경을 호출하면 401과 A002를 반환한다")
	void changePassword_withoutToken() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me/password")
			.contentType(APPLICATION_JSON)
			.body(new PasswordChangeRequest(PASSWORD, NEW_PASSWORD))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	// --- POST /api/password/email/code ---

	@Test
	@DisplayName("가입된 이메일로 재설정 코드를 발송하면 200을 반환하고 코드가 저장된다")
	void sendResetCode_success() {
		// when
		ResponseEntity<ApiResult<Void>> response = post("/password/email/code", new EmailCodeRequest(EMAIL));

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(verificationStore.findCode(EMAIL)).isPresent();
	}

	@Test
	@DisplayName("가입되지 않은 이메일로 재설정 코드를 발송하면 404와 M002를 반환한다")
	void sendResetCode_unregisteredEmail() {
		// when
		ResponseEntity<ApiResult<Void>> response =
			post("/password/email/code", new EmailCodeRequest("nobody@bookpool.kr"));

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("M002");
	}

	// --- POST /api/password/reset ---

	@Test
	@DisplayName("이메일 인증을 완료하면 비밀번호 재설정에 성공하고 새 비밀번호로 로그인된다")
	void resetPassword_success() {
		// given - 재설정 인증 완료 상태
		verificationStore.markVerified(EMAIL, Duration.ofMinutes(30));

		// when
		ResponseEntity<ApiResult<Void>> response =
			post("/password/reset", new PasswordResetRequest(EMAIL, NEW_PASSWORD));

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(login(new LoginRequest(EMAIL, NEW_PASSWORD)).getStatusCode()).isEqualTo(OK);
		// 인증 상태가 소비되어 재사용할 수 없다
		assertThat(verificationStore.isVerified(EMAIL)).isFalse();
	}

	@Test
	@DisplayName("이메일 인증 없이 재설정을 시도하면 400과 M006을 반환한다")
	void resetPassword_withoutVerification() {
		// when
		ResponseEntity<ApiResult<Void>> response =
			post("/password/reset", new PasswordResetRequest(EMAIL, NEW_PASSWORD));

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("M006");
	}

	@Test
	@DisplayName("비밀번호를 재설정하면 기존 리프레시 토큰이 무효화된다")
	void resetPassword_invalidatesRefreshToken() {
		// given - 로그인으로 리프레시 토큰을 발급해 두고, 재설정 인증을 완료한다
		login(new LoginRequest(EMAIL, PASSWORD));
		Long memberId = memberRepository.findByEmail(EMAIL).map(Member::getId).orElseThrow();
		assertThat(refreshTokenStore.find(memberId)).isPresent();
		verificationStore.markVerified(EMAIL, Duration.ofMinutes(30));

		// when
		post("/password/reset", new PasswordResetRequest(EMAIL, NEW_PASSWORD));

		// then
		assertThat(refreshTokenStore.find(memberId)).isEmpty();
	}

	private String accessToken() {
		return login(new LoginRequest(EMAIL, PASSWORD)).getBody().data().accessToken();
	}

	private ResponseEntity<ApiResult<LoginResponse>> login(LoginRequest request) {
		return restClient.post()
			.uri("/login")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	private ResponseEntity<ApiResult<Void>> post(String uri, Object body) {
		return restClient.post()
			.uri(uri)
			.contentType(APPLICATION_JSON)
			.body(body)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	private void signUp(SignUpRequest request) {
		restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();
	}
}
