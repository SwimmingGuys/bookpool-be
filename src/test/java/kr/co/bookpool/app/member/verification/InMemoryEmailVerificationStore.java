package kr.co.bookpool.app.member.verification;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트용 인메모리 이메일 인증 저장소.
 * Redis 인프라 없이 인증 흐름을 검증하기 위해 test 프로필에서 Redis 구현을 대체한다.
 * (TTL은 테스트 범위에서 의미가 없어 무시한다.)
 */
@Component
@Profile("test")
public class InMemoryEmailVerificationStore implements EmailVerificationStore {

	private final Map<String, String> codes = new ConcurrentHashMap<>();
	private final Set<String> verified = ConcurrentHashMap.newKeySet();

	@Override
	public void saveCode(String email, String code, Duration ttl) {
		codes.put(email, code);
	}

	@Override
	public Optional<String> findCode(String email) {
		return Optional.ofNullable(codes.get(email));
	}

	@Override
	public void deleteCode(String email) {
		codes.remove(email);
	}

	@Override
	public void markVerified(String email, Duration ttl) {
		verified.add(email);
	}

	@Override
	public boolean isVerified(String email) {
		return verified.contains(email);
	}
}
