package kr.co.bookpool.app.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "member",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_email", columnNames = "email")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(nullable = false, length = 255)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "ENUM('USER','ADMIN') DEFAULT 'USER'")
	private MemberRole role = MemberRole.USER;

	@Column(name = "email_subscribed", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
	private Boolean emailSubscribed = true;

	@Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
	private Boolean active = true;
}
