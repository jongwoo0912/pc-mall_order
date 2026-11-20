package com.oraclejava.pcproduct.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	/** 고객의 주문 목록 (최근 주문 먼저, 주문선/제품까지 한 번에 조회) */
	@EntityGraph(attributePaths = { "orderLines", "orderLines.product" })
	List<Order> findByCustomerMailOrderByOrderDateDescIdDesc(String mail);

	/** 본인 주문만 조회 */
	@EntityGraph(attributePaths = { "orderLines", "orderLines.product" })
	Optional<Order> findByIdAndCustomerMail(Long id, String mail);
}
