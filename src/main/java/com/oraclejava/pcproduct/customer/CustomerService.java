package com.oraclejava.pcproduct.customer;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.oraclejava.pcproduct.customer.CustomerDtos.CustomerResponse;
import com.oraclejava.pcproduct.customer.CustomerDtos.SignUpRequest;

/**
 * 회원 가입 + Spring Security 로그인 사용자 조회.
 * 로그인 사용자 이름(username)은 회원 메일 주소입니다.
 */
@Service
@Transactional(readOnly = true)
public class CustomerService implements UserDetailsService {

	private final CustomerRepository repository;
	private final PasswordEncoder passwordEncoder;

	public CustomerService(CustomerRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public CustomerResponse signUp(SignUpRequest req) {
		String mail = normalizeMail(req.mail());
		if (repository.existsByMail(mail)) {
			throw duplicateMail();
		}
		Customer customer = new Customer(req.name().trim(), passwordEncoder.encode(req.password()), mail);
		try {
			return CustomerResponse.from(repository.saveAndFlush(customer));
		} catch (DataIntegrityViolationException e) {
			// 동시 가입으로 unique 제약에 걸린 경우
			throw duplicateMail();
		}
	}

	public CustomerResponse findByMail(String mail) {
		return repository.findByMail(normalizeMail(mail))
				.map(CustomerResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Customer customer = repository.findByMail(normalizeMail(username))
				.orElseThrow(() -> new UsernameNotFoundException("Unknown mail: " + username));
		return User.withUsername(customer.getMail())
				.password(customer.getPassword())
				.roles(customer.getRoleNames().toArray(String[]::new)) // ROLE_USER, ROLE_ADMIN ...
				.build();
	}

	/** 메일은 대소문자 구분 없이 하나의 계정으로 취급 */
	static String normalizeMail(String mail) {
		return mail == null ? "" : mail.trim().toLowerCase(Locale.ROOT);
	}

	private static ResponseStatusException duplicateMail() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 메일입니다.");
	}
}
