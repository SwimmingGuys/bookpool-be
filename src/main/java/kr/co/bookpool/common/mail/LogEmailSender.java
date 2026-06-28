package kr.co.bookpool.common.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 로그 출력용 이메일 발송 구현(기본값).
 * {@code app.mail.smtp.enabled}가 false이거나 없을 때 활성화된다.
 * 실제로 메일을 보내지 않고 인증 코드를 로그로 출력해, SMTP 자격증명 없이도 인증 흐름을 테스트할 수 있게 한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.smtp.enabled", havingValue = "false", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

	@Override
	public void sendCode(String to, String messageKey, String code) {
		log.info("[메일][LOG] to={}, type={}, code={} (실제 발송하지 않음)", to, messageKey, code);
	}
}
