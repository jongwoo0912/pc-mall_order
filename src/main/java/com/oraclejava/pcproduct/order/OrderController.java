package com.oraclejava.pcproduct.order;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oraclejava.pcproduct.order.OrderDtos.OrderRequest;
import com.oraclejava.pcproduct.order.OrderDtos.OrderResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** 주문 API (로그인 회원만). 실제 결제는 이뤄지지 않습니다. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;
	private final CartService cartService;

	public OrderController(OrderService orderService, CartService cartService) {
		this.orderService = orderService;
		this.cartService = cartService;
	}

	/**
	 * 장바구니 내용으로 주문 (JSON: formName, formMail, formMsg).
	 * 성공 시 201 + 주문 내용, 장바구니는 비워짐. 입력 오류(400)·실패 시 장바구니는 그대로.
	 */
	@PostMapping
	public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal UserDetails user, HttpSession session,
			@Valid @RequestBody OrderRequest request) {
		Cart cart = cartService.cart(session, user.getUsername());
		Map<Long, Integer> items = cart.snapshot();
		OrderResponse created = orderService.placeOrder(user.getUsername(), items, request);
		// 주문이 커밋된 뒤에 장바구니를 비움
		cart.clear();
		return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
	}

	/** 내 주문 목록 */
	@GetMapping
	public List<OrderResponse> list(@AuthenticationPrincipal UserDetails user) {
		return orderService.findMine(user.getUsername());
	}

	/** 내 주문 한 건 (다른 회원의 주문은 404) */
	@GetMapping("/{id}")
	public OrderResponse get(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
		return orderService.findMine(user.getUsername(), id);
	}
}
