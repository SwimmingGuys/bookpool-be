package kr.co.bookpool.app.member.api;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.response.SignUpResponse;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberApiTest {

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
	}

	@Test
	@DisplayName("회원가입에 성공하면 201과 가입 정보를 반환한다")
	void signUp_success() {
		// given
		SignUpRequest request = new SignUpRequest("test@bookpool.kr", "북풀러", "password1234", true);

		// when
		ResponseEntity<ApiResult<SignUpResponse>> response = restClient.post()
			.uri("/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
		SignUpRequest request = new SignUpRequest("dup@bookpool.kr", "닉네임", "password1234", true);
		signUp(request); // 최초 가입

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
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
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("C001");
		assertThat(response.getBody().data())
			.extracting(FieldError::field)
			.contains("email", "password");
	}

	private void signUp(SignUpRequest request) {
		restClient.post()
			.uri("/signup")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();
	}
}
