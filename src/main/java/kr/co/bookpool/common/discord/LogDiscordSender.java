package kr.co.bookpool.common.discord;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬/테스트용 Discord 발송 구현.
 * 실제로 웹훅을 호출하지 않고 메시지를 로그로 출력해, 웹훅 URL 없이도 알림 흐름을 검증할 수 있게 한다.
 */
@Slf4j
@Component
@Profile({"local", "test"})
public class LogDiscordSender implements DiscordSender {

	@Override
	public void send(DiscordMessageRequest message) {
		log.info("[Discord][LOG] username={}, content={} (로컬/테스트 환경: 실제 발송하지 않음)",
			message.username(), message.content());
	}
}
