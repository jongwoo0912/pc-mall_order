package com.oraclejava.pcproduct.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oraclejava.pcproduct.customer.Customer;
import com.oraclejava.pcproduct.product.PcProduct;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * 주문. 결제는 하지 않고 주문 내용만 저장합니다.
 * (ORDER 는 SQL 예약어라 테이블 이름은 orders)
 */
@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 주문한 고객 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	/** 주문일자 */
	@Column(nullable = false)
	private LocalDateTime orderDate;

	/** 총금액 (주문 시점의 단가 × 수량 합계) */
	@Column(nullable = false)
	private Long totalPrice = 0L;

	/** 주문선 (order_line.order_id 로 연결) */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "order_id", nullable = false)
	@OrderBy("id")
	private List<OrderLine> orderLines = new ArrayList<>();

	/** 받는이 이름 */
	@Column(nullable = false, length = 50)
	private String formName;

	/** 받는이 메일 */
	@Column(nullable = false, length = 100)
	private String formMail;

	/** 받는이 정보 (주소, 연락처, 요청 사항 등) */
	@Column(nullable = false, length = 1000)
	private String formMsg;

	protected Order() {
	}

	public Order(Customer customer, LocalDateTime orderDate, String formName, String formMail, String formMsg) {
		this.customer = customer;
		this.orderDate = orderDate;
		this.formName = formName;
		this.formMail = formMail;
		this.formMsg = formMsg;
	}

	/** 주문선을 추가하고 총금액에 반영 */
	public void addLine(PcProduct product, int quantity) {
		orderLines.add(new OrderLine(product, quantity));
		totalPrice += (long) product.getPrice() * quantity;
	}

	public Long getId() { return id; }
	public Customer getCustomer() { return customer; }
	public LocalDateTime getOrderDate() { return orderDate; }
	public Long getTotalPrice() { return totalPrice; }
	public List<OrderLine> getOrderLines() { return Collections.unmodifiableList(orderLines); }
	public String getFormName() { return formName; }
	public String getFormMail() { return formMail; }
	public String getFormMsg() { return formMsg; }
}
