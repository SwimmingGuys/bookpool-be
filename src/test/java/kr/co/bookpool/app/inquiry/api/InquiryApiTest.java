package kr.co.bookpool.app.inquiry.api;

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
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.app.inquiry.repository.InquiryRepository;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;
import kr.co.bookpool.common.response.PageResponse;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InquiryApiTest {

	private static final String EMAIL_A = "memberA@bookpool.kr";
	private static final String EMAIL_B = "memberB@bookpool.kr";
	private static final String PASSWORD = "password1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private InquiryRepository inquiryRepository;

	@Autowired
	private EmailVerificationStore emailVerificationStore;

	private RestClient restClient;

	private String tokenA;
	private String tokenB;

	@BeforeEach
	void setUp() {
		inquiryRepository.deleteAll();
		memberRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			// 4xx/5xx도 예외 없이 ResponseEntity로 받기 위해 기본 에러 처리를 no-op으로 대체
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		// 두 명의 회원(A/B)을 가입시키고 각각의 Access 토큰을 확보한다
		signUpVerified(EMAIL_A, "회원A");
		signUpVerified(EMAIL_B, "회원B");
		tokenA = login(EMAIL_A);
		tokenB = login(EMAIL_B);
	}

	@Test
	@DisplayName("문의를 등록하면 201과 PENDING 상태의 문의 정보를 반환한다")
	void create_success() {
		// given
		Map<String, Object> request = inquiryBody("inquiry", "배송 문의", "언제 배송되나요?");

		// when
		ResponseEntity<ApiResult<InquiryResponse>> response = create(tokenA, request);

		// then
		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().code()).isEqualTo("SUCCESS");

		InquiryResponse data = response.getBody().data();
		assertThat(data.id()).isNotBlank();
		assertThat(data.type()).isEqualTo("inquiry");
		assertThat(data.title()).isEqualTo("배송 문의");
		assertThat(data.content()).isEqualTo("언제 배송되나요?");
		assertThat(data.status()).isEqualTo("pending");
		assertThat(data.answer()).isNull();
		assertThat(data.answeredAt()).isNull();
		assertThat(data.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("요청(request) 유형으로 등록하면 type이 request로 반환된다")
	void create_requestType() {
		// given
		Map<String, Object> request = inquiryBody("request", "기능 요청", "다크 모드를 추가해주세요.");

		// when
		ResponseEntity<ApiResult<InquiryResponse>> response = create(tokenA, request);

		// then
		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		assertThat(response.getBody().data().type()).isEqualTo("request");
	}

	@Test
	@DisplayName("한글 유형 \"문의\"로 등록해도 inquiry로 정상 등록된다")
	void create_koreanType() {
		// given
		Map<String, Object> request = inquiryBody("문의", "한글 유형", "한글 유형도 허용되어야 한다.");

		// when
		ResponseEntity<ApiResult<InquiryResponse>> response = create(tokenA, request);

		// then
		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		assertThat(response.getBody().data().type()).isEqualTo("inquiry");
	}

	@Test
	@DisplayName("등록한 문의는 단건 조회에서 그대로 반영된다")
	void detail_afterCreate() {
		// given
		String id = create(tokenA, inquiryBody("inquiry", "제목", "내용")).getBody().data().id();

		// when
		ResponseEntity<ApiResult<InquiryResponse>> response = restClient.get()
			.uri("/inquiries/{id}", id)
			.header("Authorization", "Bearer " + tokenA)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().data().id()).isEqualTo(id);
		assertThat(response.getBody().data().title()).isEqualTo("제목");
		assertThat(response.getBody().data().status()).isEqualTo("pending");
	}

	@Test
	@DisplayName("내 문의 목록은 본인이 등록한 문의만 최신순으로 반환한다")
	void listMine_onlyOwnAndLatestFirst() {
		// given - A가 2건, B가 1건 등록
		create(tokenA, inquiryBody("inquiry", "A-첫번째", "내용1"));
		create(tokenA, inquiryBody("request", "A-두번째", "내용2"));
		create(tokenB, inquiryBody("inquiry", "B-문의", "내용B"));

		// when - A의 목록을 조회
		ResponseEntity<ApiResult<PageResponse<InquiryResponse>>> response = restClient.get()
			.uri("/inquiries?page=0&size=20")
			.header("Authorization", "Bearer " + tokenA)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then - A의 2건만, 최신순(나중에 등록한 A-두번째가 먼저)
		assertThat(response.getStatusCode()).isEqualTo(OK);
		PageResponse<InquiryResponse> page = response.getBody().data();
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.content())
			.extracting(InquiryResponse::title)
			.containsExactly("A-두번째", "A-첫번째");
	}

	@Test
	@DisplayName("다른 회원의 문의를 단건 조회하면 404와 I001을 반환한다")
	void detail_otherMembersInquiry() {
		// given - B가 등록한 문의
		String otherId = create(tokenB, inquiryBody("inquiry", "B의 문의", "내용")).getBody().data().id();

		// when - A가 B의 문의 id로 조회
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/inquiries/{id}", otherId)
			.header("Authorization", "Bearer " + tokenA)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("I001");
	}

	@Test
	@DisplayName("존재하지 않는 문의 id로 조회하면 404와 I001을 반환한다")
	void detail_notFound() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/inquiries/{id}", 999999L)
			.header("Authorization", "Bearer " + tokenA)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("I001");
	}

	@Test
	@DisplayName("제목이 공백이면 400과 C001을 반환한다")
	void create_blankTitle() {
		// given
		Map<String, Object> request = inquiryBody("inquiry", "   ", "내용");

		// when
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.post()
			.uri("/inquiries")
			.header("Authorization", "Bearer " + tokenA)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("C001");
		assertThat(response.getBody().data())
			.extracting(FieldError::field)
			.contains("title");
	}

	@Test
	@DisplayName("내용이 공백이면 400과 C001을 반환한다")
	void create_blankContent() {
		// given
		Map<String, Object> request = inquiryBody("inquiry", "제목", "   ");

		// when
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.post()
			.uri("/inquiries")
			.header("Authorization", "Bearer " + tokenA)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("C001");
		assertThat(response.getBody().data())
			.extracting(FieldError::field)
			.contains("content");
	}

	@Test
	@DisplayName("유형(type)이 누락되면 400을 반환한다")
	void create_missingType() {
		// given - type 키가 없는 본문
		Map<String, Object> request = new HashMap<>();
		request.put("title", "제목");
		request.put("content", "내용");

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/inquiries")
			.header("Authorization", "Bearer " + tokenA)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(response.getBody().success()).isFalse();
	}

	@Test
	@DisplayName("토큰 없이 문의를 등록하면 401과 A002를 반환한다")
	void create_withoutToken() {
		// given
		Map<String, Object> request = inquiryBody("inquiry", "제목", "내용");

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/inquiries")
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
	@DisplayName("토큰 없이 문의 목록을 조회하면 401과 A002를 반환한다")
	void listMine_withoutToken() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/inquiries")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	private Map<String, Object> inquiryBody(String type, String title, String content) {
		Map<String, Object> body = new HashMap<>();
		body.put("type", type);
		body.put("title", title);
		body.put("content", content);
		return body;
	}

	private ResponseEntity<ApiResult<InquiryResponse>> create(String token, Map<String, Object> body) {
		return restClient.post()
			.uri("/inquiries")
			.header("Authorization", "Bearer " + token)
			.contentType(APPLICATION_JSON)
			.body(body)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	private void signUpVerified(String email, String nickname) {
		emailVerificationStore.markVerified(email, Duration.ofMinutes(30));
		restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(new SignUpRequest(email, nickname, PASSWORD, true))
			.retrieve()
			.toBodilessEntity();
	}

	private String login(String email) {
		ResponseEntity<ApiResult<LoginResponse>> response = restClient.post()
			.uri("/login")
			.contentType(APPLICATION_JSON)
			.body(new LoginRequest(email, PASSWORD))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		return response.getBody().data().accessToken();
	}
}
