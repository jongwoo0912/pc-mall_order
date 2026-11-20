package com.oraclejava.pcproduct.config;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.oraclejava.pcproduct.customer.Role;

/**
 * 세션 기반 로그인 (REST 엔드포인트).
 * - 로그인: POST /api/customers/login  JSON {mail, password}
 * - 로그아웃: POST /api/customers/logout
 * - CSRF 보호는 사용하지 않음
 * - 상품 조회는 누구나, 상품 등록/수정/삭제는 관리자(ROLE_ADMIN)만
 *   (로그인했지만 관리자가 아니면 403)
 */
@Configuration
public class SecurityConfig {

	private static final String ADD_PAGE = "/api/pc-products/add";

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** 로그인 엔드포인트에서 메일/비밀번호 검증에 사용 */
	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	/** 로그인 상태를 HTTP 세션에 저장/조회 */
	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			SecurityContextRepository securityContextRepository) throws Exception {
		// /api/** 는 401 응답, 그 외 화면은 로그인 페이지로 이동 (화면인 ADD_PAGE 는 /api 이지만 이동)
		RequestMatcher apiRequests = new AndRequestMatcher(
				pathPattern("/api/**"), new NegatedRequestMatcher(pathPattern(ADD_PAGE)));

		http
			.csrf(csrf -> csrf.disable())
			.securityContext(sc -> sc.securityContextRepository(securityContextRepository))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/login", "/signup", "/error",
						"/customers/**", "/pc-products/style.css", "/pc-products/list.html").permitAll()
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/customers", "/api/customers/login",
						"/api/customers/logout").permitAll()
				// 상품 관리(추가 화면, 등록/수정/삭제, 이미지 업로드/삭제)는 관리자만
				.requestMatchers(HttpMethod.GET, ADD_PAGE, "/pc-products/add.html").hasRole(Role.ADMIN)
				.requestMatchers(HttpMethod.GET, "/api/pc-products/**").permitAll()
				.requestMatchers("/api/pc-products/**").hasRole(Role.ADMIN)
				.anyRequest().authenticated())
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.logout(logout -> logout.disable())
			.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint(apiRequests)));

		return http.build();
	}

	/**
	 * 비로그인 접근 시: API 는 401,
	 * 화면 요청은 /login?next=<원래 경로> 로 이동 → 로그인 후 원래 화면으로 복귀
	 */
	private static AuthenticationEntryPoint entryPoint(RequestMatcher apiRequests) {
		AuthenticationEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
		return (request, response, ex) -> {
			if (apiRequests.matches(request)) {
				unauthorized.commence(request, response, ex);
				return;
			}
			String target = request.getRequestURI();
			if (request.getQueryString() != null) {
				target += "?" + request.getQueryString();
			}
			response.sendRedirect(request.getContextPath() + "/login?next="
					+ URLEncoder.encode(target, StandardCharsets.UTF_8));
		};
	}
}
