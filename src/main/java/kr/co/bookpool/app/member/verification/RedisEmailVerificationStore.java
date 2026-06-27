package kr.co.bookpool.app.member.verification;

import java.time.Duration;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis 기반 이메일 인증 저장소.
 * - 코드:      "EV:CODE:{email}"     = 6자리 코드,  TTL = codeTtl
 * - 인증 완료: "EV:VERIFIED:{email}" = "true",       TTL = verifiedTtl
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisEmailVerificationStore implements EmailVerificationStore {

	private static final String CODE_PREFIX = "EV:CODE:";
	private static final String VERIFIED_PREFIX = "EV:VERIFIED:";
	private static final String VERIFIED_VALUE = "true";

	private final StringRedisTemplate redisTemplate;

	@Override
	public void saveCode(String email, String code, Duration ttl) {
		redisTemplate.opsForValue().set(codeKey(email), code, ttl);
	}

	@Override
	public Optional<String> findCode(String email) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(codeKey(email)));
	}

	@Override
	public void deleteCode(String email) {
		redisTemplate.delete(codeKey(email));
	}

	@Override
	public void markVerified(String email, Duration ttl) {
		redisTemplate.opsForValue().set(verifiedKey(email), VERIFIED_VALUE, ttl);
	}

	@Override
	public boolean isVerified(String email) {
		return VERIFIED_VALUE.equals(redisTemplate.opsForValue().get(verifiedKey(email)));
	}

	private String codeKey(String email) {
		return CODE_PREFIX + email;
	}

	private String verifiedKey(String email) {
		return VERIFIED_PREFIX + email;
	}
}
