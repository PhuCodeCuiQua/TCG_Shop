package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tcgshop.dto.response.*;
import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.Shop;
import com.tcgshop.mapper.CouponSuggestionMapper;
import com.tcgshop.repository.CouponSuggestionRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CartService;
import com.tcgshop.service.CouponService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponSuggestionServiceImplTest {
    @Mock private CartService cartService;
    @Mock private CouponService couponService;
    @Mock private CouponSuggestionRepository repository;
    @Mock private CurrentUser currentUser;
    private CouponSuggestionServiceImpl service() {
        return new CouponSuggestionServiceImpl(cartService, couponService, repository,
                org.mapstruct.factory.Mappers.getMapper(CouponSuggestionMapper.class), currentUser);
    }

    @Test
    void emptyCartReturnsEmptyWithoutQueryingCoupons() {
        when(currentUser.getId()).thenReturn(12L);
        when(cartService.getCart()).thenReturn(new CartResponse(List.of(), 0, 0, BigDecimal.ZERO, false));
        assertThat(service().getSuggestions()).isEmpty();
        verifyNoInteractions(repository, couponService);
    }

    @Test
    void validatesOnlySelectedShopAgainstServerSubtotalAndOmitsInvalidCoupons() {
        when(currentUser.getId()).thenReturn(12L);
        var selected = new CartItemResponse(1L, 2L, "Card", null, null, new BigDecimal("100.5"),
                null, 2, 5, BigDecimal.ZERO, true, true, null, false, false, null);
        var unselected = new CartItemResponse(3L, 4L, "Other", null, null, new BigDecimal("999"),
                null, 1, 5, BigDecimal.ZERO, false, true, null, false, false, null);
        when(cartService.getCart()).thenReturn(new CartResponse(List.of(
                new CartShopGroupResponse(7L, "Shop", List.of(selected), BigDecimal.ZERO, true),
                new CartShopGroupResponse(8L, "Other", List.of(unselected), BigDecimal.ZERO, false)),
                2, 1, BigDecimal.ZERO, false));
        Shop shop = new Shop(); shop.setId(7L);
        Coupon valid = new Coupon(); valid.setId(1L); valid.setShop(shop); valid.setCode("SAVE");
        Coupon invalid = new Coupon(); invalid.setId(2L); invalid.setShop(shop); invalid.setCode("USED");
        when(repository.findCandidates(eq(java.util.Set.of(7L)), any())).thenReturn(List.of(valid, invalid));
        when(couponService.validateAndCalculate("SAVE", 12L, 7L, new BigDecimal("202")))
                .thenReturn(new CouponApplyResponse(true, 1L, BigDecimal.TEN, "ok"));
        when(couponService.validateAndCalculate("USED", 12L, 7L, new BigDecimal("202")))
                .thenReturn(new CouponApplyResponse(false, null, BigDecimal.ZERO, "used"));
        var result = service().getSuggestions();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("SAVE");
        assertThat(result.getFirst().shopId()).isEqualTo(7L);
        assertThat(result.getFirst().discountAmount()).isEqualByComparingTo("10");
    }
}
