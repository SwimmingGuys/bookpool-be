package kr.co.bookpool.app.member.verification;

import java.time.Duration;
import java.util.Optional;

/**
 * 이메일 인증 코드와 인증 완료 상태 저장소.
 * 운영은 Redis 구현({@link RedisEmailVerificationStore})을 쓰고, 테스트는 인메모리 구현으로 대체한다.
 */
public interface EmailVerificationStore {

	void saveCode(String email, String code, Duration ttl);

	Optional<String> findCode(String email);

	void deleteCode(String email);

	void markVerified(String email, Duration ttl);

	boolean isVerified(String email);

	void deleteVerified(String email);
}
