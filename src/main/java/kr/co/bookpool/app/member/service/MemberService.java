package kr.co.bookpool.app.member.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.response.SignUpResponse;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		if (memberRepository.existsByEmail(request.email())) {
			throw new BusinessException(DUPLICATE_EMAIL);
		}

		String encodedPassword = passwordEncoder.encode(request.password());
		Member member = memberRepository.save(
			Member.create(
				request.email(),
				request.nickname(),
				encodedPassword,
				request.emailSubscribed()
			)
		);

		return SignUpResponse.from(member);
	}
}