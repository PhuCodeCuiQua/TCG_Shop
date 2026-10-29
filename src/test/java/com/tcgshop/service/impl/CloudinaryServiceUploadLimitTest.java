package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tcgshop.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CloudinaryServiceUploadLimitTest {
	private Cloudinary cloudinary;
	private CloudinaryServiceImpl service;

	@BeforeEach
	void setUp() {
		cloudinary = mock(Cloudinary.class);
		when(cloudinary.uploader()).thenReturn(mock(Uploader.class));
		service = new CloudinaryServiceImpl(cloudinary);
	}

	@Test
	void rejectsImageLargerThanFiveMegabytesBeforeCallingCloudinary() {
		byte[] payload = new byte[5 * 1024 * 1024 + 1];
		MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", payload);

		assertThrows(BadRequestException.class, () -> service.uploadImage(file, "test"));
		verify(cloudinary, never()).uploader();
	}

	@Test
	void rejectsVideoLargerThanFiftyMegabytesBeforeCallingCloudinary() {
		byte[] payload = new byte[50 * 1024 * 1024 + 1];
		MockMultipartFile file = new MockMultipartFile("file", "large.mp4", "video/mp4", payload);

		assertThrows(BadRequestException.class, () -> service.uploadVideo(file, "test"));
		verify(cloudinary, never()).uploader();
	}
}
