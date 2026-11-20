package com.oraclejava.pcproduct.order;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oraclejava.pcproduct.order.OrderDtos.CartAddRequest;
import com.oraclejava.pcproduct.order.OrderDtos.CartQuantityRequest;
import com.oraclejava.pcproduct.order.OrderDtos.CartResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** 장바구니 API (로그인 회원만). 모든 응답은 변경 후의 장바구니 내용 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

	private final CartService service;

	public CartController(CartService service) {
		this.service = service;
	}

	@GetMapping
	public CartResponse get(@AuthenticationPrincipal UserDetails user, HttpSession session) {
		return service.view(cart(user, session));
	}

	/** 담기 (JSON: productId, quantity) — 이미 담긴 제품이면 수량을 더함 */
	@PostMapping("/items")
	public CartResponse add(@AuthenticationPrincipal UserDetails user, HttpSession session,
			@Valid @RequestBody CartAddRequest request) {
		Cart cart = cart(user, session);
		service.add(cart, request.productId(), request.quantity() == null ? 1 : request.quantity());
		return service.view(cart);
	}

	/** 수량 변경 (JSON: quantity) */
	@PutMapping("/items/{productId}")
	public CartResponse setQuantity(@AuthenticationPrincipal UserDetails user, HttpSession session,
			@PathVariable Long productId, @Valid @RequestBody CartQuantityRequest request) {
		Cart cart = cart(user, session);
		service.setQuantity(cart, productId, request.quantity());
		return service.view(cart);
	}

	@DeleteMapping("/items/{productId}")
	public CartResponse remove(@AuthenticationPrincipal UserDetails user, HttpSession session,
			@PathVariable Long productId) {
		Cart cart = cart(user, session);
		cart.remove(productId);
		return service.view(cart);
	}

	/** 비우기 */
	@DeleteMapping
	public CartResponse clear(@AuthenticationPrincipal UserDetails user, HttpSession session) {
		Cart cart = cart(user, session);
		cart.clear();
		return service.view(cart);
	}

	private Cart cart(UserDetails user, HttpSession session) {
		return service.cart(session, user.getUsername());
	}
}
