package com.tcgshop.service;

import java.util.List;

public interface DataCleanupService {
	CleanupResult cleanupProductsWithoutRealImages(boolean apply);

	record CleanupResult(List<Long> candidateIds, int hidden, boolean catalogAssetsLoaded) {
		public int candidates() {
			return candidateIds.size();
		}
	}
}
