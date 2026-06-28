package kr.co.bookpool.common.discord;

/**
 * Discord 메시지 발송 추상화.
 * 로컬/테스트는 로그 구현({@link LogDiscordSender}), 운영/개발은 웹훅 구현({@link WebhookDiscordSender})을 쓴다.
 *
 * <p>구현체는 절대 예외를 전파하지 않는다 — 알림 실패가 호출자(예: 에러 응답 처리)를 방해해서는 안 된다.
 */
public interface DiscordSender {

	void send(DiscordMessageRequest message);
}
