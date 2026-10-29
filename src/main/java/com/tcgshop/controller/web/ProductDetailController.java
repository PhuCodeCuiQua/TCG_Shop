package com.tcgshop.controller.web;

import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ProductDetailService;
import com.tcgshop.service.ViewedProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductDetailController {
	private static final Logger log = LoggerFactory.getLogger(ProductDetailController.class);

	private final ProductDetailService detailService;
	private final ViewedProductService viewedProductService;
	private final CurrentUser currentUser;

	public ProductDetailController(ProductDetailService detailService,
			ViewedProductService viewedProductService, CurrentUser currentUser) {
		this.detailService = detailService;
		this.viewedProductService = viewedProductService;
		this.currentUser = currentUser;
	}

	@GetMapping("/products/{id}")
	public String detail(@PathVariable Long id, Model model) {
		// Ném ResourceNotFoundException (404) trước khi ghi lịch sử xem.
		model.addAttribute("detail", detailService.getDetail(id));

		if (currentUser.isAuthenticated() && currentUser.hasRole(RoleName.ROLE_USER)) {
			try {
				viewedProductService.record(currentUser.getId(), id);
			} catch (RuntimeException e) {
				log.warn("Không ghi được sản phẩm đã xem {}: {}", id, e.getMessage());
			}
		}

		// TODO(PG-29): thêm reviews và canReview vào model khi ReviewService của Phong được merge.
		return "guest/product-detail";
	}
}