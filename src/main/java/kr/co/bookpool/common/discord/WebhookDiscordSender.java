package kr.co.bookpool.common.discord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 운영/개발용 Discord 웹훅 발송 구현.
 *
 * <p>요청 스레드를 막지 않도록 {@link HttpClient#sendAsync}로 비동기 전송하며,
 * 직렬화/전송 중 어떤 예외도 호출자에게 전파하지 않는다(알림 실패는 로그만 남긴다).
 */
@Slf4j
@Component
@Profile({"dev", "prod"})
public class WebhookDiscordSender implements DiscordSender {

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	private final ObjectMapper objectMapper;
	private final DiscordProperties properties;
	private final HttpClient httpClient;

	public WebhookDiscordSender(ObjectMapper objectMapper, DiscordProperties properties) {
		this.objectMapper = objectMapper;
		this.properties = properties;
		this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	}

	@Override
	public void send(DiscordMessageRequest message) {
		String webhookUrl = properties.webhookUrl();
		if (webhookUrl == null || webhookUrl.isBlank()) {
			log.warn("Discord webhook URL이 설정되지 않아 알림을 건너뜁니다.");
			return;
		}
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(webhookUrl))
				.header("Content-Type", "application/json")
				.timeout(TIMEOUT)
				.POST(HttpRequest.BodyPublishers.ofString(
					objectMapper.writeValueAsString(message), StandardCharsets.UTF_8))
				.build();

			httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenAccept(response -> {
					// 연결 실패가 아닌 비-2xx 응답(예: 429 rate limit, 400 잘못된 페이로드)은
					// future가 정상 완료되므로 여기서 직접 확인해 로깅한다.
					if (response.statusCode() / 100 != 2) {
						log.warn("Discord 웹훅 비정상 응답: status={}, body={}", response.statusCode(), response.body());
					}
				})
				.exceptionally(throwable -> {
					log.warn("Discord 웹훅 전송 실패", throwable);
					return null;
				});
		} catch (Exception e) {
			log.warn("Discord 웹훅 메시지 구성 실패", e);
		}
	}
}
