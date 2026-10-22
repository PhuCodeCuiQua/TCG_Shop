package com.tcgshop.service;

import java.math.BigDecimal;

public interface CommissionService {
	BigDecimal getRate(Long shopId);
}
