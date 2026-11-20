package com.oraclejava.pcproduct.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Screen routes. "/api/pc-products/add" is a literal path, so it takes precedence
 * over the REST controller's "/api/pc-products/{id}" pattern.
 */
@Controller
public class PcProductPageController {

	@GetMapping("/")
	public String list() {
		return "forward:/pc-products/list.html";
	}

	@GetMapping("/api/pc-products/add")
	public String addForm() {
		return "forward:/pc-products/add.html";
	}
}
