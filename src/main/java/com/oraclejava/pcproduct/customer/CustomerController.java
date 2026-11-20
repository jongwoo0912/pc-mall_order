package com.oraclejava.pcproduct.customer;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.oraclejava.pcproduct.customer.CustomerDtos.CustomerResponse;
import com.oraclejava.pcproduct.customer.CustomerDtos.LoginRequest;
import com.oraclejava.pcproduct.customer.CustomerDtos.SignUpRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class CustomerController {

	private final CustomerService service;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	public CustomerController(CustomerService service, AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository) {
		this.service = service;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	/** 회원 가입 (JSON: name, mail, password) */
	@PostMapping("/api/customers")
	public ResponseEntity<CustomerResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		CustomerResponse created = service.signUp(request);
		return ResponseEntity.created(URI.create("/api/customers/me")).body(created);
	}

	/**
	 * 로그인 (JSON: mail, password).
	 * 성공 시 200 + 회원 정보, 세션(JSESSIONID 쿠키)에 로그인 상태 저장. 실패 시 401.
	 */
	@PostMapping("/api/customers/login")
	public CustomerResponse login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String mail = CustomerService.normalizeMail(request.mail());
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(mail, request.password()));
		} catch (AuthenticationException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "메일 주소 또는 비밀번호가 올바르지 않습니다.");
		}

		// 세션 고정 공격 방지: 로그인 전 세션이 있으면 ID 교체
		if (httpRequest.getSession(false) != null) {
			httpRequest.changeSessionId();
		}
		SecurityContext context = contextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		contextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);

		return service.findByMail(authentication.getName());
	}

	/** 로그아웃: 세션 무효화. 항상 204 */
	@PostMapping("/api/customers/logout")
	public ResponseEntity<Void> logout(Authentication authentication,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		logoutHandler.logout(httpRequest, httpResponse, authentication);
		return ResponseEntity.noContent().build();
	}

	/** 로그인한 회원 정보. 비로그인 시 401 */
	@GetMapping("/api/customers/me")
	public CustomerResponse me(@AuthenticationPrincipal UserDetails user) {
		return service.findByMail(user.getUsername());
	}
}
