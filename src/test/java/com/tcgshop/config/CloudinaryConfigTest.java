package com.tcgshop.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CloudinaryConfigTest {
	@Test
	void createsCloudinaryClientWithConfiguredValues() {
		CloudinaryConfig config = new CloudinaryConfig("test-cloud", "test-key", "");

		assertNotNull(config.cloudinary());
	}
}
