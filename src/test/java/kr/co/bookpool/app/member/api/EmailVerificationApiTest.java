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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmailVerificationApiTest {

	private static final String EMAIL = "verify@bookpool.kr";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EmailVerificationStore verificationStore;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
	}

	@Test
	@DisplayName("인증 코드를 발송하면 200을 반환하고 코드가 저장된다")
	void sendCode_success() {
		// when
		ResponseEntity<ApiResult<Void>> response = post("/signup/email/code", new EmailCodeRequest(EMAIL));

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(verificationStore.findCode(EMAIL)).isPresent();
	}

	@Test
	@DisplayName("올바른 코드로 검증하면 200을 반환하고 인증 완료 상태가 된다")
	void verify_success() {
		// given - 코드 발송 후 저장된 코드를 읽어온다
		post("/signup/email/code", new EmailCodeRequest(EMAIL));
		String code = verificationStore.findCode(EMAIL).orElseThrow();

		// when
		ResponseEntity<ApiResult<Void>> response = post("/signup/email/verify", new EmailVerifyRequest(EMAIL, code));

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(verificationStore.isVerified(EMAIL)).isTrue();
	}

	@Test
	@DisplayName("틀린 코드로 검증하면 400과 M004를 반환한다")
	void verify_wrongCode() {
		// given
		post("/signup/email/code", new EmailCodeRequest(EMAIL));

		// when
		ResponseEntity<ApiResult<Void>> response = post("/signup/email/verify", new EmailVerifyRequest(EMAIL, "000000"));

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("M004");
		assertThat(verificationStore.isVerified(EMAIL)).isFalse();
	}

	@Test
	@DisplayName("이메일 인증 없이 회원가입하면 400과 M003을 반환한다")
	void signUp_withoutVerification() {
		// when
		ResponseEntity<ApiResult<Void>> response =
			post("/signup", new SignUpRequest(EMAIL, "북풀러", "password1234", true));

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("M003");
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
}
