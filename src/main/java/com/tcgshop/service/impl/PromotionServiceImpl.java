package com.tcgshop.service.impl;

import com.tcgshop.entity.Product;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.service.PromotionService;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionServiceImpl implements PromotionService {
	// STUB do Phú tạo để các nhánh biên dịch song song. Phong thay nội dung khi làm task PG-16, GIỮ NGUYÊN chữ ký.
	private final ProductRepository productRepository;

	public PromotionServiceImpl(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getEffectivePrice(Long productId) {
		return productRepository.findById(productId)
				.map(Product::getPrice)
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, BigDecimal> getEffectivePrices(Collection<Long> productIds) {
		Iterable<Product> products = productRepository.findAllById(productIds);
		Map<Long, BigDecimal> prices = new HashMap<>();
		for (Product product : products) {
			prices.put(product.getId(), product.getPrice());
		}
		for (Long productId : productIds) {
			if (!prices.containsKey(productId)) {
				throw new ResourceNotFoundException("Sản phẩm", productId);
			}
		}
		return prices;
	}
}
