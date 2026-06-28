package kr.co.bookpool.common.discord;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 에러 응답(4xx/5xx)을 Discord로 알리는 컴포넌트.
 *
 * <p>{@code discord.alert.enabled}가 true이고 상태 코드가 {@code discord.alert.min-status} 이상일 때만 발송한다.
 * 현재는 에러 알림만 연결되어 있으며, 배치/모니터링 등 다른 알림 유형은 {@link DiscordSender}를 재사용해 확장할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordAlertNotifier {

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int MAX_CONTENT_LENGTH = 1900;
	private static final int MAX_STACK_TRACE_LENGTH = 1000;

	private final DiscordSender discordSender;
	private final DiscordProperties properties;

	/**
	 * 에러 응답을 Discord로 알린다. 설정/임계값에 따라 무시될 수 있으며, 알림 실패는 호출자에게 전파되지 않는다.
	 *
	 * @param status    HTTP 상태 코드
	 * @param code      ErrorCode 코드(예: C001)
	 * @param message   에러 메시지
	 * @param method    요청 HTTP 메서드 (없으면 null)
	 * @param path      요청 경로 (없으면 null)
	 * @param throwable 원인 예외 (없으면 null) — 5xx일 때만 스택트레이스를 첨부한다
	 */
	public void notifyError(int status, String code, String message, String method, String path, Throwable throwable) {
		DiscordProperties.Alert alert = properties.alert();
		if (!alert.enabled() || status < alert.minStatus()) {
			return;
		}
		try {
			String content = buildContent(status, code, message, method, path, throwable);
			discordSender.send(DiscordMessageRequest.of(content, properties.username()));
		} catch (Exception e) {
			log.warn("Discord 에러 알림 처리 실패", e);
		}
	}

	private String buildContent(int status, String code, String message, String method, String path, Throwable throwable) {
		String emoji = status >= 500 ? "🟥" : "🟧"; // 🟥 / 🟧
		StringBuilder sb = new StringBuilder()
			.append(emoji).append(" **[").append(status).append("] ").append(code).append("**\n")
			.append("time: ").append(LocalDateTime.now().format(TIMESTAMP)).append('\n')
			.append("request: ").append(method == null ? "-" : method).append(' ')
			.append(path == null ? "-" : path).append('\n')
			.append("message: ").append(message);

		if (throwable != null && status >= 500) {
			sb.append("\n```\n").append(stackTrace(throwable)).append("\n```");
		}

		String content = sb.toString();
		if (content.length() > MAX_CONTENT_LENGTH) {
			content = content.substring(0, MAX_CONTENT_LENGTH) + "…(truncated)";
			// 코드펜스(```) 중간에서 잘리면 닫는 펜스가 사라져 마크다운이 깨지므로 보정한다.
			if (countOccurrences(content, "```") % 2 != 0) {
				content += "\n```";
			}
		}
		return content;
	}

	private int countOccurrences(String text, String token) {
		int count = 0;
		for (int idx = text.indexOf(token); idx >= 0; idx = text.indexOf(token, idx + token.length())) {
			count++;
		}
		return count;
	}

	private String stackTrace(Throwable throwable) {
		StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		String trace = writer.toString();
		return trace.length() > MAX_STACK_TRACE_LENGTH
			? trace.substring(0, MAX_STACK_TRACE_LENGTH) + "…"
			: trace;
	}
}
