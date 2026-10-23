package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotNull;

public record SelectAllRequest(@NotNull Boolean selected, Long shopId) { }
