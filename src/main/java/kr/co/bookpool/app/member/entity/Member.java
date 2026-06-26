package kr.co.bookpool.app.member.entity;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
	name = "member",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_email", columnNames = "email")
	}
)
@NoArgsConstructor(access = PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "nickname", nullable = false, length = 50)
	private String nickname;

	@Column(name = "password", nullable = false)
	private String password;

	@Enumerated(STRING)
	@Column(name = "role", nullable = false)
	private Role role;

	@Column(name = "email_subscribed", nullable = false)
	private boolean emailSubscribed;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	@Builder
	private Member(String email, String nickname, String password, Role role,
		boolean emailSubscribed, boolean isActive) {
		this.email = email;
		this.nickname = nickname;
		this.password = password;
		this.role = role;
		this.emailSubscribed = emailSubscribed;
		this.isActive = isActive;
	}

	public static Member create(String email, String nickname, String encodedPassword, boolean emailSubscribed) {
		return Member.builder()
			.email(email)
			.nickname(nickname)
			.password(encodedPassword)
			.role(Role.USER)
			.emailSubscribed(emailSubscribed)
			.isActive(true)
			.build();
	}

	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}
}

