package com.oraclejava.pcproduct.customer;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	/** BCrypt 해시 (원문 비밀번호는 저장하지 않음) */
	@Column(nullable = false, length = 100)
	private String password;

	/** 로그인 아이디로 사용 */
	@Column(nullable = false, unique = true, length = 100)
	private String mail;

	/** 부여된 역할 (customer_role 연결 테이블). 비어 있으면 일반 회원 */
	@ManyToMany
	@JoinTable(name = "customer_role",
			joinColumns = @JoinColumn(name = "customer_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	protected Customer() {
	}

	public void addRole(Role role) {
		roles.add(role);
	}

	public void removeRole(Role role) {
		roles.removeIf(r -> r.getName().equals(role.getName()));
	}

	/** 역할 이름 목록. 모든 회원은 USER, 그 외 DB 에 부여된 역할(ADMIN 등) */
	public Set<String> getRoleNames() {
		Set<String> names = new TreeSet<>();
		names.add(Role.USER);
		roles.forEach(r -> names.add(r.getName()));
		return names;
	}

	public Customer(String name, String encodedPassword, String mail) {
		this.name = name;
		this.password = encodedPassword;
		this.mail = mail;
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public String getPassword() { return password; }
	public String getMail() { return mail; }
}
