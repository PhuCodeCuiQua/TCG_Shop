package com.tcgshop.service.impl;

import com.tcgshop.dto.response.CouponSuggestionResponse;
import com.tcgshop.mapper.CouponSuggestionMapper;
import com.tcgshop.repository.CouponSuggestionRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CartService;
import com.tcgshop.service.CouponService;
import com.tcgshop.service.CouponSuggestionService;
import com.tcgshop.util.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponSuggestionServiceImpl implements CouponSuggestionService {
    private final CartService cartService;
    private final CouponService couponService;
    private final CouponSuggestionRepository repository;
    private final CouponSuggestionMapper mapper;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<CouponSuggestionResponse> getSuggestions() {
        Long userId = currentUser.getId();
        var cart = cartService.getCart();
        Map<Long, BigDecimal> subtotals = new LinkedHashMap<>();
        // Chỉ dùng giá và lựa chọn từ giỏ của người đăng nhập; không nhận shop/total từ client.
        for (var shop : cart.shops()) {
            var selected = shop.items().stream().filter(item -> item.selected()).toList();
            if (selected.isEmpty() || selected.stream().anyMatch(item -> !item.available()
                    || item.quantityExceedsStock() || item.unitPrice() == null || item.quantity() <= 0)) continue;
            BigDecimal subtotal = selected.stream()
                    .map(item -> MoneyUtils.round(item.unitPrice()).multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            subtotals.put(shop.shopId(), subtotal);
        }
        if (subtotals.isEmpty()) return List.of();
        List<CouponSuggestionResponse> result = new ArrayList<>();
        for (var coupon : repository.findCandidates(subtotals.keySet(), LocalDateTime.now())) {
            Long shopId = coupon.getShop().getId();
            var validation = couponService.validateAndCalculate(coupon.getCode(), userId, shopId, subtotals.get(shopId));
            if (validation.valid() && validation.discountAmount().signum() > 0) {
                result.add(mapper.toResponse(coupon, validation.discountAmount()));
            }
        }
        result.sort(Comparator.comparing(CouponSuggestionResponse::discountAmount).reversed()
                .thenComparing(CouponSuggestionResponse::id));
        return List.copyOf(result);
    }
}
