package com.tcgshop.service.impl;

import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.response.ReorderResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.service.CartService;
import com.tcgshop.service.OrderReorderService;
import com.tcgshop.service.UserOrderService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderReorderServiceImpl implements OrderReorderService {
    private final UserOrderService userOrderService;
    private final CartService cartService;

    @Override
    @PreAuthorize("hasRole('USER')")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReorderResponse reorder(Long orderId) {
        // getMyOrder lấy người mua từ CurrentUser, trả 404 nếu đơn không thuộc người đang đăng nhập.
        var order = userOrderService.getMyOrder(orderId);
        if (order.status() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể mua lại đơn hàng đã giao thành công");
        }
        int added = 0;
        List<String> skipped = new ArrayList<>();
        for (var item : order.items()) {
            if (item.productId() == null || item.quantity() == null || item.quantity() < 1 || item.quantity() > 99) {
                skipped.add(item.productName() + ": số lượng hoặc sản phẩm không còn hợp lệ");
                continue;
            }
            try {
                // Mỗi lời gọi CartService có transaction riêng: một sản phẩm hết hàng không rollback các sản phẩm đã thêm.
                // CartService kiểm tra trạng thái bán, tồn kho gồm lượng đã có trong giỏ và lấy giá hiện tại từ DB.
                cartService.addItem(new AddToCartRequest(item.productId(), item.quantity()));
                added++;
            } catch (BadRequestException | ResourceNotFoundException exception) {
                skipped.add(item.productName() + ": " + exception.getMessage());
            }
        }
        return new ReorderResponse(added, skipped);
    }
}
