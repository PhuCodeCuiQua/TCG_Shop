package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record TopShopResponse(Long shopId, String shopName, BigDecimal revenue, long orderCount) {
}