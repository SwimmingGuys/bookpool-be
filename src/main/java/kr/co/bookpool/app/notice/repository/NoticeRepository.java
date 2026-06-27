package kr.co.bookpool.app.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.bookpool.app.notice.entity.Notice;
import kr.co.bookpool.app.notice.entity.NoticeCategory;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

	@Override
	@EntityGraph(attributePaths = "admin")
	Page<Notice> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "admin")
	Page<Notice> findAllByCategory(NoticeCategory category, Pageable pageable);
}
