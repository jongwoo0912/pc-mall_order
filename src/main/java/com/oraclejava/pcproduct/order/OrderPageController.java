package com.oraclejava.pcproduct.order;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 장바구니 / 주문서 / 주문 내역 화면 (로그인 필요, 비로그인 시 로그인 화면으로 이동) */
@Controller
public class OrderPageController {

	@GetMapping("/cart")
	public String cart() {
		return "forward:/orders/cart.html";
	}

	@GetMapping("/order")
	public String orderForm() {
		return "forward:/orders/order.html";
	}

	@GetMapping("/orders")
	public String orders() {
		return "forward:/orders/list.html";
	}
}
