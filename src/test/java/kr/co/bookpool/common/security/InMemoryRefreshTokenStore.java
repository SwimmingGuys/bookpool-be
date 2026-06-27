package kr.co.bookpool.common.security;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트용 인메모리 리프레시 토큰 저장소.
 * 운영 코드가 Redis 인프라 없이도 동작하도록 test 프로필에서 Redis 구현을 대체한다.
 * (H2가 MySQL을 대체하는 것과 동일한 취지. TTL은 테스트 범위에서 의미가 없어 무시한다.)
 */
@Component
@Profile("test")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

	private final Map<Long, String> store = new ConcurrentHashMap<>();

	@Override
	public void save(Long memberId, String refreshToken, Duration ttl) {
		store.put(memberId, refreshToken);
	}

	@Override
	public Optional<String> find(Long memberId) {
		return Optional.ofNullable(store.get(memberId));
	}

	@Override
	public void delete(Long memberId) {
		store.remove(memberId);
	}
}
