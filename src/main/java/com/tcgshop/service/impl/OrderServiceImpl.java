package com.tcgshop.service.impl;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ActorType;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.event.OrderAssignedEvent;
import com.tcgshop.event.OrderStatusChangedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderService;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
	private final OrderRepository orderRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final UserRepository userRepository;
	private final CurrentUser currentUser;
	private final OrderTransitionPolicy transitionPolicy;
	private final ApplicationEventPublisher eventPublisher;

	public OrderServiceImpl(OrderRepository orderRepository,
			OrderStatusHistoryRepository historyRepository,
			UserRepository userRepository,
			CurrentUser currentUser,
			OrderTransitionPolicy transitionPolicy,
			ApplicationEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.historyRepository = historyRepository;
		this.userRepository = userRepository;
		this.currentUser = currentUser;
		this.transitionPolicy = transitionPolicy;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public void changeStatus(Long orderId, OrderStatus newStatus, String note) {
		changeStatusInternal(orderId, newStatus, note, false);
	}

	@Override
	@Transactional
	public void changeStatusBySystem(Long orderId, OrderStatus newStatus, String note) {
		changeStatusInternal(orderId, newStatus, note, true);
	}

	private void changeStatusInternal(Long orderId, OrderStatus newStatus, String note, boolean bySystem) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		OrderStatus oldStatus = order.getStatus();
		if (!transitionPolicy.isAllowed(oldStatus, newStatus)) {
			throw new BusinessException("Không thể chuyển trạng thái đơn hàng từ "
					+ oldStatus + " sang " + newStatus);
		}

		Set<ActorType> actors = bySystem ? EnumSet.of(ActorType.SYSTEM) : determineActorTypes(order);
		if (actors.stream().noneMatch(transitionPolicy.allowedActors(oldStatus, newStatus)::contains)) {
			throw new ForbiddenException();
		}
		if (transitionPolicy.requiresNote(oldStatus, newStatus) && (note == null || note.isBlank())) {
			throw new BadRequestException("Vui lòng nhập lý do chuyển trạng thái");
		}
		if (oldStatus == OrderStatus.CONFIRMED && newStatus == OrderStatus.PICKED_UP
				&& order.getShipper() == null) {
			throw new BusinessException("Đơn hàng chưa được phân công shipper");
		}

		Long actorId = bySystem ? null : currentUser.getId();
		User changedBy = bySystem ? null : findUser(actorId);
		order.setStatus(newStatus);
		orderRepository.save(order);
		
		// Các tác dụng phụ dưới đây thuộc PU-16, chưa thực hiện trong PU-09a.
		if (newStatus == OrderStatus.CANCELLED) {
			// TODO(PU-16): Hoàn tồn kho của các sản phẩm trong đơn khi đơn bị hủy.
			// TODO(PU-16): Nếu đơn MoMo bị hủy, chuyển giao dịch sang REFUND_PENDING.
		}
		if (newStatus == OrderStatus.DELIVERED) {
			// TODO(PU-16): Tăng sold_count của sản phẩm theo số lượng trong đơn.
			// TODO(PU-16): Chụp commission_rate, tính commission_amount và shop_revenue.
			// TODO(PU-16): Đánh dấu COD đã thu khi đơn dùng phương thức COD.
			// TODO(PU-16): Ghi delivered_at khi đơn giao thành công.
		}
		if (newStatus == OrderStatus.RETURN_REQUESTED) {
			// TODO(PU-16): Kiểm tra đơn còn trong thời hạn yêu cầu đổi trả.
		}

		historyRepository.save(createHistory(order, oldStatus, newStatus, changedBy, note));
		eventPublisher.publishEvent(new OrderStatusChangedEvent(
				order.getId(),
				order.getOrderCode(),
				idOf(order.getUser()),
				idOf(order.getShop()),
				idOf(order.getShop() == null ? null : order.getShop().getOwner()),
				idOf(order.getShipper()),
				oldStatus,
				newStatus,
				actorId,
				note,
				LocalDateTime.now()));
	}

	@Override
	@Transactional
	public void assignShipper(Long orderId, Long shipperId) {
		if (!currentUser.isAuthenticated()
				|| (!currentUser.hasRole(RoleName.ROLE_MANAGER) && !currentUser.hasRole(RoleName.ROLE_ADMIN))) {
			throw new ForbiddenException();
		}

		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		if (order.getStatus() != OrderStatus.CONFIRMED) {
			throw new BusinessException("Chỉ có thể phân công shipper cho đơn đã xác nhận");
		}

		User shipper = userRepository.findById(shipperId)
				.orElseThrow(() -> new BadRequestException("Người dùng được chọn không phải shipper hợp lệ"));
		boolean hasShipperRole = shipper.getRoles().stream()
				.anyMatch(role -> role.getName() == RoleName.ROLE_SHIPPER);
		if (!hasShipperRole || shipper.getStatus() != UserStatus.ACTIVE) {
			throw new BadRequestException("Shipper phải có ROLE_SHIPPER và trạng thái ACTIVE");
		}

		Long assignedById = currentUser.getId();
		User assignedBy = findUser(assignedById);
		order.setShipper(shipper);
		orderRepository.save(order);
		historyRepository.save(createHistory(order, OrderStatus.CONFIRMED, OrderStatus.CONFIRMED,
				assignedBy, "Phân công shipper: " + shipper.getFullName()));
		eventPublisher.publishEvent(new OrderAssignedEvent(
				order.getId(), order.getOrderCode(), shipper.getId(), idOf(order.getUser()), assignedById));
	}

	/**
	 * Xác định mọi vai trò nghiệp vụ mà tài khoản hiện tại có trên chính đơn này.
	 * Một người có thể vừa là chủ shop vừa là Manager, nên giữ tập hợp để áp dụng đúng
	 * quyền giao nhau trong bảng chính sách.
	 */
	private Set<ActorType> determineActorTypes(Order order) {
		Set<ActorType> actors = new HashSet<>();
		if (!currentUser.isAuthenticated()) {
			return actors;
		}

		Long userId = currentUser.getId();
		if (userId.equals(idOf(order.getUser()))) {
			actors.add(ActorType.BUYER);
		}
		if (order.getShop() != null && userId.equals(idOf(order.getShop().getOwner()))) {
			actors.add(ActorType.SHOP_OWNER);
		}
		if (order.getShipper() != null && userId.equals(order.getShipper().getId())) {
			actors.add(ActorType.ASSIGNED_SHIPPER);
		}
		if (currentUser.hasRole(RoleName.ROLE_MANAGER)) {
			actors.add(ActorType.MANAGER);
		}
		if (currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			actors.add(ActorType.ADMIN);
		}
		return actors;
	}

	private OrderStatusHistory createHistory(Order order, OrderStatus previousStatus,
			OrderStatus newStatus, User changedBy, String note) {
		OrderStatusHistory history = new OrderStatusHistory();
		history.setOrder(order);
		history.setPreviousStatus(previousStatus);
		history.setNewStatus(newStatus);
		history.setChangedBy(changedBy);
		history.setNote(note);
		return history;
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
	}

	private Long idOf(User user) {
		return user == null ? null : user.getId();
	}

	private Long idOf(com.tcgshop.entity.Shop shop) {
		return shop == null ? null : shop.getId();
	}
}
