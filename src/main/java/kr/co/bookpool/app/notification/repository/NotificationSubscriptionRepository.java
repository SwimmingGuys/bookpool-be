package kr.co.bookpool.app.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.bookpool.app.notification.entity.NotificationSubscription;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

	Optional<NotificationSubscription> findByMemberId(Long memberId);
}
