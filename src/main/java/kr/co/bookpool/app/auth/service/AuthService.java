package kr.co.bookpool.app.auth.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	public LoginResponse login(LoginRequest request) {
		// 이메일 미존재/비밀번호 불일치를 동일하게 처리해 계정 존재 여부를 노출하지 않는다
		Member member = memberRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(LOGIN_FAILED));

		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(LOGIN_FAILED);
		}

		if (!member.isActive()) {
			throw new BusinessException(LOGIN_FAILED);
		}

		String accessToken = jwtProvider.createAccessToken(member.getId(), member.getRole());
		return LoginResponse.of(accessToken);
	}
}
