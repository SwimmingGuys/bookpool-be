package kr.co.bookpool.app.bookmark.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.bookmark.entity.Bookmark;
import kr.co.bookpool.app.bookmark.repository.BookmarkRepository;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

	private final BookmarkRepository bookmarkRepository;
	private final CampaignRepository campaignRepository;
	private final MemberRepository memberRepository;

	public List<Long> getBookmarkedCampaignIds(Long memberId) {
		return bookmarkRepository.findCampaignIdsByMemberId(memberId);
	}

	public PageResponse<CampaignResponse> getBookmarkedCampaigns(Long memberId, int page, int size) {
		// createdAt 동률 시 순서가 비결정적이 되지 않도록 id를 2차 정렬 키로 둔다.
		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
		Pageable pageable = PageRequest.of(page, size, sort);
		return PageResponse.from(
			bookmarkRepository.findAllByMemberIdWithCampaign(memberId, pageable)
				.map(bookmark -> CampaignResponse.from(bookmark.getCampaign()))
		);
	}

	@Transactional
	public void add(Long memberId, Long campaignId) {
		if (bookmarkRepository.existsByMemberIdAndCampaignId(memberId, campaignId)) {
			throw new BusinessException(BOOKMARK_ALREADY_EXISTS);
		}
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
		Campaign campaign = campaignRepository.findById(campaignId)
			.orElseThrow(() -> new BusinessException(CAMPAIGN_NOT_FOUND));
		bookmarkRepository.save(Bookmark.create(member, campaign));
	}

	@Transactional
	public void remove(Long memberId, Long campaignId) {
		long deleted = bookmarkRepository.deleteByMemberIdAndCampaignId(memberId, campaignId);
		if (deleted == 0) {
			throw new BusinessException(BOOKMARK_NOT_FOUND);
		}
	}
}
