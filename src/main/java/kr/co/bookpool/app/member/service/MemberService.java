package kr.co.bookpool.app.member.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.request.UpdateProfileRequest;
import kr.co.bookpool.app.member.dto.response.MeResponse;
import kr.co.bookpool.app.member.dto.response.SignUpResponse;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationStore emailVerificationStore;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		// 이메일 인증을 거친 이메일만 가입을 허용한다(인증 상태는 TTL이 지나면 자동 만료).
		if (!emailVerificationStore.isVerified(request.email())) {
			throw new BusinessException(EMAIL_NOT_VERIFIED);
		}

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

	public MeResponse getMyInfo(Long memberId) {
		return MeResponse.from(findById(memberId));
	}

	@Transactional
	public MeResponse updateProfile(Long memberId, UpdateProfileRequest request) {
		Member member = findById(memberId);
		member.updateProfile(request.nickname(), request.contact());
		return MeResponse.from(member);
	}

	private Member findById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
	}
}