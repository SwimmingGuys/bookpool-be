package kr.co.bookpool.app.auth.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.JwtException;
import kr.co.bookpool.app.auth.dto.AuthTokens;
import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.security.JwtProvider;
import kr.co.bookpool.common.security.RefreshTokenStore;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final RefreshTokenStore refreshTokenStore;

	@Transactional(readOnly = true)
	public AuthTokens login(LoginRequest request) {
		// 이메일 미존재/비밀번호 불일치를 동일하게 처리해 계정 존재 여부를 노출하지 않는다
		Member member = memberRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(LOGIN_FAILED));

		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(LOGIN_FAILED);
		}

		if (!member.isActive()) {
			throw new BusinessException(LOGIN_FAILED);
		}

		return issueTokens(member);
	}

	/**
	 * 리프레시 토큰으로 액세스 토큰을 재발급한다.
	 * 검증을 통과하면 리프레시 토큰도 함께 새로 발급(회전)해 탈취된 토큰의 수명을 제한한다.
	 */
	@Transactional(readOnly = true)
	public AuthTokens reissue(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			throw new BusinessException(INVALID_REFRESH_TOKEN);
		}

		Long memberId = parseMemberId(refreshToken);

		// Redis에 보관된 토큰과 일치해야 유효. 불일치/부재면 로그아웃·만료·탈취 가능성으로 보고 거부한다.
		String stored = refreshTokenStore.find(memberId)
			.orElseThrow(() -> new BusinessException(INVALID_REFRESH_TOKEN));
		if (!stored.equals(refreshToken)) {
			refreshTokenStore.delete(memberId);
			throw new BusinessException(INVALID_REFRESH_TOKEN);
		}

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(INVALID_REFRESH_TOKEN));
		if (!member.isActive()) {
			refreshTokenStore.delete(memberId);
			throw new BusinessException(INVALID_REFRESH_TOKEN);
		}

		return issueTokens(member);
	}

	/** 서버에 보관된 리프레시 토큰을 제거한다. 토큰이 이미 유효하지 않으면 정리할 것이 없으므로 조용히 넘어간다. */
	public void logout(String refreshToken) {
		if (!StringUtils.hasText(refreshToken)) {
			return;
		}
		try {
			refreshTokenStore.delete(parseMemberId(refreshToken));
		} catch (BusinessException ignored) {
			// 유효하지 않은 토큰: 서버 상태 변경 없음
		}
	}

	private AuthTokens issueTokens(Member member) {
		String accessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
		String refreshToken = jwtProvider.createRefreshToken(member.getId());
		refreshTokenStore.save(member.getId(), refreshToken,
			Duration.ofMillis(jwtProvider.getRefreshTokenValidityMs()));
		return new AuthTokens(accessToken, refreshToken);
	}

	private Long parseMemberId(String refreshToken) {
		try {
			return jwtProvider.getMemberId(jwtProvider.parse(refreshToken));
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(INVALID_REFRESH_TOKEN);
		}
	}
}