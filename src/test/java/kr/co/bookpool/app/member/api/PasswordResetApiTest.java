package kr.co.bookpool.app.member.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

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
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.app.member.dto.request.ResetPasswordRequest;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PasswordResetApiTest {

	private static final String OLD_PASSWORD = "oldpassword1234";
	private static final String NEW_PASSWORD = "newpassword5678";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmailVerificationStore verificationStore;

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
	@DisplayName("코드 발송→검증→재설정을 거치면 새 비밀번호로 로그인되고 기존 비밀번호 로그인은 실패한다")
	void resetFlow_success() {
		// given - 가입된 회원을 만들고, 코드 발송 후 저장된 코드로 인증을 완료한다
		String email = "reset-flow@bookpool.kr";
		createMember(email, OLD_PASSWORD);

		ResponseEntity<ApiResult<Void>> sendResponse = post("/password/email/code", new EmailCodeRequest(email));
		assertThat(sendResponse.getStatusCode()).isEqualTo(OK);

		String code = verificationStore.findCode(email).orElseThrow();
		ResponseEntity<ApiResult<Void>> verifyResponse =
			post("/password/email/verify", new EmailVerifyRequest(email, code));
		assertThat(verifyResponse.getStatusCode()).isEqualTo(OK);

		// when - 새 비밀번호로 재설정한다
		ResponseEntity<ApiResult<Void>> resetResponse =
			post("/password/reset", new ResetPasswordRequest(email, NEW_PASSWORD));

		// then - 재설정에 성공하고, 새 비밀번호로만 로그인된다
		assertThat(resetResponse.getStatusCode()).isEqualTo(OK);
		assertThat(resetResponse.getBody()).isNotNull();
		assertThat(resetResponse.getBody().success()).isTrue();

		ResponseEntity<ApiResult<LoginResponse>> newLogin = login(new LoginRequest(email, NEW_PASSWORD));
		assertThat(newLogin.getStatusCode()).isEqualTo(OK);
		assertThat(newLogin.getBody()).isNotNull();
		assertThat(newLogin.getBody().data().accessToken()).isNotBlank();

		ResponseEntity<ApiResult<LoginResponse>> oldLogin = login(new LoginRequest(email, OLD_PASSWORD));
		assertThat(oldLogin.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(oldLogin.getBody()).isNotNull();
		assertThat(oldLogin.getBody().code()).isEqualTo("A001");
	}

	@Test
	@DisplayName("미가입 이메일로 코드를 발송하면 404와 M002를 반환한다")
	void sendCode_unregisteredEmail() {
		// given - 회원으로 저장하지 않은 이메일
		String email = "reset-nobody@bookpool.kr";

		// when
		ResponseEntity<ApiResult<Void>> response = post("/password/email/code", new EmailCodeRequest(email));

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("M002");
	}

	@Test
	@DisplayName("틀린 코드로 검증하면 400과 M004를 반환하고 인증 완료되지 않는다")
	void verify_wrongCode() {
		// given - 가입 회원에게 코드를 발송한다
		String email = "reset-wrongcode@bookpool.kr";
		createMember(email, OLD_PASSWORD);
		post("/password/email/code", new EmailCodeRequest(email));

		// when - 저장된 코드와 다른 값으로 검증한다
		ResponseEntity<ApiResult<Void>> response =
			post("/password/email/verify", new EmailVerifyRequest(email, "000000"));

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("M004");
		assertThat(verificationStore.isVerified(email)).isFalse();
	}

	@Test
	@DisplayName("이메일 인증을 완료하지 않은 상태로 재설정하면 400과 M003을 반환한다")
	void reset_withoutVerification() {
		// given - 가입은 했으나 이메일 인증을 하지 않은 회원
		String email = "reset-unverified@bookpool.kr";
		createMember(email, OLD_PASSWORD);

		// when
		ResponseEntity<ApiResult<Void>> response =
			post("/password/reset", new ResetPasswordRequest(email, NEW_PASSWORD));

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("M003");
	}

	@Test
	@DisplayName("새 비밀번호가 8자 미만이면 400과 C001을 반환한다")
	void reset_tooShortPassword() {
		// given - 인증까지 완료한 회원이라도 입력값 검증이 먼저 동작한다
		String email = "reset-shortpw@bookpool.kr";
		createMember(email, OLD_PASSWORD);
		verificationStore.markVerified(email, java.time.Duration.ofMinutes(30));

		// when - 7자 비밀번호로 재설정 시도
		ResponseEntity<ApiResult<Void>> response =
			post("/password/reset", new ResetPasswordRequest(email, "short12"));

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("C001");
	}

	@Test
	@DisplayName("토큰 없이도 비밀번호 재설정 엔드포인트에 접근할 수 있어 401이 발생하지 않는다")
	void endpoints_arePublic() {
		// given - 미가입 이메일(비즈니스 결과는 404이지만 인증 단계는 통과해야 한다)
		String email = "reset-public@bookpool.kr";

		// when - 인증 헤더 없이 호출한다
		ResponseEntity<ApiResult<Void>> response = post("/password/email/code", new EmailCodeRequest(email));

		// then - 보안 필터에 막혀 401이 나면 안 된다
		assertThat(response.getStatusCode()).isNotEqualTo(UNAUTHORIZED);
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	private void createMember(String email, String rawPassword) {
		memberRepository.save(
			Member.create(email, "북풀러", passwordEncoder.encode(rawPassword), true)
		);
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

	private ResponseEntity<ApiResult<LoginResponse>> login(LoginRequest request) {
		return restClient.post()
			.uri("/login")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}
}
