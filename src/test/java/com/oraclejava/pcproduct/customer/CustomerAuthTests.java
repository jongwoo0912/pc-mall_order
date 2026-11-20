package com.oraclejava.pcproduct.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerAuthTests {

	@Autowired MockMvc mvc;
	@Autowired CustomerRepository repository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired RoleRepository roleRepository;
	@Autowired TransactionTemplate tx;

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	private void signUp(String name, String mail, String password) throws Exception {
		mvc.perform(post("/api/customers")						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","mail":"%s","password":"%s"}""".formatted(name, mail, password)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.mail").value(mail.toLowerCase()))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void signUpStoresHashedPassword() throws Exception {
		signUp("홍길동", "Hong@Example.com", "password123");

		Customer saved = repository.findByMail("hong@example.com").orElseThrow();
		assertThat(saved.getName()).isEqualTo("홍길동");
		assertThat(saved.getPassword()).isNotEqualTo("password123");
		assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
	}

	@Test
	void duplicateMailIsRejected() throws Exception {
		signUp("홍길동", "hong@example.com", "password123");

		mvc.perform(post("/api/customers")						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"다른사람","mail":"HONG@example.com","password":"password456"}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void invalidSignUpIsRejected() throws Exception {
		mvc.perform(post("/api/customers")						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"","mail":"not-a-mail","password":"short"}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void signUpWithoutCsrfTokenSucceeds() throws Exception {
		// CSRF 보호를 껐으므로 토큰 없이도 가입 가능
		signUp("홍길동", "hong@example.com", "password123");
	}

	private ResultActions login(String mail, String password) throws Exception {
		return mvc.perform(post("/api/customers/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"mail":"%s","password":"%s"}""".formatted(mail, password)));
	}

	@Test
	void loginWithMailSucceedsAndSessionKeepsLogin() throws Exception {
		signUp("홍길동", "hong@example.com", "password123");

		MvcResult result = login("HONG@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("홍길동"))
				.andExpect(jsonPath("$.mail").value("hong@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();

		// 같은 세션으로 다음 요청 → 로그인 상태 유지
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		mvc.perform(get("/api/customers/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mail").value("hong@example.com"));

		// 로그아웃 후에는 401
		mvc.perform(post("/api/customers/logout").session(session))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/customers/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithWrongPasswordFails() throws Exception {
		signUp("홍길동", "hong@example.com", "password123");

		login("hong@example.com", "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(unauthenticated());
		login("nobody@example.com", "password123")
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithEmptyBodyIsBadRequest() throws Exception {
		mvc.perform(post("/api/customers/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"mail":"","password":""}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void meRequiresLogin() throws Exception {
		mvc.perform(get("/api/customers/me")).andExpect(status().isUnauthorized());

		signUp("홍길동", "hong@example.com", "password123");
		mvc.perform(get("/api/customers/me").with(user("hong@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("홍길동"))
				.andExpect(jsonPath("$.mail").value("hong@example.com"));
	}

	@Test
	void productReadIsPublicButWriteNeedsLogin() throws Exception {
		mvc.perform(get("/api/pc-products")).andExpect(status().isOk());

		String body = """
				{"name":"ThinkPad","price":650000,"kind":"business","text":""}""";
		mvc.perform(post("/api/pc-products")						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
		// 일반 회원은 403, 관리자만 등록 가능
		mvc.perform(post("/api/pc-products").with(user("hong@example.com").roles("USER"))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(post("/api/pc-products").with(user("admin@example.com").roles("USER", "ADMIN"))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	@Test
	void adminRoleIsSeeded() {
		assertThat(roleRepository.findByName(Role.ADMIN)).isPresent();
	}

	@Test
	void newCustomerIsUserOnly() throws Exception {
		signUp("홍길동", "hong@example.com", "password123");

		login("hong@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles", contains("USER")));
	}

	@Test
	void customerWithAdminRoleCanManageProducts() throws Exception {
		signUp("관리자", "admin@example.com", "password123");
		grantAdmin("admin@example.com");

		MvcResult result = login("admin@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles", containsInAnyOrder("ADMIN", "USER")))
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		mvc.perform(post("/api/pc-products").session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"ThinkPad","price":650000,"kind":"business","text":""}"""))
				.andExpect(status().isCreated());
		mvc.perform(get("/api/pc-products/add").session(session).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk());
	}

	@Test
	void regularCustomerCannotOpenAddPage() throws Exception {
		mvc.perform(get("/api/pc-products/add").with(user("hong@example.com").roles("USER"))
						.accept(MediaType.TEXT_HTML))
				.andExpect(status().isForbidden());
	}

	private void grantAdmin(String mail) {
		tx.executeWithoutResult(s -> {
			Customer c = repository.findByMail(mail).orElseThrow();
			c.addRole(roleRepository.findByName(Role.ADMIN).orElseThrow());
		});
	}

	@Test
	void addPageRedirectsToLogin() throws Exception {
		// 브라우저처럼 HTML 을 요청하면 로그인 페이지로 이동
		mvc.perform(get("/api/pc-products/add").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?next=%2Fapi%2Fpc-products%2Fadd"));
	}

	@Test
	void loginAndSignupPagesArePublic() throws Exception {
		mvc.perform(get("/login")).andExpect(status().isOk());
		mvc.perform(get("/signup")).andExpect(status().isOk());
	}
}
