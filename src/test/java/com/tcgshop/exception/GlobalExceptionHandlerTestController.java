package com.tcgshop.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Các endpoint lỗi này chỉ tồn tại trong test để kiểm tra GlobalExceptionHandler. */
@RestController
class GlobalExceptionHandlerTestController {
	@GetMapping({"/test/not-found", "/api/test/not-found"})
	String notFound() {
		throw new ResourceNotFoundException("Sản phẩm không tồn tại");
	}

	@GetMapping({"/test/forbidden", "/api/test/forbidden"})
	String forbidden() {
		throw new ForbiddenException("Không được phép truy cập");
	}

	@GetMapping({"/test/access-denied", "/api/test/access-denied"})
	String accessDenied() {
		throw new AccessDeniedException("Denied internally");
	}

	@RequestMapping(path = {"/test/bad-request", "/api/test/bad-request"}, method = {RequestMethod.GET, RequestMethod.POST})
	String badRequest() {
		throw new BadRequestException("Dữ liệu không hợp lệ");
	}

	@GetMapping({"/test/conflict", "/api/test/conflict"})
	String conflict() {
		throw new ConflictException("Thông tin bị xung đột");
	}

	@RequestMapping(path = {"/test/business", "/api/test/business"}, method = {RequestMethod.GET, RequestMethod.POST})
	String business() {
		throw new BusinessException("Không thể thực hiện nghiệp vụ");
	}

	@GetMapping({"/test/optimistic-lock", "/api/test/optimistic-lock"})
	String optimisticLock() {
		throw new ObjectOptimisticLockingFailureException("Product", 7L);
	}

	@GetMapping({"/test/unexpected", "/api/test/unexpected"})
	String unexpected() {
		throw new IllegalStateException("internal stack detail");
	}

	@PostMapping({"/test/validate", "/api/test/validate"})
	String validate(@Valid @RequestBody ValidationRequest request) {
		return "ok";
	}

	record ValidationRequest(@NotBlank(message = "Tên không được để trống") String name) {
	}
}
