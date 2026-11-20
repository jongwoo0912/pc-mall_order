package com.oraclejava.pcproduct.order;

import java.time.LocalDateTime;
import java.util.List;

import com.oraclejava.pcproduct.product.PcProduct;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

	private OrderDtos() {
	}

	// ---- 장바구니 ----

	/** 장바구니 담기 (quantity 생략 시 1) */
	public record CartAddRequest(
			@NotNull Long productId,
			@Min(1) @Max(Cart.MAX_QUANTITY) Integer quantity) {
	}

	/** 장바구니 수량 변경 */
	public record CartQuantityRequest(
			@NotNull @Min(1) @Max(Cart.MAX_QUANTITY) Integer quantity) {
	}

	public record CartItemResponse(
			Long productId, String name, Integer price, int quantity, long subtotal, String picSUrl) {

		static CartItemResponse of(PcProduct p, int quantity) {
			String picSUrl = p.getPicS() != null ? "/api/pc-products/" + p.getId() + "/pic-s" : null;
			return new CartItemResponse(p.getId(), p.getName(), p.getPrice(), quantity,
					(long) p.getPrice() * quantity, picSUrl);
		}
	}

	public record CartResponse(List<CartItemResponse> items, int totalQuantity, long totalPrice) {

		static CartResponse of(List<CartItemResponse> items) {
			return new CartResponse(items,
					items.stream().mapToInt(CartItemResponse::quantity).sum(),
					items.stream().mapToLong(CartItemResponse::subtotal).sum());
		}
	}

	// ---- 주문 ----

	/** 주문 폼: 받는이 이름 / 메일 / 정보 */
	public record OrderRequest(
			@NotBlank @Size(max = 50) String formName,
			@NotBlank @Email @Size(max = 100) String formMail,
			@NotBlank @Size(max = 1000) String formMsg) {
	}

	/** price 는 현재 제품 단가 (주문선에는 단가를 따로 저장하지 않음) */
	public record OrderLineResponse(Long id, Long productId, String productName, Integer price, int quantity) {

		static OrderLineResponse from(OrderLine line) {
			PcProduct p = line.getProduct();
			return new OrderLineResponse(line.getId(), p.getId(), p.getName(), p.getPrice(), line.getQuantity());
		}
	}

	public record OrderResponse(
			Long id,
			LocalDateTime orderDate,
			Long totalPrice,
			String formName,
			String formMail,
			String formMsg,
			List<OrderLineResponse> orderLines) {

		static OrderResponse from(Order o) {
			return new OrderResponse(o.getId(), o.getOrderDate(), o.getTotalPrice(),
					o.getFormName(), o.getFormMail(), o.getFormMsg(),
					o.getOrderLines().stream().map(OrderLineResponse::from).toList());
		}
	}
}
