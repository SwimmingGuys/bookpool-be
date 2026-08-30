package kr.co.bookpool.app.notice.api;

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
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.notice.dto.request.NoticeRequest;
import kr.co.bookpool.app.notice.dto.response.NoticeResponse;
import kr.co.bookpool.app.notice.entity.NoticeCategory;
import kr.co.bookpool.app.notice.repository.NoticeRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminNoticeApiTest {

	private static final String ADMIN_EMAIL = "notice-admin@bookpool.kr";
	private static final String ADMIN_PASSWORD = "admin1234";
	private static final String USER_EMAIL = "notice-user@bookpool.kr";
	private static final String USER_PASSWORD = "user1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private NoticeRepository noticeRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private RestClient restClient;
	private String adminToken;
	private String userToken;

	@BeforeEach
	void setUp() {
		noticeRepository.deleteAll();
		memberRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		memberRepository.save(
			Member.createAdmin(ADMIN_EMAIL, "공지관리자", passwordEncoder.encode(ADMIN_PASSWORD)));
		adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

		memberRepository.save(
			Member.create(USER_EMAIL, "사용자", passwordEncoder.encode(USER_PASSWORD), false));
		userToken = login(USER_EMAIL, USER_PASSWORD);
	}

	@Test
	@DisplayName("관리자가 공지를 등록하면 작성자가 기록되고 공개 목록에 노출된다")
	void create_success() {
		NoticeRequest request = new NoticeRequest("점검 안내", "9월 1일 새벽 점검", NoticeCategory.MAINTENANCE, true);

		ResponseEntity<ApiResult<NoticeResponse>> created = restClient.post()
			.uri("/admin/notices")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(created.getStatusCode()).isEqualTo(CREATED);
		assertThat(created.getBody().data().title()).isEqualTo("점검 안내");
		assertThat(created.getBody().data().author()).isEqualTo("공지관리자");
		assertThat(created.getBody().data().isPinned()).isTrue();
		assertThat(created.getBody().data().category()).isEqualTo("maintenance");

		// 공개 목록에도 바로 보인다
		ResponseEntity<ApiResult<PageResponse<NoticeResponse>>> publicList = restClient.get()
			.uri("/notices")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(publicList.getBody().data().content())
			.extracting(NoticeResponse::title)
			.containsExactly("점검 안내");
	}

	@Test
	@DisplayName("관리자가 공지를 수정하면 내용이 바뀌고 작성자는 보존된다")
	void update_success() {
		String id = create("수정 전", NoticeCategory.GENERAL, false).id();
		NoticeRequest request = new NoticeRequest("수정 후", "바뀐 내용", NoticeCategory.EVENT, true);

		ResponseEntity<ApiResult<NoticeResponse>> response = restClient.put()
			.uri("/admin/notices/" + id)
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(response.getBody().data().id()).isEqualTo(id);
		assertThat(response.getBody().data().title()).isEqualTo("수정 후");
		assertThat(response.getBody().data().category()).isEqualTo("event");
		assertThat(response.getBody().data().author()).isEqualTo("공지관리자");
	}

	@Test
	@DisplayName("관리자가 공지를 삭제하면 공개 목록에서도 사라진다")
	void delete_success() {
		String id = create("삭제할 공지", NoticeCategory.GENERAL, false).id();

		ResponseEntity<ApiResult<Void>> response = restClient.delete()
			.uri("/admin/notices/" + id)
			.header("Authorization", "Bearer " + adminToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(noticeRepository.existsById(Long.valueOf(id))).isFalse();
	}

	@Test
	@DisplayName("존재하지 않는 공지 수정 시 404 N001을 반환한다")
	void update_notFound() {
		NoticeRequest request = new NoticeRequest("제목", "내용", NoticeCategory.GENERAL, false);

		ResponseEntity<ApiResult<Void>> response = restClient.put()
			.uri("/admin/notices/999999")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("N001");
	}

	@Test
	@DisplayName("USER 토큰으로 공지를 등록하면 403 A004를 반환한다")
	void create_forbiddenForUser() {
		NoticeRequest request = new NoticeRequest("권한 없는 공지", "내용", NoticeCategory.GENERAL, false);

		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/admin/notices")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
		assertThat(response.getBody().code()).isEqualTo("A004");
	}

	// ---------- helpers ----------

	private NoticeResponse create(String title, NoticeCategory category, boolean pinned) {
		return restClient.post()
			.uri("/admin/notices")
			.header("Authorization", "Bearer " + adminToken)
			.contentType(APPLICATION_JSON)
			.body(new NoticeRequest(title, "내용", category, pinned))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<NoticeResponse>>() {
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
