package kr.co.bookpool.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.co.bookpool.app.member.entity.Role;

@Component
public class JwtProvider {

	private static final String ROLE_CLAIM = "role";

	private final SecretKey key;
	private final long accessTokenValidityMs;

	public JwtProvider(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessTokenValidityMs = properties.accessTokenValidityMs();
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

	public boolean validate(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public Long getMemberId(String token) {
		return Long.valueOf(parseClaims(token).getSubject());
	}

	public Role getRole(String token) {
		return Role.valueOf(parseClaims(token).get(ROLE_CLAIM, String.class));
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
