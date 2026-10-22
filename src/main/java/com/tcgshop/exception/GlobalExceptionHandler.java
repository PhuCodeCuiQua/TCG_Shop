package com.tcgshop.exception;

import com.tcgshop.dto.response.ApiErrorResponse;
import com.tcgshop.dto.response.FieldErrorResponse;
import com.tcgshop.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String GENERIC_SERVER_MESSAGE = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.";
	private static final String CONFLICT_MESSAGE = "Dữ liệu vừa được thay đổi, vui lòng thử lại";
	private static final String DUPLICATE_MESSAGE = "Dữ liệu bị trùng hoặc đang được sử dụng";
	private static final String UPLOAD_LIMIT_MESSAGE = "Tệp tải lên vượt quá dung lượng cho phép";

	@ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class,
			NoHandlerFoundException.class})
	public Object handleNotFound(Exception exception, HttpServletRequest request) {
		// Spring 6.1+ dùng NoResourceFoundException cho URL không có tài nguyên; bắt riêng để tránh rơi vào lỗi 500 tổng quát.
		String message = exception instanceof ResourceNotFoundException
				? exception.getMessage() : "Không tìm thấy tài nguyên";
		return respond(request, 404, message, null, null, false, exception);
	}

	@ExceptionHandler({BadRequestException.class, HttpMessageNotReadableException.class,
			MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
	public Object handleBadRequest(Exception exception, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		String message = exception instanceof BadRequestException
				? exception.getMessage() : "Dữ liệu gửi lên không hợp lệ";
		return respond(request, 400, message, null, redirectAttributes, true, exception);
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public Object handleBindingErrors(BindException exception, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(GlobalExceptionHandler::toFieldError)
				.toList();
		String message = fieldErrors.isEmpty() ? "Dữ liệu gửi lên không hợp lệ" : fieldErrors.get(0).message();
		return respond(request, 400, message, fieldErrors, redirectAttributes, true, exception);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public Object handleConstraintViolations(ConstraintViolationException exception,
			HttpServletRequest request, RedirectAttributes redirectAttributes) {
		List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations().stream()
				.map(violation -> new FieldErrorResponse(
						violation.getPropertyPath().toString(), violation.getMessage()))
				.toList();
		String message = fieldErrors.isEmpty() ? "Dữ liệu gửi lên không hợp lệ" : fieldErrors.get(0).message();
		return respond(request, 400, message, fieldErrors, redirectAttributes, true, exception);
	}

	@ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
	public Object handleForbidden(Exception exception, HttpServletRequest request) {
		// AccessDeniedException do @PreAuthorize ném ra cần ánh xạ tại đây; nếu rơi vào handler tổng quát sẽ thành 500.
		String message = exception instanceof ForbiddenException
				? exception.getMessage() : "Bạn không có quyền thực hiện thao tác này";
		return respond(request, 403, message, null, null, false, exception);
	}

	@ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
	public Object handleAuthenticationRequired(AuthenticationCredentialsNotFoundException exception,
			HttpServletRequest request) {
		log.warn("HTTP 401 {} {}: authentication is required", request.getMethod(), request.getRequestURI());
		if (RequestUtils.isApiRequest(request)) {
			return apiResponse(request, 401, "Authentication is required", null, null);
		}
		return "redirect:/auth/login";
	}

	@ExceptionHandler({AuthenticationFailedException.class, AccountNotActivatedException.class,
			AccountLockedException.class})
	public Object handleAuthenticationFailure(AuthException exception, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		int status = exception instanceof AuthenticationFailedException ? 401 : 403;
		return respond(request, status, exception.getMessage(), null, redirectAttributes, true, exception);
	}

	@ExceptionHandler(ConflictException.class)
	public Object handleConflict(ConflictException exception, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		return respond(request, 409, CONFLICT_MESSAGE, null, redirectAttributes, true, exception);
	}

	@ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
	public Object handlePersistenceConflict(Exception exception, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		String message = exception instanceof DataIntegrityViolationException
				? DUPLICATE_MESSAGE : CONFLICT_MESSAGE;
		return respond(request, 409, message, null, redirectAttributes, true, exception);
	}

	@ExceptionHandler(BusinessException.class)
	public Object handleBusiness(BusinessException exception, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		return respond(request, 422, exception.getMessage(), null, redirectAttributes, true, exception);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public Object handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return respond(request, 405, "Phương thức yêu cầu không được hỗ trợ", null, null, false, exception);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public Object handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request) {
		return respond(request, 415, "Định dạng nội dung yêu cầu không được hỗ trợ", null, null, false, exception);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public Object handleUploadTooLarge(MaxUploadSizeExceededException exception,
			HttpServletRequest request, RedirectAttributes redirectAttributes) {
		return respond(request, 413, UPLOAD_LIMIT_MESSAGE, null, redirectAttributes, false, exception);
	}

	@ExceptionHandler(Exception.class)
	public Object handleUnexpected(Exception exception, HttpServletRequest request) {
		String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		log.error("Unhandled exception for {} {} traceId={}", request.getMethod(),
				request.getRequestURI(), traceId, exception);
		return respond(request, 500, GENERIC_SERVER_MESSAGE, null, null, false, exception, traceId);
	}

	private Object respond(HttpServletRequest request, int status, String message,
			List<FieldErrorResponse> fieldErrors, RedirectAttributes redirectAttributes,
			boolean redirectOnMutation, Exception exception) {
		return respond(request, status, message, fieldErrors, redirectAttributes, redirectOnMutation, exception, null);
	}

	private Object respond(HttpServletRequest request, int status, String message,
			List<FieldErrorResponse> fieldErrors, RedirectAttributes redirectAttributes,
			boolean redirectOnMutation, Exception exception, String traceId) {
		if (status < 500) {
			log.warn("HTTP {} {} {}: {}", status, request.getMethod(), request.getRequestURI(), message);
		} else if (traceId == null) {
			traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
			log.error("Server error for {} {} traceId={}", request.getMethod(),
					request.getRequestURI(), traceId, exception);
		}

		if (RequestUtils.isApiRequest(request)) {
			return apiResponse(request, status, message, fieldErrors, traceId);
		}
		if (redirectOnMutation && isMutation(request.getMethod()) && redirectAttributes != null) {
			redirectAttributes.addFlashAttribute("errorMessage", message);
			return "redirect:" + safeRefererPath(request);
		}
		return errorView(status, message, traceId);
	}

	private ResponseEntity<ApiErrorResponse> apiResponse(HttpServletRequest request, int status,
			String message, List<FieldErrorResponse> fieldErrors, String traceId) {
		String error = HttpStatus.valueOf(status).getReasonPhrase();
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status, error, message,
				requestPath(request), fieldErrors, traceId);
		return ResponseEntity.status(status).body(body);
	}

	private ModelAndView errorView(int status, String message, String traceId) {
		String view = switch (status) {
			case 403 -> "error/403";
			case 404 -> "error/404";
			case 500 -> "error/500";
			default -> "error/error";
		};
		ModelAndView modelAndView = new ModelAndView(view);
		modelAndView.setStatus(HttpStatus.valueOf(status));
		modelAndView.addObject("status", status);
		modelAndView.addObject("message", message);
		if (traceId != null) modelAndView.addObject("traceId", traceId);
		return modelAndView;
	}

	private String safeRefererPath(HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		if (referer == null || referer.isBlank()) return "/";
		try {
			URI uri = URI.create(referer);
			if (!uri.isAbsolute()) {
				String path = uri.getRawPath();
				return uri.getRawAuthority() == null && isSafeRedirectPath(path)
						? path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()) : "/";
			}
			if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
					&& uri.getUserInfo() == null && uri.getHost() != null
					&& uri.getHost().equalsIgnoreCase(request.getServerName())
					&& effectivePort(uri) == request.getServerPort()) {
				String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
				return isSafeRedirectPath(path)
						? path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()) : "/";
			}
		} catch (IllegalArgumentException exception) {
			log.debug("Ignoring malformed Referer during error redirect");
		}
		return "/";
	}

	private int effectivePort(URI uri) {
		if (uri.getPort() >= 0) return uri.getPort();
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}

	private boolean isSafeRedirectPath(String path) {
		return path != null && path.startsWith("/") && !path.startsWith("//")
				&& !path.contains("\\") && path.indexOf('\r') < 0 && path.indexOf('\n') < 0;
	}

	private boolean isMutation(String method) {
		return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
				|| "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
	}

	private String requestPath(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		return contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)
				? requestUri.substring(contextPath.length()) : requestUri;
	}

	private static FieldErrorResponse toFieldError(FieldError fieldError) {
		return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
	}
}
