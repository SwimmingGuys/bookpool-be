package kr.co.bookpool.app.member.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.member.verification.EmailVerificationProperties;
import kr.co.bookpool.app.member.verification.EmailVerificationStore;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.mail.EmailSender;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private static final int CODE_BOUND = 1_000_000; // 000000 ~ 999999

	private final SecureRandom secureRandom = new SecureRandom();

	private final MemberRepository memberRepository;
	private final EmailVerificationStore verificationStore;
	private final EmailSender emailSender;
	private final EmailVerificationProperties properties;

	/** 회원가입 인증 코드를 발급한다. 아직 가입되지 않은 이메일만 허용한다(이미 가입 시 M001). */
	/** 인증 코드를 발급해 Redis에 저장하고 이메일로 발송한다. (회원가입용 — 신규 이메일만 허용) */
	public void sendCode(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new BusinessException(DUPLICATE_EMAIL);
		}

		String code = issueCode(email);
		emailSender.sendCode(email, EmailSender.SIGNUP_VERIFICATION, code);
	}

	/** 비밀번호 재설정용 인증 코드를 발급한다. 활성 상태의 가입된 이메일만 허용한다(미가입/비활성 시 M002). */
	public void sendCodeForPasswordReset(String email) {
		memberRepository.findByEmail(email)
			.filter(Member::isActive)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

		String code = issueCode(email);
		emailSender.sendCode(email, EmailSender.PASSWORD_RESET, code);
		issueCode(email);
	}

	/** 비밀번호 재설정용 인증 코드를 발급한다. (가입된 이메일만 허용) */
	public void sendResetCode(String email) {
		if (!memberRepository.existsByEmail(email)) {
			throw new BusinessException(MEMBER_NOT_FOUND);
		}

		issueCode(email);
	}

	private void issueCode(String email) {
		String code = generateCode();
		verificationStore.saveCode(email, code, properties.codeTtl());
		emailSender.sendVerificationCode(email, code);
	}

	/** 코드를 검증하고, 일치하면 인증 완료 상태로 표시한다. */
	public void verify(String email, String code) {
		String stored = verificationStore.findCode(email)
			.orElseThrow(() -> new BusinessException(INVALID_VERIFICATION_CODE));

		if (!stored.equals(code)) {
			throw new BusinessException(INVALID_VERIFICATION_CODE);
		}

		verificationStore.markVerified(email, properties.verifiedTtl());
		verificationStore.deleteCode(email);
	}

	/** 6자리 코드를 생성해 TTL과 함께 저장하고 코드를 반환한다. */
	private String issueCode(String email) {
		String code = generateCode();
		verificationStore.saveCode(email, code, properties.codeTtl());
		return code;
	}

	private String generateCode() {
		return String.format("%06d", secureRandom.nextInt(CODE_BOUND));
	}
}
