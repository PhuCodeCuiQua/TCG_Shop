package com.tcgshop.service.impl;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.dto.request.CreateReturnRequest;
import com.tcgshop.dto.request.EscalateReturnRequest;
import com.tcgshop.dto.response.ReturnRequestInfoResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.ReturnRequest;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnReason;
import com.tcgshop.entity.enums.ReturnStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.event.ReturnEscalationRejectedEvent;
import com.tcgshop.event.ReturnRequestEscalatedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.ReturnRequestRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.ReturnRequestService;
import com.tcgshop.service.UploadResult;
import com.tcgshop.util.ReturnStatusUi;
import com.tcgshop.util.TransactionUtils;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ReturnRequestServiceImpl implements ReturnRequestService {
	private static final String RETURN_UPLOAD_FOLDER = "tcgshop/returns";
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final OrderRepository orderRepository;
	private final ReturnRequestRepository returnRequestRepository;
	private final CurrentUser currentUser;
	private final ReturnWindowPolicy returnWindowPolicy;
	private final OrderProperties orderProperties;
	private final OrderService orderService;
	private final CloudinaryService cloudinaryService;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	public ReturnRequestServiceImpl(OrderRepository orderRepository,
			ReturnRequestRepository returnRequestRepository, CurrentUser currentUser,
			ReturnWindowPolicy returnWindowPolicy, OrderProperties orderProperties,
			OrderService orderService, CloudinaryService cloudinaryService,
			ApplicationEventPublisher eventPublisher, Clock clock) {
		this.orderRepository = orderRepository;
		this.returnRequestRepository = returnRequestRepository;
		this.currentUser = currentUser;
		this.returnWindowPolicy = returnWindowPolicy;
		this.orderProperties = orderProperties;
		this.orderService = orderService;
		this.cloudinaryService = cloudinaryService;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
	}

	@Override
	public ReturnRequestInfoResponse createReturnRequest(Long orderId, CreateReturnRequest form,
			MultipartFile image, MultipartFile video) {
		Long userId = currentUser.getId();
		// 1. Khóa đơn thuộc người đang đăng nhập trước để hai yêu cầu đồng thời không cùng vượt qua kiểm tra trùng.
		Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		// 2. Kiểm tra trùng trước các bước khác; unique constraint vẫn bảo vệ trường hợp cạnh tranh ngoài dự kiến.
		if (returnRequestRepository.existsByOrderId(orderId)) {
			throw new BusinessException("Đơn hàng này đã có yêu cầu trả hàng");
		}
		// 3. Chỉ đơn đã giao mới được yêu cầu trả hàng.
		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new BusinessException("Chỉ đơn hàng đã giao mới có thể yêu cầu trả hàng");
		}
		// 4. Dùng chung chính sách thời hạn với state machine và trang chi tiết đơn.
		if (!returnWindowPolicy.isWithinWindow(order)) {
			throw new BusinessException("Đơn hàng đã quá thời hạn đổi trả");
		}
		// 5. Kiểm tra tệp bắt buộc trước khi tạo bất kỳ tài nguyên Cloudinary nào.
		if (image == null || image.isEmpty()) {
			throw new BadRequestException("Vui lòng đính kèm ít nhất một ảnh bằng chứng");
		}

		// 6. Mỗi lần upload được đăng ký bù trừ ngay lập tức để rollback DB không để lại tệp mồ côi.
		UploadResult imageUpload = cloudinaryService.uploadImage(image, RETURN_UPLOAD_FOLDER);
		TransactionUtils.runAfterRollback(() -> cloudinaryService.delete(imageUpload.publicId(), "image"));
		UploadResult videoUpload = null;
		if (video != null && !video.isEmpty()) {
			UploadResult uploadedVideo = cloudinaryService.uploadVideo(video, RETURN_UPLOAD_FOLDER);
			videoUpload = uploadedVideo;
			TransactionUtils.runAfterRollback(() -> cloudinaryService.delete(uploadedVideo.publicId(), "video"));
		}

		// 7. Chuẩn hóa phần mô tả để ghi chú lịch sử ngắn gọn, dễ đọc.
		String note = form.getReason().getLabel() + ": " + normalizeWhitespace(form.getDetail());
		// 8. Đổi trạng thái qua OrderService để giữ nguyên phân quyền, history và event của state machine.
		orderService.changeStatus(orderId, OrderStatus.RETURN_REQUESTED, note);

		// 9. Lưu yêu cầu sau khi chuyển trạng thái; mọi thao tác vẫn nằm trong cùng transaction.
		ReturnRequest entity = new ReturnRequest();
		entity.setOrder(order);
		entity.setUser(order.getUser());
		entity.setReason(note);
		entity.setProofImageUrl(imageUpload.url());
		entity.setProofImagePublicId(imageUpload.publicId());
		if (videoUpload != null) {
			entity.setProofVideoUrl(videoUpload.url());
			entity.setProofVideoPublicId(videoUpload.publicId());
		}
		entity.setStatus(ReturnStatus.PENDING);
		return toResponse(returnRequestRepository.save(entity));
	}

	@Override
	public void escalate(Long orderId, EscalateReturnRequest form) {
		Long userId = currentUser.getId();
		Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		ReturnRequest request = returnRequestRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Yêu cầu trả hàng", orderId));
		if (order.getStatus() != OrderStatus.RETURN_REJECTED) {
			throw new BusinessException("Chỉ có thể khiếu nại khi yêu cầu bị từ chối");
		}
		if (request.getStatus() != ReturnStatus.REJECTED || hasText(request.getAdminNote())) {
			throw new BusinessException("Yêu cầu này đã được khiếu nại");
		}
		LocalDateTime decidedAt = request.getDecidedAt();
		LocalDateTime deadline = decidedAt == null ? null : decidedAt.plusDays(orderProperties.escalationWindowDays());
		if (deadline == null || LocalDateTime.now(clock).isAfter(deadline)) {
			throw new BusinessException("Đã quá thời hạn khiếu nại ("
					+ orderProperties.escalationWindowDays() + " ngày)");
		}
		request.setStatus(ReturnStatus.ESCALATED);
		request.setEscalationReason(normalizeWhitespace(form.getDetail()));
		Long shopId = order.getShop() == null ? null : order.getShop().getId();
		Long shopOwnerId = order.getShop() == null || order.getShop().getOwner() == null
				? null : order.getShop().getOwner().getId();
		eventPublisher.publishEvent(new ReturnRequestEscalatedEvent(order.getId(), order.getOrderCode(),
				order.getUser() == null ? null : order.getUser().getId(), shopId, shopOwnerId));
	}

	@Override
	@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
	public void rejectEscalation(Long orderId, String note) {
		if (!currentUser.isAuthenticated()
				|| (!currentUser.hasRole(RoleName.ROLE_MANAGER) && !currentUser.hasRole(RoleName.ROLE_ADMIN))) {
			throw new ForbiddenException();
		}
		String normalizedNote = note == null ? null : note.trim();
		if (!hasText(normalizedNote) || normalizedNote.length() > 500) {
			throw new BadRequestException("Vui lòng nhập ghi chú (tối đa 500 ký tự) khi từ chối khiếu nại");
		}
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		ReturnRequest request = returnRequestRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Yêu cầu trả hàng", orderId));
		if (request.getStatus() != ReturnStatus.ESCALATED || order.getStatus() != OrderStatus.RETURN_REJECTED) {
			throw new BusinessException("Yêu cầu này không ở trạng thái đang khiếu nại");
		}
		request.setStatus(ReturnStatus.REJECTED);
		request.setAdminNote(normalizedNote);
		request.setDecidedAt(LocalDateTime.now(clock));
		// Khi Manager ghi adminNote, khiếu nại đã có kết luận cuối cùng và người mua không thể gửi lại.
		eventPublisher.publishEvent(new ReturnEscalationRejectedEvent(order.getId(), order.getOrderCode(),
				request.getUser() == null ? (order.getUser() == null ? null : order.getUser().getId())
						: request.getUser().getId(), normalizedNote));
	}

	private ReturnRequestInfoResponse toResponse(ReturnRequest request) {
		return new ReturnRequestInfoResponse(request.getStatus(), ReturnStatusUi.label(request.getStatus()),
				ReturnStatusUi.color(request.getStatus()), trimToNull(request.getReason()),
				trimToNull(request.getVendorNote()), trimToNull(request.getAdminNote()),
				trimToNull(request.getEscalationReason()), request.getProofImageUrl(), request.getProofVideoUrl(),
				request.getCreatedAt(), request.getDecidedAt());
	}

	private String normalizeWhitespace(String value) {
		return WHITESPACE.matcher(value.trim()).replaceAll(" ");
	}

	private String trimToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
