package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.PaymentStatus;

public record MockMomoPaymentResponse(PaymentStatus paymentStatus, String redirectUrl) { }
