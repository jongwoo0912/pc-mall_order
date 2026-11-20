package com.oraclejava.pcproduct.order;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 장바구니 (HTTP 세션에 보관). 제품 ID → 수량, 담은 순서 유지.
 * 같은 세션의 여러 요청이 동시에 접근할 수 있어 메서드를 동기화합니다.
 */
public class Cart implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 제품 하나당 최대 수량 */
	public static final int MAX_QUANTITY = 99;

	private final LinkedHashMap<Long, Integer> items = new LinkedHashMap<>();

	/** 담기: 이미 있으면 수량을 더함 (최대 MAX_QUANTITY) */
	public synchronized void add(Long productId, int quantity) {
		items.merge(productId, quantity, (a, b) -> Math.min(a + b, MAX_QUANTITY));
	}

	/** 수량 변경. 장바구니에 없는 제품이면 false */
	public synchronized boolean setQuantity(Long productId, int quantity) {
		return items.replace(productId, quantity) != null;
	}

	public synchronized void remove(Long productId) {
		items.remove(productId);
	}

	public synchronized void clear() {
		items.clear();
	}

	/** 현재 내용의 복사본 */
	public synchronized Map<Long, Integer> snapshot() {
		return new LinkedHashMap<>(items);
	}
}
