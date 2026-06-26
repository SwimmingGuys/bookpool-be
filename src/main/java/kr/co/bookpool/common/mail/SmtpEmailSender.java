package kr.co.bookpool.common.mail;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 실제 SMTP 이메일 발송 구현.
 * {@code app.mail.smtp.enabled=true}일 때만 활성화된다(프로필 무관).
 * JavaMailSender 빈이 필요하므로 spring.mail.* 설정이 함께 있어야 한다.
 */
@Component
@ConditionalOnProperty(name = "app.mail.smtp.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final MessageSource messageSource;

	@Value("${app.mail.from:noreply@bookpool.co.kr}")
	private String from;

	@Override
	public void sendCode(String to, String messageKey, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(messageSource.getMessage(messageKey + ".subject", null, Locale.KOREA));
		message.setText(messageSource.getMessage(messageKey + ".body", new Object[] {code}, Locale.KOREA));
		mailSender.send(message);
	}
}
