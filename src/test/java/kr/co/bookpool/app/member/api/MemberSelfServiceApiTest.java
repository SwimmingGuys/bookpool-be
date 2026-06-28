package kr.co.bookpool.app.member.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

import java.time.Duration;
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
import kr.co.bookpool.app.member.dto.request.ChangePasswordRequest;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.request.UpdateProfileRequest;
import kr.co.bookpool.app.member.dto.response.MeResponse;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberSelfServiceApiTest {

	private static final String EMAIL = "self@bookpool.kr";
	private static final String PASSWORD = "password1234";

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

		// 로그인 가능한 회원을 미리 가입시켜 둔다(이메일 인증은 가입 전제 조건).
		emailVerificationStore.markVerified(EMAIL, Duration.ofMinutes(30));
		signUp(new SignUpRequest(EMAIL, "북풀러", PASSWORD, true));
	}

	@Test
	@DisplayName("프로필을 수정하면 200과 변경된 닉네임/연락처를 반환하고 이후 조회에도 반영된다")
	void updateProfile_success() {
		// given
		String accessToken = obtainAccessToken();
		UpdateProfileRequest request = new UpdateProfileRequest("새북풀러", "010-1234-5678");

		// when
		ResponseEntity<ApiResult<MeResponse>> response = restClient.patch()
			.uri("/me")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().code()).isEqualTo("SUCCESS");
		assertThat(response.getBody().data().nickname()).isEqualTo("새북풀러");
		assertThat(response.getBody().data().contact()).isEqualTo("010-1234-5678");

		// then - 이후 GET /api/me 조회에도 반영
		MeResponse me = getMyInfo(accessToken).getBody().data();
		assertThat(me.nickname()).isEqualTo("새북풀러");
		assertThat(me.contact()).isEqualTo("010-1234-5678");
	}

	@Test
	@DisplayName("닉네임이 공백이면 400과 C001 필드 에러를 반환한다")
	void updateProfile_blankNickname() {
		// given
		String accessToken = obtainAccessToken();
		UpdateProfileRequest request = new UpdateProfileRequest("   ", "010-1234-5678");

		// when
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.patch()
			.uri("/me")
			.header("Authorization", "Bearer " + accessToken)
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
			.contains("nickname");
	}

	@Test
	@DisplayName("토큰 없이 프로필 수정을 호출하면 401과 A002를 반환한다")
	void updateProfile_withoutToken() {
		// given
		UpdateProfileRequest request = new UpdateProfileRequest("새북풀러", "010-1234-5678");

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me")
			.contentType(APPLICATION_JSON)
			.body(request)
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
	@DisplayName("비밀번호를 변경하면 200을 반환하고 새 비밀번호로는 로그인되며 기존 비밀번호로는 로그인이 실패한다")
	void changePassword_success() {
		// given
		String accessToken = obtainAccessToken();
		String newPassword = "newpassword5678";
		ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, newPassword);

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me/password")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().code()).isEqualTo("SUCCESS");

		// then - 새 비밀번호로는 로그인 성공
		ResponseEntity<ApiResult<LoginResponse>> loginWithNew = login(new LoginRequest(EMAIL, newPassword));
		assertThat(loginWithNew.getStatusCode()).isEqualTo(OK);
		assertThat(loginWithNew.getBody().data().accessToken()).isNotBlank();

		// then - 기존 비밀번호로는 로그인 실패(401 A001)
		ResponseEntity<ApiResult<LoginResponse>> loginWithOld = login(new LoginRequest(EMAIL, PASSWORD));
		assertThat(loginWithOld.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(loginWithOld.getBody().success()).isFalse();
		assertThat(loginWithOld.getBody().code()).isEqualTo("A001");
	}

	@Test
	@DisplayName("현재 비밀번호가 틀리면 400과 M005를 반환한다")
	void changePassword_currentPasswordMismatch() {
		// given
		String accessToken = obtainAccessToken();
		ChangePasswordRequest request = new ChangePasswordRequest("wrongpassword", "newpassword5678");

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me/password")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("M005");

		// then - 변경에 실패했으므로 기존 비밀번호로 여전히 로그인 가능
		assertThat(login(new LoginRequest(EMAIL, PASSWORD)).getStatusCode()).isEqualTo(OK);
	}

	@Test
	@DisplayName("새 비밀번호가 8자 미만이면 400과 C001 필드 에러를 반환한다")
	void changePassword_newPasswordTooShort() {
		// given
		String accessToken = obtainAccessToken();
		ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, "short");

		// when
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.patch()
			.uri("/me/password")
			.header("Authorization", "Bearer " + accessToken)
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
			.contains("newPassword");
	}

	@Test
	@DisplayName("토큰 없이 비밀번호 변경을 호출하면 401과 A002를 반환한다")
	void changePassword_withoutToken() {
		// given
		ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, "newpassword5678");

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me/password")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	private String obtainAccessToken() {
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

	private ResponseEntity<ApiResult<MeResponse>> getMyInfo(String accessToken) {
		return restClient.get()
			.uri("/me")
			.header("Authorization", "Bearer " + accessToken)
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
