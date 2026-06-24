package kr.co.bookpool.common.security;

import java.time.Duration;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis 기반 리프레시 토큰 저장소.
 * key = "RT:{memberId}", value = 발급된 리프레시 토큰, TTL = 리프레시 토큰 유효기간.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_PREFIX = "RT:";

	private final StringRedisTemplate redisTemplate;

	@Override
	public void save(Long memberId, String refreshToken, Duration ttl) {
		redisTemplate.opsForValue().set(key(memberId), refreshToken, ttl);
	}

	@Override
	public Optional<String> find(Long memberId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(memberId)));
	}

	@Override
	public void delete(Long memberId) {
		redisTemplate.delete(key(memberId));
	}

	private String key(Long memberId) {
		return KEY_PREFIX + memberId;
	}
}
