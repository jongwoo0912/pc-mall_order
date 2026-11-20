package com.oraclejava.pcproduct.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.WebUtils;

import com.oraclejava.pcproduct.order.OrderDtos.CartItemResponse;
import com.oraclejava.pcproduct.order.OrderDtos.CartResponse;
import com.oraclejava.pcproduct.product.PcProduct;
import com.oraclejava.pcproduct.product.PcProductRepository;

import jakarta.servlet.http.HttpSession;

/**
 * 세션 장바구니.
 * 로그인 회원(메일)별로 세션 속성을 따로 두어, 같은 브라우저에서 다른 회원으로
 * 로그인해도 이전 회원의 장바구니가 섞이지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class CartService {

	private static final String ATTR_PREFIX = Cart.class.getName() + ":";

	private final PcProductRepository products;

	public CartService(PcProductRepository products) {
		this.products = products;
	}

	/** 회원의 장바구니 (없으면 새로 만들어 세션에 저장) */
	public Cart cart(HttpSession session, String mail) {
		String key = ATTR_PREFIX + mail;
		synchronized (WebUtils.getSessionMutex(session)) {
			Cart cart = (Cart) session.getAttribute(key);
			if (cart == null) {
				cart = new Cart();
				session.setAttribute(key, cart);
			}
			return cart;
		}
	}

	/** 장바구니 내용 + 현재 단가 기준 합계. 그 사이 삭제된 제품은 장바구니에서 뺌 */
	public CartResponse view(Cart cart) {
		Map<Long, Integer> items = cart.snapshot();
		Map<Long, PcProduct> found = products.findAllById(items.keySet()).stream()
				.collect(Collectors.toMap(PcProduct::getId, Function.identity()));

		List<CartItemResponse> result = new ArrayList<>();
		items.forEach((productId, quantity) -> {
			PcProduct product = found.get(productId);
			if (product == null) {
				cart.remove(productId);
			} else {
				result.add(CartItemResponse.of(product, quantity));
			}
		});
		return CartResponse.of(result);
	}

	public void add(Cart cart, Long productId, int quantity) {
		if (!products.existsById(productId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PcProduct " + productId + " not found");
		}
		cart.add(productId, quantity);
	}

	public void setQuantity(Cart cart, Long productId, int quantity) {
		if (!cart.setQuantity(productId, quantity)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not in cart: " + productId);
		}
	}
}
