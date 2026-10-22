package com.tcgshop.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
		@JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldErrorResponse> fieldErrors,
		String traceId) { }
