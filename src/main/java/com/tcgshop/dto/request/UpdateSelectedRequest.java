package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSelectedRequest(@NotNull Boolean selected) { }
