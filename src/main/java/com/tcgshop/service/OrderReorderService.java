package com.tcgshop.service;

import com.tcgshop.dto.response.ReorderResponse;

public interface OrderReorderService {
    ReorderResponse reorder(Long orderId);
}
