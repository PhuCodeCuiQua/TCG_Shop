package com.tcgshop.listener;

import com.tcgshop.entity.enums.NotificationType;
import com.tcgshop.event.ReviewCreatedEvent;
import com.tcgshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends a vendor notification only after a newly-created review has committed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewNotificationListener {

	private final NotificationService notificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onReviewCreated(ReviewCreatedEvent event) {
		if (event.shopOwnerId() == null) {
			log.warn("Bỏ qua thông báo review sản phẩm {} vì shop không có chủ sở hữu", event.productId());
			return;
		}

		String content = "Sản phẩm " + event.productName() + " vừa nhận được đánh giá mới.";
		try {
			notificationService.send(
					event.shopOwnerId(),
					NotificationType.REVIEW_NEW,
					"Sản phẩm vừa nhận được đánh giá mới",
					content,
					"/products/" + event.productId() + "#reviews");
		} catch (Exception exception) {
			log.error("Không thể gửi thông báo review sản phẩm {} tới chủ shop {}",
					event.productId(), event.shopOwnerId(), exception);
		}
	}
}
