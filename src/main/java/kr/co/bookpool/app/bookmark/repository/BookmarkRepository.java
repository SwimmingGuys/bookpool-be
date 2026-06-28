package kr.co.bookpool.app.bookmark.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.co.bookpool.app.bookmark.entity.Bookmark;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

	@Query("select b.campaign.id from Bookmark b where b.member.id = :memberId")
	List<Long> findCampaignIdsByMemberId(@Param("memberId") Long memberId);

	@Query(
		value = "select b from Bookmark b join fetch b.campaign where b.member.id = :memberId",
		countQuery = "select count(b) from Bookmark b where b.member.id = :memberId"
	)
	Page<Bookmark> findAllByMemberIdWithCampaign(@Param("memberId") Long memberId, Pageable pageable);

	long deleteByMemberIdAndCampaignId(Long memberId, Long campaignId);

	boolean existsByMemberIdAndCampaignId(Long memberId, Long campaignId);
}
