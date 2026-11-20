package com.oraclejava.pcproduct.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 회원 역할. name 은 'ADMIN' 처럼 접두어 없이 저장하고,
 * Spring Security 권한으로는 'ROLE_ADMIN' 으로 사용합니다.
 */
@Entity
@Table(name = "role")
public class Role {

	public static final String ADMIN = "ADMIN";
	/** 모든 회원이 기본으로 갖는 역할 (DB 에 저장하지 않음) */
	public static final String USER = "USER";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String name;

	protected Role() {
	}

	public Role(String name) {
		this.name = name;
	}

	public Long getId() { return id; }
	public String getName() { return name; }
}
