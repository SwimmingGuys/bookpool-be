package kr.co.bookpool.common.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 운영/개발용 SMTP 이메일 발송 구현.
 * JavaMailSender 빈이 필요하므로 spring.mail.* 설정이 있어야 한다.
 */
@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;

	@Value("${app.mail.from:noreply@bookpool.co.kr}")
	private String from;

	@Override
	public void sendVerificationCode(String email, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(email);
		message.setSubject("[북풀] 이메일 인증 코드");
		message.setText("인증 코드: " + code + "\n5분 안에 입력해 주세요.");
		mailSender.send(message);
	}
}
