package kr.co.bookpool.common.config;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;

/**
 * 쿼리 파라미터의 문자열을 enum으로 변환한다.
 *
 * <p>Spring 기본 변환기는 {@code Enum.valueOf}만 쓰기 때문에 {@code @JsonCreator}로 만든
 * 별칭(한국어 라벨 등)이 요청 본문에서만 동작하고 쿼리 파라미터에서는 무시됐다.
 * 프론트는 카테고리를 '자기계발' 같은 라벨로 보내므로 양쪽 규칙이 같아야 한다.
 *
 * <p>enum에 {@code public static X from(String)}이 있으면 그것을 쓰고,
 * 없으면 대소문자만 맞춰 {@code valueOf}로 처리한다.
 */
public class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

	// 리플렉션 조회는 요청마다 반복할 필요가 없다.
	private static final Map<Class<?>, Method> FACTORY_METHODS = new ConcurrentHashMap<>();
	private static final Method NONE;

	static {
		try {
			NONE = StringToEnumConverterFactory.class.getDeclaredMethod("noFactoryMethod");
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unused")
	private static void noFactoryMethod() {
	}

	@Override
	@NonNull
	public <T extends Enum> Converter<String, T> getConverter(@NonNull Class<T> targetType) {
		return new StringToEnum<>(targetType);
	}

	private record StringToEnum<T extends Enum>(Class<T> enumType) implements Converter<String, T> {

		@Override
		@SuppressWarnings("unchecked")
		public T convert(@NonNull String source) {
			String value = source.trim();
			if (value.isEmpty()) return null;

			Method factory = factoryMethod(enumType);
			if (factory != null) {
				try {
					return (T)factory.invoke(null, value);
				} catch (ReflectiveOperationException e) {
					Throwable cause = e.getCause();
					if (cause instanceof IllegalArgumentException illegal) throw illegal;
					throw new IllegalArgumentException("값을 변환할 수 없습니다: " + source, e);
				}
			}

			return (T)Enum.valueOf(enumType.asSubclass(Enum.class), value.toUpperCase(Locale.ROOT));
		}

		private static Method factoryMethod(Class<?> enumType) {
			Method cached = FACTORY_METHODS.computeIfAbsent(enumType, type -> {
				try {
					Method method = type.getDeclaredMethod("from", String.class);
					boolean usable = java.lang.reflect.Modifier.isStatic(method.getModifiers())
						&& java.lang.reflect.Modifier.isPublic(method.getModifiers())
						&& type.isAssignableFrom(method.getReturnType());
					return usable ? method : NONE;
				} catch (NoSuchMethodException e) {
					return NONE;
				}
			});
			return cached == NONE ? null : cached;
		}
	}
}
