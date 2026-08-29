package kr.co.bookpool.app.inquiry.api;

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
import kr.co.bookpool.app.inquiry.dto.request.AnswerInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.request.CreateInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.app.inquiry.entity.InquiryType;
import kr.co.bookpool.app.inquiry.repository.InquiryRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminInquiryApiTest {

	private static final String ADMIN_EMAIL = "inquiry-admin@bookpool.kr";
	private static final String ADMIN_PASSWORD = "admin1234";
	private static final String USER_EMAIL = "inquiry-user@bookpool.kr";
	private static final String USER_PASSWORD = "user1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private InquiryRepository inquiryRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private RestClient restClient;
	private String adminToken;
	private String userToken;

	@BeforeEach
	void setUp() {
		inquiryRepository.deleteAll();
		memberRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		memberRepository.save(
			Member.createAdmin(ADMIN_EMAIL, "문의관리자", passwordEncoder.encode(ADMIN_PASSWORD)));
		adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

		memberRepository.save(
			Member.create(USER_EMAIL, "문의사용자", passwordEncoder.encode(USER_PASSWORD), false));
		userToken = login(USER_EMAIL, USER_PASSWORD);
	}

	@Test
	@DisplayName("관리자 목록에는 작성자 정보가 함께 내려온다")
	void list_includesAuthor() {
		submitInquiry("결제가 안 돼요");

		ResponseEntity<ApiResult<PageResponse<InquiryResponse>>> response = restClient.get()
			.uri("/admin/inquiries")
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(OK);
		InquiryResponse found = response.getBody().data().content().get(0);
		assertThat(found.title()).isEqualTo("결제가 안 돼요");
		assertThat(found.authorEmail()).isEqualTo(USER_EMAIL);
		assertThat(found.authorNickname()).isEqualTo("문의사용자");
	}

	@Test
	@DisplayName("사용자 목록에는 작성자 정보가 담기지 않는다")
	void listMine_hidesAuthor() {
		submitInquiry("내 문의");

		ResponseEntity<ApiResult<PageResponse<InquiryResponse>>> response = restClient.get()
			.uri("/inquiries")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		InquiryResponse found = response.getBody().data().content().get(0);
		assertThat(found.authorEmail()).isNull();
		assertThat(found.authorNickname()).isNull();
	}

	@Test
	@DisplayName("관리자가 답변하면 상태가 answered로 바뀌고 사용자 조회에도 반영된다")
	void answer_success() {
		String id = submitInquiry("문의합니다").id();

		ResponseEntity<ApiResult<InquiryResponse>> answered = restClient.post()
			.uri("/admin/inquiries/" + id + "/answer")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new AnswerInquiryRequest("확인 후 처리했습니다."))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(answered.getStatusCode()).isEqualTo(OK);
		assertThat(answered.getBody().data().status()).isEqualTo("answered");
		assertThat(answered.getBody().data().answer()).isEqualTo("확인 후 처리했습니다.");
		assertThat(answered.getBody().data().answeredAt()).isNotNull();

		// 문의를 남긴 사용자도 답변을 볼 수 있다
		ResponseEntity<ApiResult<InquiryResponse>> mine = restClient.get()
			.uri("/inquiries/" + id)
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(mine.getBody().data().answer()).isEqualTo("확인 후 처리했습니다.");
	}

	@Test
	@DisplayName("status=PENDING으로 미답변 문의만 조회한다")
	void list_filterByStatus() {
		String answeredId = submitInquiry("답변할 문의").id();
		submitInquiry("대기 중인 문의");

		restClient.post()
			.uri("/admin/inquiries/" + answeredId + "/answer")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new AnswerInquiryRequest("답변"))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<InquiryResponse>>() {
			});

		ResponseEntity<ApiResult<PageResponse<InquiryResponse>>> response = restClient.get()
			.uri("/admin/inquiries?status=PENDING")
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getBody().data().content())
			.extracting(InquiryResponse::title)
			.containsExactly("대기 중인 문의");
	}

	@Test
	@DisplayName("USER 토큰으로 관리자 문의 목록을 조회하면 403 A004를 반환한다")
	void list_forbiddenForUser() {
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/admin/inquiries")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("A004");
	}

	@Test
	@DisplayName("존재하지 않는 문의에 답변하면 404 I001을 반환한다")
	void answer_notFound() {
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/admin/inquiries/999999/answer")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new AnswerInquiryRequest("답변"))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("I001");
	}

	// ---------- helpers ----------

	private InquiryResponse submitInquiry(String title) {
		return restClient.post()
			.uri("/inquiries")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new CreateInquiryRequest(InquiryType.INQUIRY, title, "문의 내용입니다."))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<InquiryResponse>>() {
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
