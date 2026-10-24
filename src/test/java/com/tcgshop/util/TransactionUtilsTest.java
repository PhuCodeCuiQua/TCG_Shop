package com.tcgshop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionUtilsTest {

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void runsImmediatelyWhenNoSynchronizationIsActive() {
		AtomicBoolean ran = new AtomicBoolean();

		TransactionUtils.runAfterCommit(() -> ran.set(true));

		assertTrue(ran.get());
	}

	@Test
	void waitsUntilAfterCommitWhenSynchronizationIsActive() {
		TransactionSynchronizationManager.initSynchronization();
		AtomicBoolean ran = new AtomicBoolean();

		TransactionUtils.runAfterCommit(() -> ran.set(true));

		assertFalse(ran.get());
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCommit();
		}
		assertTrue(ran.get());
	}

	@Test
	void catchesRunnableExceptionsInBothExecutionPaths() {
		TransactionUtils.runAfterCommit(() -> { throw new IllegalStateException("expected"); });
		TransactionSynchronizationManager.initSynchronization();
		TransactionUtils.runAfterCommit(() -> { throw new IllegalStateException("expected after commit"); });

		assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCommit();
		}
	}

	@Test
	void reportsCommitAndRollbackAfterCompletion() {
		TransactionSynchronizationManager.initSynchronization();
		java.util.List<Boolean> results = new java.util.ArrayList<>();
		TransactionUtils.runAfterCompletion(results::add);
		TransactionSynchronizationManager.getSynchronizations().get(0)
				.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();

		TransactionSynchronizationManager.initSynchronization();
		TransactionUtils.runAfterCompletion(results::add);
		TransactionSynchronizationManager.getSynchronizations().get(0)
				.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		assertEquals(java.util.List.of(true, false), results);
	}

	@Test
	void runsAfterCompletionImmediatelyWhenNoSynchronizationIsActive() {
		AtomicBoolean committed = new AtomicBoolean();

		TransactionUtils.runAfterCompletion(committed::set);

		assertTrue(committed.get());
	}

	@Test
	void rollbackCallbackRunsOnlyAfterRollbackAndDoesNothingWithoutSynchronization() {
		AtomicBoolean rolledBack = new AtomicBoolean();
		TransactionUtils.runAfterRollback(() -> rolledBack.set(true));
		assertFalse(rolledBack.get());

		TransactionSynchronizationManager.initSynchronization();
		TransactionUtils.runAfterRollback(() -> rolledBack.set(true));
		TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
		synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		assertFalse(rolledBack.get());
		TransactionSynchronizationManager.clearSynchronization();

		TransactionSynchronizationManager.initSynchronization();
		TransactionUtils.runAfterRollback(() -> rolledBack.set(true));
		TransactionSynchronizationManager.getSynchronizations().get(0)
				.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		assertTrue(rolledBack.get());
	}

	@Test
	void rollbackCallbackLogsAndSuppressesRunnableFailure() {
		TransactionSynchronizationManager.initSynchronization();
		TransactionUtils.runAfterRollback(() -> { throw new IllegalStateException("expected"); });
		TransactionSynchronizationManager.getSynchronizations().get(0)
				.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}
}
