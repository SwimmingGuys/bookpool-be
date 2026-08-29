package kr.co.bookpool.app.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.co.bookpool.app.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@EntityGraph(attributePaths = {"campaign", "member"})
	Page<Notification> findAllByMemberId(Long memberId, Pageable pageable);

	@EntityGraph(attributePaths = {"campaign", "member"})
	Optional<Notification> findWithCampaignById(Long id);

	long countByMemberIdAndReadIsFalse(Long memberId);

	// 한 건씩 읽음 처리하면 알림이 많을 때 쿼리가 그만큼 늘어난다.
	@Modifying(clearAutomatically = true)
	@Query("update Notification n set n.read = true where n.member.id = :memberId and n.read = false")
	int markAllRead(@Param("memberId") Long memberId);
}
