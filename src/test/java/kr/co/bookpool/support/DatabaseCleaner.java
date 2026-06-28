package kr.co.bookpool.support;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 통합 테스트용 DB 정리 유틸.
 *
 * <p>여러 {@code @SpringBootTest} 클래스가 컨텍스트 캐싱으로 같은 H2 인스턴스를 공유하므로,
 * 한 테스트가 남긴 자식 행(bookmark/recent_view 등)이 다른 테스트의 member 삭제를 FK 제약으로 막을 수 있다.
 * 참조 무결성을 잠시 끄고 모든 테이블을 truncate 해 도메인 추가에도 깨지지 않게 한다.
 */
@Component
@Profile("test")
public class DatabaseCleaner {

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void clear() {
		entityManager.flush();
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

		@SuppressWarnings("unchecked")
		List<String> tables = entityManager.createNativeQuery(
			"SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'"
		).getResultList();

		for (String table : tables) {
			entityManager.createNativeQuery("TRUNCATE TABLE \"" + table + "\"").executeUpdate();
		}

		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
	}
}
