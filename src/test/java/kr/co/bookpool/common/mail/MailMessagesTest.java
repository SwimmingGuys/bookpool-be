package kr.co.bookpool.common.mail;

import static org.assertj.core.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * messages.properties의 메일 문구가 키로 정상 해석되고 코드가 치환되는지 검증한다.
 * (실제 발송 경로는 LogEmailSender를 쓰는 통합 테스트에서 다루지 않으므로 여기서 문구를 보장한다.)
 */
class MailMessagesTest {

	private final ResourceBundleMessageSource messageSource = createMessageSource();

	private static ResourceBundleMessageSource createMessageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		return source;
	}

	@Test
	@DisplayName("회원가입 인증 메일의 제목/본문이 해석되고 코드가 치환된다")
	void signupMessages() {
		String subject = messageSource.getMessage(EmailSender.SIGNUP_VERIFICATION + ".subject", null, Locale.KOREA);
		String body = messageSource.getMessage(EmailSender.SIGNUP_VERIFICATION + ".body", new Object[] {"123456"},
			Locale.KOREA);

		assertThat(subject).isEqualTo("[북풀] 회원가입 이메일 인증 코드");
		assertThat(body).contains("회원가입 인증 코드: 123456");
	}

	@Test
	@DisplayName("비밀번호 재설정 메일의 제목/본문이 해석되고 코드가 치환된다")
	void passwordResetMessages() {
		String subject = messageSource.getMessage(EmailSender.PASSWORD_RESET + ".subject", null, Locale.KOREA);
		String body = messageSource.getMessage(EmailSender.PASSWORD_RESET + ".body", new Object[] {"654321"},
			Locale.KOREA);

		assertThat(subject).isEqualTo("[북풀] 비밀번호 재설정 인증 코드");
		assertThat(body).contains("비밀번호 재설정 인증 코드: 654321");
	}
}
