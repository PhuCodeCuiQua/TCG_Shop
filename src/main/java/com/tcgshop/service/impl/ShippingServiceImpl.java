package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ShippingCarrierResponse;
import com.tcgshop.entity.ShippingCarrier;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.mapper.ShippingCarrierMapper;
import com.tcgshop.repository.ShippingCarrierRepository;
import com.tcgshop.service.ShippingService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingServiceImpl implements ShippingService {
	// Bản tối thiểu do Phát thực hiện theo PA-18; đã áp dụng đúng quy tắc phí vận chuyển trong hợp đồng.
	private final ShippingCarrierRepository carrierRepository;
	private final ShippingCarrierMapper carrierMapper;

	public ShippingServiceImpl(ShippingCarrierRepository carrierRepository, ShippingCarrierMapper carrierMapper) {
		this.carrierRepository = carrierRepository;
		this.carrierMapper = carrierMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShippingCarrierResponse> getActiveCarriers() {
		return carrierRepository.findAll().stream()
				.filter(ShippingCarrier::isActive)
				.map(carrierMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal calculateFee(Long carrierId, BigDecimal orderSubtotal) {
		ShippingCarrier carrier = carrierRepository.findById(carrierId)
				.orElseThrow(() -> new BadRequestException("Nhà vận chuyển không tồn tại hoặc đã ngừng hoạt động"));
		if (!carrier.isActive()) {
			throw new BadRequestException("Nhà vận chuyển không tồn tại hoặc đã ngừng hoạt động");
		}

		BigDecimal threshold = carrier.getFreeShipThreshold();
		if (threshold != null && orderSubtotal != null && orderSubtotal.compareTo(threshold) >= 0) {
			return BigDecimal.ZERO;
		}
		return carrier.getBaseFee();
	}
}
