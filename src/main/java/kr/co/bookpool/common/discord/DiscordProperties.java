package kr.co.bookpool.common.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Discord 알림 설정.
 *
 * <ul>
 *   <li>{@code webhookUrl} - Discord 채널 웹훅 URL (민감 정보, 환경변수/secret으로 주입)</li>
 *   <li>{@code username}   - 알림 메시지에 표시할 봇 이름</li>
 *   <li>{@code alert}      - 에러 알림(4xx/5xx) 동작 설정</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
	String webhookUrl,
	@DefaultValue("Bookpool Alert") String username,
	@DefaultValue Alert alert
) {

	/**
	 * @param enabled   에러 알림 활성화 여부 (기본 false — 환경별로 명시적으로 켠다)
	 * @param minStatus 알림을 보낼 최소 HTTP 상태 코드 (기본 400 — 4xx/5xx만 알림)
	 */
	public record Alert(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("400") int minStatus
	) {
	}
}
