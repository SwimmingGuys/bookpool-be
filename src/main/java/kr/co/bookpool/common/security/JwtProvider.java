package kr.co.bookpool.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.co.bookpool.app.member.entity.Role;

@Component
public class JwtProvider {

	private static final String ROLE_CLAIM = "role";

	private final SecretKey key;
	private final long accessTokenValidityMs;
	private final long refreshTokenValidityMs;

	public JwtProvider(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessTokenValidityMs = properties.accessTokenValidityMs();
		this.refreshTokenValidityMs = properties.refreshTokenValidityMs();
	}

	public String createAccessToken(Long memberId, Role role) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + accessTokenValidityMs);

		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.claim(ROLE_CLAIM, role.name())
			.issuedAt(now)
			.expiration(expiration)
			.signWith(key)
			.compact();
	}

	// 리프레시 토큰은 재발급 용도로만 쓰이므로 식별자(subject)만 담고 권한은 싣지 않는다.
	// 재발급 시 회원을 다시 조회해 그 시점의 role/활성 상태를 반영한다.
	// jti(UUID)를 넣어 같은 초에 발급해도 토큰이 항상 유일하도록 보장한다(회전·재사용 감지의 전제).
	public String createRefreshToken(Long memberId) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + refreshTokenValidityMs);

		return Jwts.builder()
			.id(UUID.randomUUID().toString())
			.subject(String.valueOf(memberId))
			.issuedAt(now)
			.expiration(expiration)
			.signWith(key)
			.compact();
	}

	public long getRefreshTokenValidityMs() {
		return refreshTokenValidityMs;
	}

	public Claims parse(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public Long getMemberId(Claims claims) {
		return Long.valueOf(claims.getSubject());
	}

	public Role getRole(Claims claims) {
		return Role.valueOf(claims.get(ROLE_CLAIM, String.class));
	}
}
