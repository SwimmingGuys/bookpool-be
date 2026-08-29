package kr.co.bookpool.app.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 "이 공고에 신청했다"고 표시한 기록.
 *
 * <p>신청 버튼을 누르면 외부 폼으로 나가기 때문에 서비스는 실제 신청 여부를 알 수 없다.
 * 그래서 자기 신고로 남기고, 이걸 기준으로 마이페이지에서 발표일·서평 마감을 챙겨준다.
 * 신청 → 발표 → 서평으로 이어지는 흐름의 가운데가 그동안 비어 있었다.
 */
@Getter
@Entity
@Table(
	name = "application",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_application_member_campaign", columnNames = {"member_id", "campaign_id"})
	},
	indexes = {
		@Index(name = "idx_application_campaign_id", columnList = "campaign_id"),
		@Index(name = "idx_application_member_status", columnList = "member_id, status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApplicationStatus status;

	private Application(Member member, Campaign campaign, ApplicationStatus status) {
		this.member = member;
		this.campaign = campaign;
		this.status = status;
	}

	public static Application create(Member member, Campaign campaign, ApplicationStatus status) {
		return new Application(member, campaign, status == null ? ApplicationStatus.APPLIED : status);
	}

	public void changeStatus(ApplicationStatus status) {
		this.status = status;
	}
}
