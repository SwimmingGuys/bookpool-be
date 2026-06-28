package kr.co.bookpool.app.recentview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.co.bookpool.app.recentview.entity.RecentView;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {

	Optional<RecentView> findByMemberIdAndCampaignId(Long memberId, Long campaignId);

	@Query("select rv.campaign.id from RecentView rv where rv.member.id = :memberId order by rv.viewedAt desc, rv.id desc")
	List<Long> findRecentCampaignIds(@Param("memberId") Long memberId, Pageable pageable);

	@Query(
		value = "select rv from RecentView rv join fetch rv.campaign where rv.member.id = :memberId",
		countQuery = "select count(rv) from RecentView rv where rv.member.id = :memberId"
	)
	Page<RecentView> findAllByMemberIdWithCampaign(@Param("memberId") Long memberId, Pageable pageable);
}
