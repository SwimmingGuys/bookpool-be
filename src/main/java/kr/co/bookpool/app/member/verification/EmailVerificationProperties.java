package kr.co.bookpool.app.member.verification;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이메일 인증 설정.
 * - codeTtl: 발송한 인증 코드의 유효시간
 * - verifiedTtl: 인증 완료 상태의 유효시간(이 안에 회원가입을 마쳐야 함)
 */
@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
	Duration codeTtl,
	Duration verifiedTtl
) {
}
