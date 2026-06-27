package kr.co.bookpool.app.bookmark.entity;

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
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "bookmark",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_bookmark_member_campaign", columnNames = {"member_id", "campaign_id"})
	},
	indexes = {
		@Index(name = "idx_bookmark_campaign_id", columnList = "campaign_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	private Bookmark(Member member, Campaign campaign) {
		this.member = member;
		this.campaign = campaign;
	}

	public static Bookmark create(Member member, Campaign campaign) {
		return new Bookmark(member, campaign);
	}
}
