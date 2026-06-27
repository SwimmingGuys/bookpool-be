package kr.co.bookpool.common.config;

import static org.springframework.security.config.http.SessionCreationPolicy.*;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import kr.co.bookpool.app.member.verification.EmailVerificationProperties;
import kr.co.bookpool.app.member.entity.Role;
import kr.co.bookpool.common.security.CookieProperties;
import kr.co.bookpool.common.security.JwtAuthenticationEntryPoint;
import kr.co.bookpool.common.security.JwtAuthenticationFilter;
import kr.co.bookpool.common.security.JwtProperties;
import kr.co.bookpool.common.security.RestAccessDeniedHandler;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
	JwtProperties.class, CookieProperties.class, CorsProperties.class, EmailVerificationProperties.class
})
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
		"/api/signup",
		"/api/signup/email/code",
		"/api/signup/email/verify",
		"/api/login",
		"/api/reissue",
		"/api/logout",
		"/api/notices/**",
		"/api/campaigns/**",
		"/swagger-ui/**",
		"/v3/api-docs/**",
		"/actuator/health"
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;
	private final CorsProperties corsProperties;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		return http
			// JWT 기반 API 서버: 폼 로그인/기본 인증/CSRF 불필요
			.csrf(AbstractHttpConfigurer::disable)
			// 프론트가 다른 오리진이므로 CORS 활성화 (쿠키 송수신 위해 allowCredentials=true)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			// 세션 사용 안 함 (토큰으로만 인증)
			.sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
			// 경로별 인가 규칙
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
				.requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
				.anyRequest().authenticated()
			)
			// 미인증은 ApiResult 401, 권한 부족은 ApiResult 403
			.exceptionHandling(handler -> handler
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(restAccessDeniedHandler))
			// JWT 인증 필터 등록
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(corsProperties.allowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		// 자격 증명(쿠키) 허용. 이 때문에 허용 오리진에 와일드카드를 쓸 수 없다.
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
