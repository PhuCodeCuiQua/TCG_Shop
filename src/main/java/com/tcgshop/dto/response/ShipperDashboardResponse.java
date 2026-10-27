package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record ShipperDashboardResponse(long ordersToDeliver, long deliveredToday, BigDecimal weeklyCodIncome) { }
