package kr.co.bookpool.app.recentview.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "recent_view",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_recent_view_member_campaign", columnNames = {"member_id", "campaign_id"})
	},
	indexes = {
		@Index(name = "idx_recent_view_member_viewed", columnList = "member_id, viewed_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentView {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	@Column(name = "viewed_at", nullable = false)
	private LocalDateTime viewedAt;

	private RecentView(Member member, Campaign campaign) {
		this.member = member;
		this.campaign = campaign;
		this.viewedAt = LocalDateTime.now();
	}

	public static RecentView create(Member member, Campaign campaign) {
		return new RecentView(member, campaign);
	}

	public void touch() {
		this.viewedAt = LocalDateTime.now();
	}
}
