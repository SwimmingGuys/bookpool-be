package kr.co.bookpool.app.application.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;

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

import kr.co.bookpool.app.application.dto.request.ApplicationRequest;
import kr.co.bookpool.app.application.dto.request.ApplicationStatusRequest;
import kr.co.bookpool.app.application.dto.response.ApplicationResponse;
import kr.co.bookpool.app.application.entity.ApplicationStatus;
import kr.co.bookpool.app.application.repository.ApplicationRepository;
import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationApiTest {

	private static final String USER_EMAIL = "apply-user@bookpool.kr";
	private static final String USER_PASSWORD = "user1234";
	private static final String OTHER_EMAIL = "apply-other@bookpool.kr";
	private static final String OTHER_PASSWORD = "other1234";

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CampaignRepository campaignRepository;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private RestClient restClient;
	private String userToken;
	private String otherToken;
	private Long campaignId;
	private Long otherCampaignId;

	@BeforeEach
	void setUp() {
		applicationRepository.deleteAll();
		campaignRepository.deleteAll();
		memberRepository.deleteAll();

		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

		memberRepository.save(
			Member.create(USER_EMAIL, "신청자", passwordEncoder.encode(USER_PASSWORD), false));
		userToken = login(USER_EMAIL, USER_PASSWORD);

		memberRepository.save(
			Member.create(OTHER_EMAIL, "다른사람", passwordEncoder.encode(OTHER_PASSWORD), false));
		otherToken = login(OTHER_EMAIL, OTHER_PASSWORD);

		campaignId = saveCampaign("신청할 공고").getId();
		otherCampaignId = saveCampaign("다른 공고").getId();
	}

	@Test
	@DisplayName("신청 표시를 하면 APPLIED로 저장되고 공고 정보가 함께 내려온다")
	void apply_success() {
		ResponseEntity<ApiResult<ApplicationResponse>> response = restClient.post()
			.uri("/me/applications")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ApplicationRequest(campaignId, null))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(CREATED);
		ApplicationResponse application = response.getBody().data();
		assertThat(application.status()).isEqualTo(ApplicationStatus.APPLIED.name());
		assertThat(application.appliedAt()).isNotNull();
		// 마이페이지가 발표일·서평 마감을 바로 보여줄 수 있도록 공고를 통째로 담는다
		assertThat(application.campaign().title()).isEqualTo("신청할 공고");
		assertThat(application.campaign().announcementDate()).isNotNull();
	}

	@Test
	@DisplayName("신청한 공고 ID 목록으로 카드에 '신청함'을 표시할 수 있다")
	void appliedIds() {
		apply(campaignId);

		ResponseEntity<ApiResult<List<Long>>> response = restClient.get()
			.uri("/me/applications/ids")
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getBody().data()).containsExactly(campaignId);
	}

	@Test
	@DisplayName("같은 공고에 두 번 신청 표시하면 409 AP002를 반환한다")
	void apply_duplicate() {
		apply(campaignId);

		ResponseEntity<ApiResult<Void>> second = restClient.post()
			.uri("/me/applications")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ApplicationRequest(campaignId, null))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(second.getStatusCode()).isEqualTo(CONFLICT);
		assertThat(second.getBody().code()).isEqualTo("AP002");
	}

	@Test
	@DisplayName("발표 결과를 당첨으로 바꾸면 상태 필터로 걸러 볼 수 있다")
	void changeStatus_andFilter() {
		apply(campaignId);
		apply(otherCampaignId);

		ResponseEntity<ApiResult<ApplicationResponse>> changed = restClient.patch()
			.uri("/me/applications/" + campaignId)
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ApplicationStatusRequest(ApplicationStatus.SELECTED))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(changed.getStatusCode()).isEqualTo(OK);
		assertThat(changed.getBody().data().status()).isEqualTo(ApplicationStatus.SELECTED.name());

		assertThat(list("?status=SELECTED").content())
			.extracting(a -> a.campaign().title())
			.containsExactly("신청할 공고");
		assertThat(list("?status=APPLIED").content())
			.extracting(a -> a.campaign().title())
			.containsExactly("다른 공고");
		assertThat(list("").content()).hasSize(2);
	}

	@Test
	@DisplayName("한국어 라벨로도 상태를 바꿀 수 있다")
	void changeStatus_koreanLabel() {
		apply(campaignId);

		ResponseEntity<ApiResult<ApplicationResponse>> changed = restClient.patch()
			.uri("/me/applications/" + campaignId)
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body("{\"status\":\"미당첨\"}")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(changed.getStatusCode()).isEqualTo(OK);
		assertThat(changed.getBody().data().status()).isEqualTo(ApplicationStatus.NOT_SELECTED.name());
	}

	@Test
	@DisplayName("신청 표시를 해제하면 목록에서 사라진다")
	void cancel() {
		apply(campaignId);

		ResponseEntity<ApiResult<Void>> response = restClient.delete()
			.uri("/me/applications/" + campaignId)
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(OK);
		assertThat(list("").content()).isEmpty();
	}

	@Test
	@DisplayName("신청하지 않은 공고의 상태를 바꾸려 하면 404 AP001을 반환한다")
	void changeStatus_notFound() {
		ResponseEntity<ApiResult<Void>> response = restClient.patch()
			.uri("/me/applications/" + campaignId)
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ApplicationStatusRequest(ApplicationStatus.SELECTED))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("AP001");
	}

	@Test
	@DisplayName("남의 신청 기록은 보이지도, 바꿀 수도 없다")
	void isolatedPerMember() {
		apply(campaignId);

		// 다른 사용자의 목록에는 없다
		ResponseEntity<ApiResult<PageResponse<ApplicationResponse>>> otherList = restClient.get()
			.uri("/me/applications")
			.header("Authorization", "Bearer " + otherToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(otherList.getBody().data().content()).isEmpty();

		// 다른 사용자가 상태를 바꾸려 하면 자기 기록이 없으므로 404
		ResponseEntity<ApiResult<Void>> changed = restClient.patch()
			.uri("/me/applications/" + campaignId)
			.header("Authorization", "Bearer " + otherToken)
			.contentType(APPLICATION_JSON)
			.body(new ApplicationStatusRequest(ApplicationStatus.SELECTED))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		assertThat(changed.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	@Test
	@DisplayName("존재하지 않는 공고에 신청 표시하면 404 CP001을 반환한다")
	void apply_campaignNotFound() {
		ResponseEntity<ApiResult<Void>> response = restClient.post()
			.uri("/me/applications")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ApplicationRequest(999999L, null))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("CP001");
	}

	@Test
	@DisplayName("비로그인 상태로 신청 목록을 조회하면 401 A002를 반환한다")
	void list_unauthorized() {
		ResponseEntity<ApiResult<Void>> response = restClient.get()
			.uri("/me/applications")
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});

		assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
		assertThat(response.getBody().code()).isEqualTo("A002");
	}

	// ---------- helpers ----------

	private void apply(Long id) {
		restClient.post()
			.uri("/me/applications")
			.header("Authorization", "Bearer " + userToken)
			.contentType(APPLICATION_JSON)
			.body(new ApplicationRequest(id, null))
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<ApplicationResponse>>() {
			});
	}

	private PageResponse<ApplicationResponse> list(String queryString) {
		return restClient.get()
			.uri("/me/applications" + queryString)
			.header("Authorization", "Bearer " + userToken)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<ApiResult<PageResponse<ApplicationResponse>>>() {
			})
			.getBody()
			.data();
	}

	private Campaign saveCampaign(String title) {
		return campaignRepository.save(Campaign.builder()
			.title(title)
			.bookTitle(title + " 도서")
			.publisherName("테스트 출판사")
			.category(CampaignCategory.IT)
			.type(CampaignType.REVIEWER)
			.applyUrl("https://apply.example.com")
			.deadlineAt(LocalDateTime.now().plusDays(7))
			.announcementDate(LocalDate.now().plusDays(14))
			.reviewDueDate(LocalDate.now().plusDays(30))
			.status(CampaignStatus.OPEN)
			.build());
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
