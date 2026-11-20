package com.oraclejava.pcproduct.customer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 로그인 / 회원 가입 화면 */
@Controller
public class CustomerPageController {

	@GetMapping("/login")
	public String login() {
		return "forward:/customers/login.html";
	}

	@GetMapping("/signup")
	public String signup() {
		return "forward:/customers/signup.html";
	}
}
