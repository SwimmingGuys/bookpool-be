package kr.co.bookpool.app.campaign.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.campaign.dto.request.CampaignCreateRequest;
import kr.co.bookpool.app.campaign.dto.request.CampaignUpdateRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.FieldError;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCampaignApiTest {

	private static final String ADMIN_EMAIL = "admin@bookpool.kr";
	private static final String ADMIN_PASSWORD = "admin1234";
	private static final String USER_EMAIL = "user@bookpool.kr";
	private static final String USER_PASSWORD = "user1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CampaignRepository campaignRepository;

	@Autowired
	private EmailVerificationStore emailVerificationStore;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private RestClient restClient;
	private String adminToken;
	private String userToken;

	@BeforeEach
	void setUp() {
		campaignRepository.deleteAll();
		memberRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			// 4xx/5xx도 예외 없이 ResponseEntity로 받기 위해 기본 에러 처리를 no-op으로 대체
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		// 관리자 계정을 직접 저장하고 로그인해 ROLE_ADMIN 토큰을 확보한다
		memberRepository.save(
			Member.createAdmin(ADMIN_EMAIL, "관리자", passwordEncoder.encode(ADMIN_PASSWORD)));
		adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

		// 일반 사용자 계정을 저장하고 로그인해 ROLE_USER 토큰을 확보한다
		memberRepository.save(
			Member.create(USER_EMAIL, "사용자", passwordEncoder.encode(USER_PASSWORD), true));
		userToken = login(USER_EMAIL, USER_PASSWORD);
	}

	@Test
	@DisplayName("관리자가 캠페인을 등록하면 201과 저장된 내용이 반환되고 목록/단건 조회에 반영된다")
	void create_success() {
		// given
		CampaignCreateRequest request = sampleCreateRequest("스프링 부트 리뷰어 모집", CampaignStatus.OPEN);

		// when - 등록
		ResponseEntity<ApiResult<CampaignResponse>> created = restClient.post()
			.uri("/admin/campaigns")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then - 201과 저장된 내용 확인
		assertThat(created.getStatusCode()).isEqualTo(CREATED);
		assertThat(created.getBody()).isNotNull();
		assertThat(created.getBody().success()).isTrue();
		assertThat(created.getBody().data().title()).isEqualTo("스프링 부트 리뷰어 모집");
		assertThat(created.getBody().data().bookTitle()).isEqualTo("스프링 인 액션");
		assertThat(created.getBody().data().category()).isEqualTo("IT/개발");
		assertThat(created.getBody().data().status()).isEqualTo("open");
		assertThat(created.getBody().data().viewCount()).isZero();

		String id = created.getBody().data().id();
		assertThat(campaignRepository.existsById(Long.valueOf(id))).isTrue();

		// then - 단건 조회에 반영
		ResponseEntity<ApiResult<CampaignResponse>> detail = restClient.get()
			.uri("/admin/campaigns/" + id)
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(detail.getStatusCode()).isEqualTo(OK);
		assertThat(detail.getBody().data().title()).isEqualTo("스프링 부트 리뷰어 모집");

		// then - 목록 조회에 반영
		ResponseEntity<ApiResult<PageResponse<CampaignResponse>>> list = restClient.get()
			.uri("/admin/campaigns?page=0&size=20")
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(list.getStatusCode()).isEqualTo(OK);
		assertThat(list.getBody().data().totalElements()).isEqualTo(1);
		assertThat(list.getBody().data().content())
			.extracting(CampaignResponse::id)
			.containsExactly(id);
	}

	@Test
	@DisplayName("관리자가 캠페인을 수정하면 변경 내용이 반영되고 식별자/조회수는 보존된다")
	void update_success() {
		// given - 캠페인 등록
		CampaignResponse origin = create(sampleCreateRequest("수정 전 제목", CampaignStatus.OPEN));
		String id = origin.id();
		CampaignUpdateRequest request = new CampaignUpdateRequest(
			"수정 후 제목", "수정된 책", "수정된 출판사", CampaignCategory.NOVEL, CampaignType.BETA_READER,
			"https://apply.example.com/edit", null, "수정된 설명",
			LocalDate.now(), LocalDateTime.now().plusDays(10), LocalDate.now().plusDays(20),
			CampaignStatus.CLOSED);

		// when
		ResponseEntity<ApiResult<CampaignResponse>> response = restClient.put()
			.uri("/admin/campaigns/" + id)
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then - 변경 내용 반영, 식별자/조회수 보존
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody().success()).isTrue();
		assertThat(response.getBody().data().id()).isEqualTo(id);
		assertThat(response.getBody().data().title()).isEqualTo("수정 후 제목");
		assertThat(response.getBody().data().category()).isEqualTo("소설");
		assertThat(response.getBody().data().status()).isEqualTo("closed");
		assertThat(response.getBody().data().viewCount()).isEqualTo(origin.viewCount());
	}

	@Test
	@DisplayName("관리자가 캠페인을 삭제하면 이후 단건 조회는 404 CP001을 반환한다")
	void delete_success() {
		// given
		String id = create(sampleCreateRequest("삭제될 캠페인", CampaignStatus.OPEN)).id();

		// when - 삭제
		ResponseEntity<ApiResult<Void>> deleted = restClient.delete()
			.uri("/admin/campaigns/" + id)
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then - 200, 그리고 단건 조회 시 404 CP001
		assertThat(deleted.getStatusCode()).isEqualTo(OK);
		assertThat(deleted.getBody().success()).isTrue();

		ResponseEntity<ApiResult<Void>> detail = restClient.get()
			.uri("/admin/campaigns/" + id)
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(detail.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(detail.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("존재하지 않는 id 단건 조회 시 404 CP001을 반환한다")
	void detail_notFound() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/admin/campaigns/999999")
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("존재하지 않는 id 수정 시 404 CP001을 반환한다")
	void update_notFound() {
		// given
		CampaignUpdateRequest request = new CampaignUpdateRequest(
			"제목", "책", "출판사", CampaignCategory.IT, CampaignType.REVIEWER,
			"https://apply.example.com", null, null,
			null, LocalDateTime.now().plusDays(7), null, CampaignStatus.OPEN);

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.put()
			.uri("/admin/campaigns/999999")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("존재하지 않는 id 삭제 시 404 CP001을 반환한다")
	void delete_notFound() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.delete()
			.uri("/admin/campaigns/999999")
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("필수 필드(title 공백, deadlineAt null) 누락 시 400 C001과 필드 에러 목록을 반환한다")
	void create_validationFail() {
		// given - title 공백, deadlineAt null
		CampaignCreateRequest request = new CampaignCreateRequest(
			" ", "책", "출판사", CampaignCategory.IT, CampaignType.REVIEWER,
			"https://apply.example.com", null, null, null, null, null, CampaignStatus.OPEN);

		// when
		ResponseEntity<ApiResult<List<FieldError>>> response = restClient.post()
			.uri("/admin/campaigns")
			.header("Authorization", "Bearer " + adminToken)
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
			.contains("title", "deadlineAt");
	}

	@Test
	@DisplayName("USER 토큰으로 등록을 시도하면 403 A004를 반환한다")
	void create_forbiddenForUser() {
		// given
		CampaignCreateRequest request = sampleCreateRequest("권한 없는 등록", CampaignStatus.OPEN);

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/admin/campaigns")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A004");
	}

	@Test
	@DisplayName("USER 토큰으로 목록 조회를 시도하면 403 A004를 반환한다")
	void list_forbiddenForUser() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/admin/campaigns")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("A004");
	}

	@Test
	@DisplayName("USER 토큰으로 수정을 시도하면 403 A004를 반환한다")
	void update_forbiddenForUser() {
		// given
		String id = create(sampleCreateRequest("기존 캠페인", CampaignStatus.OPEN)).id();
		CampaignUpdateRequest request = new CampaignUpdateRequest(
			"권한 없는 수정", "책", "출판사", CampaignCategory.IT, CampaignType.REVIEWER,
			"https://apply.example.com", null, null,
			null, LocalDateTime.now().plusDays(7), null, CampaignStatus.OPEN);

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.put()
			.uri("/admin/campaigns/" + id)
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("A004");
	}

	@Test
	@DisplayName("USER 토큰으로 삭제를 시도하면 403 A004를 반환한다")
	void delete_forbiddenForUser() {
		// given
		String id = create(sampleCreateRequest("기존 캠페인", CampaignStatus.OPEN)).id();

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.delete()
			.uri("/admin/campaigns/" + id)
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("A004");
	}

	@Test
	@DisplayName("토큰 없이 목록 조회를 시도하면 401 A002를 반환한다")
	void list_unauthorizedWithoutToken() {
		// when
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/admin/campaigns")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	@Test
	@DisplayName("토큰 없이 등록을 시도하면 401 A002를 반환한다")
	void create_unauthorizedWithoutToken() {
		// given
		CampaignCreateRequest request = sampleCreateRequest("토큰 없는 등록", CampaignStatus.OPEN);

		// when
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/admin/campaigns")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then
		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	@Test
	@DisplayName("목록은 CLOSED를 포함한 모든 상태의 캠페인을 최신순으로 반환한다")
	void list_includesAllStatuses() {
		// given - OPEN/CLOSED/UPCOMING 캠페인을 각각 등록
		create(sampleCreateRequest("열린 캠페인", CampaignStatus.OPEN));
		create(sampleCreateRequest("마감 캠페인", CampaignStatus.CLOSED));
		create(sampleCreateRequest("예정 캠페인", CampaignStatus.UPCOMING));

		// when
		ResponseEntity<ApiResult<PageResponse<CampaignResponse>>> response = restClient.get()
			.uri("/admin/campaigns?page=0&size=20")
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		// then - 상태와 무관하게 3건 모두 노출, 최신 등록순(나중에 등록한 것이 먼저)
		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody().data().totalElements()).isEqualTo(3);
		assertThat(response.getBody().data().content())
			.extracting(CampaignResponse::title)
			.containsExactly("예정 캠페인", "마감 캠페인", "열린 캠페인");
	}

	private CampaignCreateRequest sampleCreateRequest(String title, CampaignStatus status) {
		return new CampaignCreateRequest(
			title, "스프링 인 액션", "출판사", CampaignCategory.IT, CampaignType.REVIEWER,
			"https://apply.example.com", "https://image.example.com/cover.png", "캠페인 설명입니다.",
			LocalDate.now(), LocalDateTime.now().plusDays(7), LocalDate.now().plusDays(14), status);
	}

	private CampaignResponse create(CampaignCreateRequest request) {
		return restClient.post()
			.uri("/admin/campaigns")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<CampaignResponse>>() {
			})
			.getBody()
			.data();
	}

	private String login(String email, String password) {
		ResponseEntity<ApiResult<LoginResponse>> response = restClient.post()
			.uri("/login")
			.contentType(APPLICATION_JSON)
			.body(new LoginRequest(email, password))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		return response.getBody().data().accessToken();
	}
}
