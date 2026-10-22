package com.tcgshop.service.impl;

import com.tcgshop.entity.CommissionSetting;
import com.tcgshop.entity.Shop;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.CommissionSettingRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.service.CommissionService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionServiceImpl implements CommissionService {
	// Bản tối thiểu do Phát thực hiện theo PA-17; đã áp dụng đúng thứ tự ưu tiên tỉ lệ hoa hồng trong hợp đồng.
	private static final BigDecimal FALLBACK_RATE = new BigDecimal("5.00");
	private final ShopRepository shopRepository;
	private final CommissionSettingRepository settingRepository;

	public CommissionServiceImpl(ShopRepository shopRepository, CommissionSettingRepository settingRepository) {
		this.shopRepository = shopRepository;
		this.settingRepository = settingRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getRate(Long shopId) {
		Shop shop = shopRepository.findById(shopId)
				.orElseThrow(() -> new ResourceNotFoundException("Cửa hàng", shopId));
		if (shop.getCommissionRate() != null) {
			return shop.getCommissionRate();
		}
		return settingRepository.findTopByOrderByCreatedAtDesc()
				.map(CommissionSetting::getDefaultRate)
				.orElse(FALLBACK_RATE);
	}
}
