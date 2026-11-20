package com.oraclejava.pcproduct.customer;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CustomerDtos {

	private CustomerDtos() {
	}

	/** 회원 가입 요청 */
	public record SignUpRequest(
			@NotBlank @Size(max = 50) String name,
			@NotBlank @Email @Size(max = 100) String mail,
			@NotBlank @Size(min = 8, max = 64) String password) {
	}

	/** 로그인 요청 (사용자 이름 = 메일 주소) */
	public record LoginRequest(
			@NotBlank String mail,
			@NotBlank String password) {
	}

	/** 회원 정보 응답 (비밀번호 제외) */
	public record CustomerResponse(Long id, String name, String mail, List<String> roles) {

		static CustomerResponse from(Customer c) {
			return new CustomerResponse(c.getId(), c.getName(), c.getMail(), List.copyOf(c.getRoleNames()));
		}
	}
}
