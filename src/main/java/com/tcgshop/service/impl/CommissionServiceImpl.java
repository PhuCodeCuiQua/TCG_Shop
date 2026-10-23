package com.tcgshop.service.impl;

import com.tcgshop.dto.request.CommissionRateRequest;
import com.tcgshop.dto.response.CommissionSettingResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShopCommissionResponse;
import com.tcgshop.entity.CommissionSetting;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.CommissionSettingMapper;
import com.tcgshop.repository.CommissionSettingRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CommissionService;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionServiceImpl implements CommissionService {
	// Bản tối thiểu do Phát thực hiện theo PA-17; đã áp dụng đúng thứ tự ưu tiên tỉ lệ hoa hồng trong hợp đồng.
	private static final BigDecimal FALLBACK_RATE = new BigDecimal("5.00");
	private final ShopRepository shopRepository;
	private final CommissionSettingRepository settingRepository;
	private final UserRepository userRepository;
	private final CommissionSettingMapper commissionSettingMapper;
	private final CurrentUser currentUser;

	public CommissionServiceImpl(
			ShopRepository shopRepository,
			CommissionSettingRepository settingRepository,
			UserRepository userRepository,
			CommissionSettingMapper commissionSettingMapper,
			CurrentUser currentUser) {
		this.shopRepository = shopRepository;
		this.settingRepository = settingRepository;
		this.userRepository = userRepository;
		this.commissionSettingMapper = commissionSettingMapper;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal getRate(Long shopId) {
		Shop shop = shopRepository.findById(shopId)
				.orElseThrow(() -> new ResourceNotFoundException("Cửa hàng", shopId));
		if (shop.getCommissionRate() != null) {
			return shop.getCommissionRate();
		}
		return findDefaultRate();
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public BigDecimal getDefaultRate() {
		requireAdmin();
		return findDefaultRate();
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public PageResponse<CommissionSettingResponse> getSettingsHistory(Pageable pageable) {
		requireAdmin();
		return PageResponse.from(
				settingRepository.findAllByOrderByCreatedAtDesc(pageable),
				commissionSettingMapper::toSettingResponse);
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public PageResponse<ShopCommissionResponse> getShopCommissionRates(Pageable pageable) {
		requireAdmin();
		BigDecimal defaultRate = findDefaultRate();
		return PageResponse.from(
				shopRepository.findAllNotDeleted(pageable),
				shop -> commissionSettingMapper.toShopResponse(shop, defaultRate));
	}

	@Override
	@Transactional(readOnly = true)
	@PreAuthorize("hasRole('ADMIN')")
	public ShopCommissionResponse getShopCommissionRate(Long shopId) {
		requireAdmin();
		Shop shop = findActiveShop(shopId);
		return commissionSettingMapper.toShopResponse(shop, findDefaultRate());
	}

	@Override
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public void addDefaultRate(CommissionRateRequest request) {
		requireAdmin();
		BigDecimal rate = validateRate(request);
		User actor = findCurrentUser();

		CommissionSetting setting = new CommissionSetting();
		setting.setDefaultRate(rate);
		setting.setUpdatedBy(actor);
		settingRepository.save(setting);
	}

	@Override
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public void setShopRate(Long shopId, CommissionRateRequest request) {
		requireAdmin();
		BigDecimal rate = validateRate(request);
		Shop shop = findActiveShop(shopId);
		shop.setCommissionRate(rate);
	}

	@Override
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public void resetShopRate(Long shopId) {
		requireAdmin();
		Shop shop = findActiveShop(shopId);
		shop.setCommissionRate(null);
	}

	private BigDecimal findDefaultRate() {
		// TODO(PA-17): Lọc effective_from <= ngày hiện tại sau khi Phú bổ sung cột vào CommissionSetting.
		return settingRepository.findTopByOrderByCreatedAtDesc()
				.map(CommissionSetting::getDefaultRate)
				.orElse(FALLBACK_RATE);
	}

	private BigDecimal validateRate(CommissionRateRequest request) {
		if (request == null || request.getRate() == null) {
			throw new BadRequestException("Vui lòng nhập tỷ lệ hoa hồng");
		}
		BigDecimal rate = request.getRate();
		if (rate.signum() < 0 || rate.compareTo(new BigDecimal("100.00")) > 0
				|| rate.stripTrailingZeros().scale() > 2) {
			throw new BadRequestException("Tỷ lệ hoa hồng phải từ 0% đến 100%, tối đa 2 chữ số thập phân");
		}
		return rate;
	}

	private User findCurrentUser() {
		Long userId = currentUser.getId();
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
	}

	private Shop findActiveShop(Long shopId) {
		Shop shop = shopRepository.findById(shopId)
				.orElseThrow(() -> new ResourceNotFoundException("Cửa hàng", shopId));
		if (Boolean.TRUE.equals(shop.getDeleted())) {
			throw new ResourceNotFoundException("Cửa hàng", shopId);
		}
		return shop;
	}

	private void requireAdmin() {
		if (!currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Chỉ Admin được quản lý tỷ lệ hoa hồng");
		}
	}
}
