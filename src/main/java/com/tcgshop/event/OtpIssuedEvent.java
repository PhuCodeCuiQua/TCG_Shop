package com.tcgshop.event;

import com.tcgshop.entity.enums.OtpPurpose;

public record OtpIssuedEvent(String email, String fullName, String otp, OtpPurpose purpose, int validMinutes) {
}
