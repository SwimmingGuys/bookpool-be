package kr.co.bookpool.app.notification.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.notification.dto.NotificationSubscriptionDto;
import kr.co.bookpool.app.notification.entity.NotificationSubscription;
import kr.co.bookpool.app.notification.repository.NotificationSubscriptionRepository;
import kr.co.bookpool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSubscriptionService {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};

	private final NotificationSubscriptionRepository subscriptionRepository;
	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;

	public NotificationSubscriptionDto get(Long memberId) {
		return subscriptionRepository.findByMemberId(memberId)
			.map(this::toDto)
			.orElseGet(NotificationSubscriptionDto::empty);
	}

	@Transactional
	public NotificationSubscriptionDto save(Long memberId, NotificationSubscriptionDto request) {
		String typesJson = writeJson(request.types());
		String categoriesJson = writeJson(request.categories());
		String publishersJson = writeJson(request.publishers());

		NotificationSubscription saved = subscriptionRepository.findByMemberId(memberId)
			.map(existing -> {
				existing.update(typesJson, categoriesJson, publishersJson);
				return existing;
			})
			.orElseGet(() -> {
				Member member = memberRepository.findById(memberId)
					.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
				return subscriptionRepository.save(
					NotificationSubscription.create(member, typesJson, categoriesJson, publishersJson)
				);
			});

		return toDto(saved);
	}

	private NotificationSubscriptionDto toDto(NotificationSubscription entity) {
		return new NotificationSubscriptionDto(
			readJson(entity.getTypesJson()),
			readJson(entity.getCategoriesJson()),
			readJson(entity.getPublishersJson())
		);
	}

	private String writeJson(List<String> values) {
		return objectMapper.writeValueAsString(values == null ? List.of() : values);
	}

	private List<String> readJson(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		List<String> parsed = objectMapper.readValue(raw, STRING_LIST);
		return parsed == null ? List.of() : parsed;
	}
}
