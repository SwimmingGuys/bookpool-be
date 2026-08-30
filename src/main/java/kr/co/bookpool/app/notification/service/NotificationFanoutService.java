package kr.co.bookpool.app.notification.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.notification.entity.Notification;
import kr.co.bookpool.app.notification.entity.NotificationKind;
import kr.co.bookpool.app.notification.entity.NotificationSubscription;
import kr.co.bookpool.app.notification.repository.NotificationRepository;
import kr.co.bookpool.app.notification.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공고가 게시될 때 구독 조건에 맞는 회원에게 알림을 남긴다.
 *
 * <p>구독 설정은 저장되고 있었지만 알림이 쌓이는 곳이 없어 헤더의 벨이 항상 0이었다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFanoutService {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};

	private final NotificationSubscriptionRepository subscriptionRepository;
	private final NotificationRepository notificationRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public int notifyNewCampaign(Campaign campaign) {
		if (!campaign.isPublished()) return 0;

		List<Notification> notifications = new ArrayList<>();
		for (NotificationSubscription subscription : subscriptionRepository.findAll()) {
			if (!matches(campaign, subscription)) continue;
			notifications.add(Notification.of(
				subscription.getMember(),
				NotificationKind.NEW_RECRUITMENT,
				campaign,
				"관심 조건에 맞는 새 공고가 올라왔어요."
			));
		}

		if (notifications.isEmpty()) return 0;
		notificationRepository.saveAll(notifications);
		log.info("[알림] 공고 {} 게시 → {}명에게 발송", campaign.getId(), notifications.size());
		return notifications.size();
	}

	/**
	 * 구독 조건과 공고를 대조한다.
	 * 조건이 하나도 없으면 알림을 보내지 않는다(전체 수신은 명시적으로 고르게 한다).
	 * 지정한 축은 AND, 축 안의 값은 OR로 본다.
	 */
	private boolean matches(Campaign campaign, NotificationSubscription subscription) {
		List<String> types = readJson(subscription.getTypesJson());
		List<String> categories = readJson(subscription.getCategoriesJson());
		List<String> publishers = readJson(subscription.getPublishersJson());

		if (types.isEmpty() && categories.isEmpty() && publishers.isEmpty()) return false;

		if (!types.isEmpty() && !containsType(types, campaign)) return false;
		if (!categories.isEmpty() && !containsCategory(categories, campaign)) return false;
		return publishers.isEmpty() || publishers.contains(campaign.getPublisherName());
	}

	// 프론트는 'Reviewer'/'Beta Reader'로, 백엔드는 REVIEWER/BETA_READER로 다룬다.
	private boolean containsType(List<String> types, Campaign campaign) {
		String name = campaign.getType().name();
		String label = campaign.getType().name().equals("REVIEWER") ? "Reviewer" : "Beta Reader";
		return types.stream().anyMatch(t -> t.equalsIgnoreCase(name) || t.equalsIgnoreCase(label));
	}

	// 카테고리는 enum 이름과 한국어 라벨 양쪽으로 저장돼 있을 수 있다.
	private boolean containsCategory(List<String> categories, Campaign campaign) {
		String name = campaign.getCategory().name();
		String label = campaign.getCategory().getLabel();
		return categories.stream().anyMatch(c -> c.equalsIgnoreCase(name) || c.equals(label));
	}

	private List<String> readJson(String raw) {
		if (raw == null || raw.isBlank()) return List.of();
		List<String> parsed = objectMapper.readValue(raw, STRING_LIST);
		return parsed == null ? List.of() : parsed;
	}
}
