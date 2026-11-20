package com.oraclejava.pcproduct.order;

import com.oraclejava.pcproduct.product.PcProduct;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 주문선: 주문 한 건에 들어 있는 제품 한 종류와 수량 */
@Entity
@Table(name = "order_line")
public class OrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 제품 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pc_product_id", nullable = false)
	private PcProduct product;

	/** 수량 */
	@Column(nullable = false)
	private int quantity;

	protected OrderLine() {
	}

	OrderLine(PcProduct product, int quantity) {
		this.product = product;
		this.quantity = quantity;
	}

	public Long getId() { return id; }
	public PcProduct getProduct() { return product; }
	public int getQuantity() { return quantity; }
}
