package com.tcgshop.service.impl;

import com.tcgshop.dto.response.CouponApplyResponse;
import com.tcgshop.service.CouponService;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CouponServiceImpl implements CouponService {
	// STUB do Phú tạo để các nhánh biên dịch song song. Phong thay nội dung khi làm task PG-17, GIỮ NGUYÊN chữ ký.
	@Override
	public CouponApplyResponse validateAndCalculate(String code, Long userId, Long shopId, BigDecimal subtotal) {
		return new CouponApplyResponse(false, null, BigDecimal.ZERO,
				"Chức năng mã giảm giá chưa sẵn sàng");
	}

	@Override
	public void recordUsage(Long couponId, Long userId, Long orderId, BigDecimal discountAmount) {
		log.debug("Stub ghi nhận sử dụng couponId={} userId={} orderId={}", couponId, userId, orderId);
	}
}
