package kr.co.bookpool.support;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 모든 통합 테스트의 각 테스트 직전에 DB를 비워, 컨텍스트 캐싱으로 공유되는 H2의
 * 클래스 간 데이터 누수(특히 FK 위반)를 방지한다. (ServiceLoader 자동 감지로 전역 적용)
 *
 * <p>Spring 컨텍스트가 없는 단위 테스트에서는 아무 일도 하지 않는다.
 */
public class DatabaseCleanupExtension implements BeforeEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) {
		ApplicationContext applicationContext;
		try {
			applicationContext = SpringExtension.getApplicationContext(context);
		} catch (Exception e) {
			return; // Spring 컨텍스트가 없는 테스트 → 정리 불필요
		}

		try {
			applicationContext.getBean(DatabaseCleaner.class).clear();
		} catch (org.springframework.beans.BeansException e) {
			// DatabaseCleaner 빈이 없는 컨텍스트(예: 비-test 프로필) → 건너뜀
		}
	}
}
