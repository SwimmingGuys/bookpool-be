package kr.co.bookpool.common.mail;

/**
 * 이메일 발송 추상화.
 * 로컬/테스트는 로그 구현({@link LogEmailSender}), 운영/개발은 SMTP 구현({@link SmtpEmailSender})을 쓴다.
 */
public interface EmailSender {

	void sendVerificationCode(String email, String code);
}
