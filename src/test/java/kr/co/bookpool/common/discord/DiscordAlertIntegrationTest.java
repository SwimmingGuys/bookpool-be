package kr.co.bookpool.common.discord;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;

/**
 * Discord 에러 알림 통합 동작 검증.
 *
 * <p>{@code discord.alert.enabled=true}로 알림을 켜고, 테스트 기본 {@link LogDiscordSender} 대신
 * 녹화용 {@link DiscordSender}를 {@link Primary} 빈으로 주입한다.
 * 실제 4xx 에러 응답이 발생하면 알림이 트리거되는지 확인한다.
 */
@ActiveProfiles("test")
@TestPropertySource(properties = "discord.alert.enabled=true")
@Import(DiscordAlertIntegrationTest.RecordingSenderConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscordAlertIntegrationTest {

	@TestConfiguration
	static class RecordingSenderConfig {
		@Bean
		@Primary
		RecordingDiscordSender recordingDiscordSender() {
			return new RecordingDiscordSender();
		}
	}

	/** 발송된 메시지를 저장하는 녹화용 Sender (테스트 기본 LogDiscordSender 대체). */
	static class RecordingDiscordSender implements DiscordSender {
		final List<DiscordMessageRequest> sent = Collections.synchronizedList(new ArrayList<>());

		@Override
		public void send(DiscordMessageRequest message) {
			sent.add(message);
		}
	}

	@LocalServerPort
	private int port;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EmailVerificationStore emailVerificationStore;

	@Autowired
	private RecordingDiscordSender recordingDiscordSender;

	private RestClient restClient;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		recordingDiscordSender.sent.clear();
		restClient = RestClient.builder()
			.baseUrl("http://localhost:" + port + "/api")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();
	}

	@Test
	@DisplayName("잘못된 본문으로 회원가입 시 400 응답과 함께 Discord 알림이 발송된다")
	void validationError_triggersAlert() {
		// given - 잘못된 이메일 + 짧은 비밀번호 → 검증 실패(C001)
		SignUpRequest request = new SignUpRequest("not-an-email", "닉네임", "short", true);

		// when
		restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();

		// then - 알림이 1건 발송되고 본문에 400/C001이 담긴다
		assertThat(recordingDiscordSender.sent).hasSize(1);
		assertThat(recordingDiscordSender.sent.get(0).content())
			.contains("400")
			.contains("C001");
	}

	@Test
	@DisplayName("중복 이메일로 회원가입 시 409 응답과 함께 Discord 알림이 발송된다")
	void duplicateEmail_triggersAlert() {
		// given - 최초 가입 완료 후 동일 이메일 재요청
		emailVerificationStore.markVerified("dup@bookpool.kr", Duration.ofMinutes(30));
		SignUpRequest request = new SignUpRequest("dup@bookpool.kr", "닉네임", "password1234", true);
		restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();
		recordingDiscordSender.sent.clear(); // 최초 가입은 성공이라 알림 없음 — 명시적으로 비우고 시작

		// when - 중복 가입 시도
		restClient.post()
			.uri("/signup")
			.contentType(APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();

		// then - 409(M001) 알림이 발송된다
		assertThat(recordingDiscordSender.sent).hasSize(1);
		assertThat(recordingDiscordSender.sent.get(0).content())
			.contains("409")
			.contains("M001");
	}
}
