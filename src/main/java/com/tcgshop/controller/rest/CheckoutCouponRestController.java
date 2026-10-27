package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.CouponSuggestionResponse;
import com.tcgshop.service.CouponSuggestionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CheckoutCouponRestController {
    private final CouponSuggestionService couponSuggestionService;

    @GetMapping
    public ResponseEntity<List<CouponSuggestionResponse>> getSuggestions() {
        return ResponseEntity.ok(couponSuggestionService.getSuggestions());
    }
}
