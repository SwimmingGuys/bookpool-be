package kr.co.bookpool.common.discord;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link DiscordAlertNotifier} 단위 테스트.
 *
 * <p>Spring 컨텍스트 없이 녹화용 가짜 {@link DiscordSender}와 직접 만든 {@link DiscordProperties}로
 * 임계값(min-status)/스위치(enabled)/실패 안전(예외 미전파) 동작을 검증한다.
 */
class DiscordAlertNotifierTest {

	/** 발송된 메시지를 그대로 저장하는 녹화용 Sender. */
	static class RecordingDiscordSender implements DiscordSender {
		final List<DiscordMessageRequest> sent = new ArrayList<>();

		@Override
		public void send(DiscordMessageRequest message) {
			sent.add(message);
		}
	}

	private static DiscordProperties properties(boolean enabled, int minStatus) {
		return new DiscordProperties(
			"https://discord.test/webhook",
			"Bookpool Alert",
			new DiscordProperties.Alert(enabled, minStatus)
		);
	}

	@Test
	@DisplayName("알림이 켜져 있고 상태 코드가 임계값 이상이면 status/code/message가 담긴 메시지를 발송한다")
	void notifyError_sendsWhenEnabledAndAboveThreshold() {
		// given - enabled=true, minStatus=400
		RecordingDiscordSender sender = new RecordingDiscordSender();
		DiscordAlertNotifier notifier = new DiscordAlertNotifier(sender, properties(true, 400));

		// when - 4xx/5xx 경계 및 대표 상태 코드들
		notifier.notifyError(400, "C001", "잘못된 입력값입니다", "POST", "/api/signup", null);
		notifier.notifyError(404, "C002", "리소스를 찾을 수 없습니다", "GET", "/api/x", null);
		notifier.notifyError(409, "M001", "이미 가입된 이메일입니다", "POST", "/api/signup", null);
		notifier.notifyError(500, "C500", "서버 오류", "GET", "/api/y", null);

		// then - 4건 모두 발송되고 본문에 status/code/message가 포함된다
		assertThat(sender.sent).hasSize(4);
		assertThat(sender.sent.get(0).content())
			.contains("400").contains("C001").contains("잘못된 입력값입니다");
		assertThat(sender.sent.get(1).content())
			.contains("404").contains("C002").contains("리소스를 찾을 수 없습니다");
		assertThat(sender.sent.get(2).content())
			.contains("409").contains("M001").contains("이미 가입된 이메일입니다");
		assertThat(sender.sent.get(3).content())
			.contains("500").contains("C500").contains("서버 오류");
		assertThat(sender.sent).allSatisfy(
			message -> assertThat(message.username()).isEqualTo("Bookpool Alert"));
	}

	@Test
	@DisplayName("알림이 꺼져 있으면 500 에러여도 아무것도 발송하지 않는다")
	void notifyError_doesNotSendWhenDisabled() {
		// given - enabled=false
		RecordingDiscordSender sender = new RecordingDiscordSender();
		DiscordAlertNotifier notifier = new DiscordAlertNotifier(sender, properties(false, 400));

		// when
		notifier.notifyError(500, "C500", "서버 오류", "GET", "/api/y", new RuntimeException("boom"));

		// then
		assertThat(sender.sent).isEmpty();
	}

	@Test
	@DisplayName("min-status가 500이면 400은 발송하지 않고 500부터 발송한다(임계값 경계)")
	void notifyError_respectsMinStatusThreshold() {
		// given - enabled=true, minStatus=500
		RecordingDiscordSender sender = new RecordingDiscordSender();
		DiscordAlertNotifier notifier = new DiscordAlertNotifier(sender, properties(true, 500));

		// when - 임계값 미만(400)과 임계값(500)
		notifier.notifyError(400, "C001", "잘못된 입력값입니다", "POST", "/api/signup", null);
		notifier.notifyError(500, "C500", "서버 오류", "GET", "/api/y", null);

		// then - 500만 발송된다
		assertThat(sender.sent).hasSize(1);
		assertThat(sender.sent.get(0).content()).contains("500").contains("C500");
	}

	@Test
	@DisplayName("5xx이고 throwable이 있으면 스택트레이스를 첨부한다")
	void notifyError_attachesStackTraceFor5xx() {
		// given
		RecordingDiscordSender sender = new RecordingDiscordSender();
		DiscordAlertNotifier notifier = new DiscordAlertNotifier(sender, properties(true, 400));
		IllegalStateException cause = new IllegalStateException("DB 연결 실패");

		// when
		notifier.notifyError(500, "C500", "서버 오류", "GET", "/api/y", cause);

		// then - 본문에 예외 클래스명/메시지가 포함된다
		assertThat(sender.sent).hasSize(1);
		assertThat(sender.sent.get(0).content())
			.contains("IllegalStateException")
			.contains("DB 연결 실패");
	}

	@Test
	@DisplayName("4xx는 throwable이 있어도 스택트레이스를 첨부하지 않는다")
	void notifyError_doesNotAttachStackTraceFor4xx() {
		// given
		RecordingDiscordSender sender = new RecordingDiscordSender();
		DiscordAlertNotifier notifier = new DiscordAlertNotifier(sender, properties(true, 400));
		IllegalArgumentException cause = new IllegalArgumentException("validation");

		// when
		notifier.notifyError(400, "C001", "잘못된 입력값입니다", "POST", "/api/signup", cause);

		// then - 발송은 되지만 스택트레이스(예외 클래스명)는 포함되지 않는다
		assertThat(sender.sent).hasSize(1);
		assertThat(sender.sent.get(0).content())
			.contains("C001")
			.doesNotContain("IllegalArgumentException");
	}

	@Test
	@DisplayName("Sender가 예외를 던져도 notifyError는 예외를 전파하지 않는다(실패 안전)")
	void notifyError_isFailSafeWhenSenderThrows() {
		// given - 항상 예외를 던지는 Sender
		DiscordSender throwingSender = message -> {
			throw new RuntimeException("webhook down");
		};
		DiscordAlertNotifier notifier = new DiscordAlertNotifier(throwingSender, properties(true, 400));

		// when & then - 예외가 전파되지 않아야 한다
		assertThatCode(() ->
			notifier.notifyError(500, "C500", "서버 오류", "GET", "/api/y", new RuntimeException("boom"))
		).doesNotThrowAnyException();
	}
}
