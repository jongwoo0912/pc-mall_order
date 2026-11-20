package com.oraclejava.pcproduct.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.oraclejava.pcproduct.customer.Customer;
import com.oraclejava.pcproduct.customer.CustomerRepository;
import com.oraclejava.pcproduct.order.OrderDtos.OrderRequest;
import com.oraclejava.pcproduct.order.OrderDtos.OrderResponse;
import com.oraclejava.pcproduct.product.PcProduct;
import com.oraclejava.pcproduct.product.PcProductRepository;

/** 주문 처리. 결제는 하지 않고 주문 내용만 저장합니다. */
@Service
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orders;
	private final CustomerRepository customers;
	private final PcProductRepository products;

	public OrderService(OrderRepository orders, CustomerRepository customers, PcProductRepository products) {
		this.orders = orders;
		this.customers = customers;
		this.products = products;
	}

	/**
	 * 장바구니 항목(제품 ID → 수량)과 주문 폼으로 주문을 저장.
	 * - 장바구니가 비어 있으면 400
	 * - 장바구니에 그 사이 삭제된 제품이 있으면 409
	 */
	@Transactional
	public OrderResponse placeOrder(String mail, Map<Long, Integer> items, OrderRequest form) {
		if (items.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다.");
		}
		Customer customer = customers.findByMail(mail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer not found"));

		Map<Long, PcProduct> found = products.findAllById(items.keySet()).stream()
				.collect(Collectors.toMap(PcProduct::getId, Function.identity()));
		if (found.size() != items.size()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "장바구니에 판매가 중단된 제품이 있습니다.");
		}

		Order order = new Order(customer, LocalDateTime.now(),
				form.formName().trim(), form.formMail().trim(), form.formMsg().trim());
		items.forEach((productId, quantity) -> order.addLine(found.get(productId), quantity));
		return OrderResponse.from(orders.save(order));
	}

	/** 내 주문 목록 (최근 주문 먼저) */
	public List<OrderResponse> findMine(String mail) {
		return orders.findByCustomerMailOrderByOrderDateDescIdDesc(mail).stream()
				.map(OrderResponse::from).toList();
	}

	/** 내 주문 한 건. 다른 회원의 주문이면 404 */
	public OrderResponse findMine(String mail, Long id) {
		return orders.findByIdAndCustomerMail(id, mail)
				.map(OrderResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
	}
}
