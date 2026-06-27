package kr.co.bookpool.common.mail;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬/테스트용 이메일 발송 구현.
 * 실제로 메일을 보내지 않고 인증 코드를 로그로 출력해, SMTP 자격증명 없이도 인증 흐름을 테스트할 수 있게 한다.
 */
@Slf4j
@Component
@Profile({"local", "test"})
public class LogEmailSender implements EmailSender {

	@Override
	public void sendVerificationCode(String email, String code) {
		log.info("[이메일 인증][LOG] to={}, code={} (로컬/테스트 환경: 실제 발송하지 않음)", email, code);
	}
}
