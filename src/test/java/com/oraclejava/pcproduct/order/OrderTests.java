package com.oraclejava.pcproduct.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import com.oraclejava.pcproduct.customer.Customer;
import com.oraclejava.pcproduct.customer.CustomerRepository;
import com.oraclejava.pcproduct.product.PcProduct;
import com.oraclejava.pcproduct.product.PcProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
class OrderTests {

	private static final String ORDER_FORM = """
			{"formName":"김받는","formMail":"recv@example.com","formMsg":"서울시 중구 1-1, 010-1234-5678"}""";

	@Autowired MockMvc mvc;
	@Autowired OrderRepository orders;
	@Autowired CustomerRepository customers;
	@Autowired PcProductRepository products;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired TransactionTemplate tx;

	PcProduct thinkPad;
	PcProduct gram;

	@BeforeEach
	void setUp() {
		cleanUp();
		thinkPad = products.save(new PcProduct("ThinkPad X1", 650_000, "business", ""));
		gram = products.save(new PcProduct("LG gram", 900_000, "ultrabook", ""));
	}

	/** 주문이 고객/제품을 참조하므로 주문부터 지움 (다른 테스트 클래스의 deleteAll 과 충돌 방지) */
	@AfterEach
	void cleanUp() {
		orders.deleteAll();
		customers.deleteAll();
		products.deleteAll();
	}

	private MockHttpSession loginAs(String name, String mail) throws Exception {
		customers.save(new Customer(name, passwordEncoder.encode("password123"), mail));
		return (MockHttpSession) mvc.perform(post("/api/customers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"mail":"%s","password":"password123"}""".formatted(mail)))
				.andExpect(status().isOk())
				.andReturn().getRequest().getSession(false);
	}

	private ResultActions addToCart(MockHttpSession session, Long productId, int quantity) throws Exception {
		return mvc.perform(post("/api/cart/items").session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"productId":%d,"quantity":%d}""".formatted(productId, quantity)));
	}

	private ResultActions placeOrder(MockHttpSession session, String body) throws Exception {
		return mvc.perform(post("/api/orders").session(session)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void cartAndOrdersRequireLogin() throws Exception {
		mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ORDER_FORM))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/cart").accept(MediaType.TEXT_HTML))
				.andExpect(redirectedUrl("/login?next=%2Fcart"));
		mvc.perform(get("/order").accept(MediaType.TEXT_HTML))
				.andExpect(redirectedUrl("/login?next=%2Forder"));
	}

	@Test
	void addSameProductTwiceAddsQuantity() throws Exception {
		MockHttpSession session = loginAs("홍길동", "hong@example.com");

		addToCart(session, thinkPad.getId(), 2).andExpect(status().isOk());
		addToCart(session, gram.getId(), 1).andExpect(status().isOk());
		addToCart(session, thinkPad.getId(), 1)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.items[0].productId").value(thinkPad.getId()))
				.andExpect(jsonPath("$.items[0].quantity").value(3))
				.andExpect(jsonPath("$.items[0].subtotal").value(1_950_000))
				.andExpect(jsonPath("$.totalQuantity").value(4))
				.andExpect(jsonPath("$.totalPrice").value(2_850_000));
	}

	@Test
	void changeQuantityAndRemoveItem() throws Exception {
		MockHttpSession session = loginAs("홍길동", "hong@example.com");
		addToCart(session, thinkPad.getId(), 1);
		addToCart(session, gram.getId(), 1);

		mvc.perform(put("/api/cart/items/" + gram.getId()).session(session)
						.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[1].quantity").value(5));
		mvc.perform(put("/api/cart/items/" + gram.getId()).session(session)
						.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
				.andExpect(status().isBadRequest());

		mvc.perform(delete("/api/cart/items/" + thinkPad.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.totalPrice").value(4_500_000));
	}

	@Test
	void addUnknownProductIsNotFound() throws Exception {
		MockHttpSession session = loginAs("홍길동", "hong@example.com");
		addToCart(session, 999_999L, 1).andExpect(status().isNotFound());
	}

	@Test
	void placeOrderSavesOrderAndEmptiesCart() throws Exception {
		MockHttpSession session = loginAs("홍길동", "hong@example.com");
		addToCart(session, thinkPad.getId(), 2);
		addToCart(session, gram.getId(), 1);

		placeOrder(session, ORDER_FORM)
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.totalPrice").value(2_200_000))
				.andExpect(jsonPath("$.formName").value("김받는"))
				.andExpect(jsonPath("$.formMail").value("recv@example.com"))
				.andExpect(jsonPath("$.orderDate").exists())
				.andExpect(jsonPath("$.orderLines", hasSize(2)))
				.andExpect(jsonPath("$.orderLines[0].productName").value("ThinkPad X1"))
				.andExpect(jsonPath("$.orderLines[0].quantity").value(2));

		// 장바구니는 비워짐
		mvc.perform(get("/api/cart").session(session))
				.andExpect(jsonPath("$.items", hasSize(0)));

		// DB 에 고객과 연결되어 저장됨
		tx.executeWithoutResult(s -> {
			assertThat(orders.findAll()).singleElement().satisfies(o -> {
				assertThat(o.getCustomer().getMail()).isEqualTo("hong@example.com");
				assertThat(o.getTotalPrice()).isEqualTo(2_200_000L);
				assertThat(o.getOrderLines()).hasSize(2);
			});
		});

		mvc.perform(get("/api/orders").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void emptyCartCannotBeOrdered() throws Exception {
		MockHttpSession session = loginAs("홍길동", "hong@example.com");
		placeOrder(session, ORDER_FORM).andExpect(status().isBadRequest());
		assertThat(orders.count()).isZero();
	}

	@Test
	void invalidOrderFormKeepsCart() throws Exception {
		MockHttpSession session = loginAs("홍길동", "hong@example.com");
		addToCart(session, thinkPad.getId(), 1);

		placeOrder(session, """
				{"formName":"","formMail":"not-a-mail","formMsg":""}""")
				.andExpect(status().isBadRequest());

		assertThat(orders.count()).isZero();
		mvc.perform(get("/api/cart").session(session))
				.andExpect(jsonPath("$.items", hasSize(1)));
	}

	@Test
	void customersCannotSeeEachOthersOrders() throws Exception {
		MockHttpSession hong = loginAs("홍길동", "hong@example.com");
		addToCart(hong, thinkPad.getId(), 1);
		String location = placeOrder(hong, ORDER_FORM).andReturn().getResponse().getHeader("Location");

		mvc.perform(get(location).session(hong)).andExpect(status().isOk());

		MockHttpSession kim = loginAs("김철수", "kim@example.com");
		mvc.perform(get(location).session(kim)).andExpect(status().isNotFound());
		mvc.perform(get("/api/orders").session(kim)).andExpect(jsonPath("$", hasSize(0)));
		// 장바구니도 회원별로 따로
		mvc.perform(get("/api/cart").session(kim)).andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void orderedProductCannotBeDeleted() throws Exception {
		MockHttpSession session = loginAs("홍길동", "hong@example.com");
		addToCart(session, thinkPad.getId(), 1);
		placeOrder(session, ORDER_FORM).andExpect(status().isCreated());

		mvc.perform(delete("/api/pc-products/" + thinkPad.getId()).with(user("admin@example.com").roles("ADMIN")))
				.andExpect(status().isConflict());
		mvc.perform(delete("/api/pc-products/" + gram.getId()).with(user("admin@example.com").roles("ADMIN")))
				.andExpect(status().isNoContent());
	}
}
