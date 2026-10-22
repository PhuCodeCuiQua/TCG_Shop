package com.tcgshop.service.impl;

import com.tcgshop.service.ViewedProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ViewedProductServiceImpl implements ViewedProductService {
	// STUB do Phú tạo để các nhánh biên dịch song song. Phong thay nội dung khi làm task PG-27, GIỮ NGUYÊN chữ ký.
	@Override
	public void record(Long userId, Long productId) {
		log.debug("Stub ghi nhận lượt xem userId={} productId={}", userId, productId);
	}
}
