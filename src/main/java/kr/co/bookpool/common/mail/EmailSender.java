package kr.co.bookpool.common.mail;

/**
 * 이메일 발송 추상화.
 * SMTP 구현({@link SmtpEmailSender})과 로그 구현({@link LogEmailSender})을 토글로 전환한다.
 * 제목/본문 문구는 messages.properties에 모아두고 messageKey로 가져온다.
 */
public interface EmailSender {

	// 메시지 키(messages.properties의 {key}.subject / {key}.body)
	String SIGNUP_VERIFICATION = "mail.signup";
	String PASSWORD_RESET = "mail.password-reset";

	/** messageKey에 해당하는 제목/본문으로 인증 코드 메일을 발송한다. */
	void sendCode(String to, String messageKey, String code);
}
