package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ReturnComplaintDetailResponse;
import com.tcgshop.dto.response.ReturnComplaintDetailResponse.TimelineItem;
import com.tcgshop.dto.response.ReturnComplaintResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.ReturnRequest;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnStatus;
import com.tcgshop.event.ReturnEscalationRejectedEvent;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ReturnComplaintMapper;
import com.tcgshop.repository.ManagerReturnRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.TimelineRow;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.ReturnComplaintService;
import com.tcgshop.util.TransactionUtils;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ReturnComplaintServiceImpl implements ReturnComplaintService {
	private final ManagerReturnRepository returnRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final UserRepository userRepository;
	private final OrderService orderService;
	private final ReturnComplaintMapper mapper;
	private final Clock clock;
	private final ApplicationEventPublisher applicationEventPublisher;

	public ReturnComplaintServiceImpl(ManagerReturnRepository returnRepository,
			OrderStatusHistoryRepository historyRepository, UserRepository userRepository,
				OrderService orderService, ReturnComplaintMapper mapper, Clock clock,
				ApplicationEventPublisher applicationEventPublisher) {
		this.returnRepository = returnRepository;
		this.historyRepository = historyRepository;
		this.userRepository = userRepository;
		this.orderService = orderService;
		this.mapper = mapper;
		this.clock = clock;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ReturnComplaintResponse> listComplaints(Pageable pageable) {
		return returnRepository.findByStates(OrderStatus.RETURN_REJECTED, ReturnStatus.ESCALATED, pageable)
				.map(mapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ReturnComplaintResponse> listRefundable(Pageable pageable) {
		return returnRepository.findByOrderStatus(OrderStatus.RETURNED, pageable).map(mapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public ReturnComplaintDetailResponse getDetail(Long orderId) {
		ReturnRequest request = find(orderId);
		List<TimelineRow> rows = historyRepository.findTimelineByOrderId(orderId);
		Set<Long> actorIds = rows.stream().map(TimelineRow::getActorId).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, String> names = new HashMap<>();
		for (User user : userRepository.findAllById(actorIds)) {
			names.put(user.getId(), user.getFullName());
		}
		List<TimelineItem> timeline = rows.stream()
				.map(row -> new TimelineItem(row.getPreviousStatus(), row.getNewStatus(), row.getNote(),
						row.getCreatedAt(), row.getActorId() == null ? "Hệ thống" : names.get(row.getActorId())))
				.toList();
		return mapper.toDetail(request, timeline);
	}

	@Override
	@Transactional
	public void acceptComplaint(Long orderId, String note) {
		ReturnRequest request = find(orderId);
		requireComplaintState(request);
		orderService.changeStatus(orderId, OrderStatus.RETURN_APPROVED, note);
	}

	@Override
	@Transactional
	public void rejectComplaint(Long orderId, String note) {
		ReturnRequest request = find(orderId);
		requireComplaintState(request);
		request.setStatus(ReturnStatus.REJECTED);
		request.setAdminNote(note.trim());
		request.setDecidedAt(LocalDateTime.now(clock));
		returnRepository.save(request);
		Order order = request.getOrder();
		String orderCode = order.getOrderCode();
		Long buyerId = order.getUser().getId();
		String trimmedNote = note.trim();
		TransactionUtils.runAfterCommit(() -> applicationEventPublisher.publishEvent(
				new ReturnEscalationRejectedEvent(orderId, orderCode, buyerId, trimmedNote)));
	}

	@Override
	@Transactional
	public void refund(Long orderId, String note) {
		ReturnRequest request = find(orderId);
		if (request.getOrder().getStatus() != OrderStatus.RETURNED) {
			throw new BusinessException("Chỉ hoàn tiền cho đơn đã nhận hàng trả về");
		}
		// refundAmount và refundedAt do OrderStatusSideEffectHandler tự điền, không gán lại ở đây.
		orderService.changeStatus(orderId, OrderStatus.REFUNDED, note);
	}

	private void requireComplaintState(ReturnRequest request) {
		if (request.getOrder().getStatus() != OrderStatus.RETURN_REJECTED
				|| request.getStatus() != ReturnStatus.ESCALATED) {
			throw new BusinessException("Đơn này không ở trạng thái khiếu nại chờ xử lý");
		}
	}

	private ReturnRequest find(Long orderId) {
		return returnRepository.findDetailByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Yêu cầu trả hàng", orderId));
	}
}
