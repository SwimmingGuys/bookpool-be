package kr.co.bookpool.common.security;

import java.time.Duration;
import java.util.Optional;

/**
 * 회원별 리프레시 토큰 저장소.
 * 서버가 토큰을 보관하므로 로그아웃/탈취 시 즉시 무효화(삭제)할 수 있다.
 * 운영은 Redis 구현({@link RedisRefreshTokenStore})을 쓰고, 테스트는 인메모리 구현으로 대체한다.
 */
public interface RefreshTokenStore {

	void save(Long memberId, String refreshToken, Duration ttl);

	Optional<String> find(Long memberId);

	void delete(Long memberId);
}
