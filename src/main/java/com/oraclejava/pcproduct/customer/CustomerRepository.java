package com.oraclejava.pcproduct.customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	/** 역할(roles)까지 한 번에 조회 */
	@EntityGraph(attributePaths = "roles")
	Optional<Customer> findByMail(String mail);

	boolean existsByMail(String mail);
}
