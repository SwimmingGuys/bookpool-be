package kr.co.bookpool.app.auth.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

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

import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.response.MeResponse;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiTest {

	private static final String EMAIL = "login@bookpool.kr";
	private static final String PASSWORD = "password1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

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

		signUp(new SignUpRequest(EMAIL, "북풀러", PASSWORD, true));
	}

	@Test
	@DisplayName("올바른 이메일/비밀번호로 로그인하면 200과 Access 토큰을 반환한다")
	void login_success() {
		// given
		LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

		// when
		ResponseEntity<ApiResult<LoginResponse>> response = login(request);

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().code()).isEqualTo("SUCCESS");
		assertThat(response.getBody().data().accessToken()).isNotBlank();
		assertThat(response.getBody().data().tokenType()).isEqualTo("Bearer");
	}

	@Test
	@DisplayName("비밀번호가 틀리면 401과 A001을 반환한다")
	void login_wrongPassword() {
		// given
		LoginRequest request = new LoginRequest(EMAIL, "wrongpassword");

		// when
		ResponseEntity<ApiResult<Void>> response = loginError(request);

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A001");
	}

	@Test
	@DisplayName("존재하지 않는 이메일로 로그인하면 비밀번호 오류와 동일하게 401 A001을 반환한다")
	void login_nonexistentEmail() {
		// given
		LoginRequest request = new LoginRequest("nobody@bookpool.kr", PASSWORD);

		// when
		ResponseEntity<ApiResult<Void>> response = loginError(request);

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("A001");
	}

	@Test
	@DisplayName("입력값 검증에 실패하면 400과 필드 에러 목록을 반환한다")
	void login_validationFail() {
		// given - 잘못된 이메일 형식 + 빈 비밀번호
		LoginRequest request = new LoginRequest("not-an-email", "");

		// when
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.post()
			.uri("/login")
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
	@DisplayName("발급받은 토큰으로 /api/me를 호출하면 200과 내 정보를 반환한다")
	void me_withValidToken() {
		// given
		String accessToken = login(new LoginRequest(EMAIL, PASSWORD)).getBody().data().accessToken();

		// when
		ResponseEntity<ApiResult<MeResponse>> response = restClient.get()
			.uri("/me")
			.header("Authorization", "Bearer " + accessToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().data().email()).isEqualTo(EMAIL);
	}

	@Test
	@DisplayName("토큰 없이 /api/me를 호출하면 401과 A002를 반환한다")
	void me_withoutToken() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/me")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A002");
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

	private ResponseEntity<ApiResult<Void>> loginError(LoginRequest request) {
		return restClient.post()
			.uri("/login")
			.contentType(APPLICATION_JSON)
			.body(request)
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
