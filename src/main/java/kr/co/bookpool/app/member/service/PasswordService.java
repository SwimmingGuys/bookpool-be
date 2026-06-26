package kr.co.bookpool.app.member.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.security.RefreshTokenStore;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationStore emailVerificationStore;
	private final RefreshTokenStore refreshTokenStore;

	/** 로그인 상태에서 현재 비밀번호 확인 후 새 비밀번호로 변경한다. */
	@Transactional
	public void changePassword(Long memberId, String currentPassword, String newPassword) {
		Member member = memberRepository.findById(memberId)
			.filter(Member::isActive)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

		if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
			throw new BusinessException(INVALID_CURRENT_PASSWORD);
		}

		member.changePassword(passwordEncoder.encode(newPassword));
	}

	/**
	 * 이메일 인증을 완료한 이메일의 비밀번호를 재설정한다.
	 * - 인증 상태는 한 번 쓰고 소비해 동일 인증으로 재설정을 반복할 수 없게 한다.
	 * - 기존 리프레시 토큰을 무효화해 비번 변경 전 발급된 세션으로는 재발급할 수 없게 한다.
	 */
	@Transactional
	public void resetPassword(String email, String newPassword) {
		if (!emailVerificationStore.isVerified(email)) {
			throw new BusinessException(PASSWORD_RESET_NOT_VERIFIED);
		}

		Member member = memberRepository.findByEmail(email)
			.filter(Member::isActive)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

		member.changePassword(passwordEncoder.encode(newPassword));
		emailVerificationStore.deleteVerified(email);
		refreshTokenStore.delete(member.getId());
	}
}
