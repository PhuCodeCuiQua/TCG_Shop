package com.tcgshop.util;

import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionUtils {

	private static final Logger log = LoggerFactory.getLogger(TransactionUtils.class);

	private TransactionUtils() { }

	public static void runAfterCommit(Runnable action) {
		Runnable safeAction = () -> {
			try {
				action.run();
			} catch (Exception exception) {
				log.warn("Tác vụ sau commit thất bại", exception);
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					safeAction.run();
				}
			});
		} else {
			safeAction.run();
		}
	}

	/** Chạy callback khi transaction hoàn tất; true biểu thị commit, false biểu thị rollback. */
	public static void runAfterCompletion(Consumer<Boolean> action) {
		Objects.requireNonNull(action, "action");
		Consumer<Boolean> safeAction = committed -> {
			try {
				action.accept(committed);
			} catch (Exception exception) {
				log.warn("Tác vụ sau khi transaction hoàn tất thất bại", exception);
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					safeAction.accept(status == STATUS_COMMITTED);
				}
			});
		} else {
			// Không có transaction để rollback nên xem thao tác là hoàn tất thành công.
			safeAction.accept(true);
		}
	}
}
