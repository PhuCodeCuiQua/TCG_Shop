package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.CloudinaryUploadException;
import com.tcgshop.service.UploadResult;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class CloudinaryServiceImplTest {
	private Cloudinary cloudinary;
	private Uploader uploader;
	private CloudinaryServiceImpl service;

	@BeforeEach
	void setUp() {
		cloudinary = mock(Cloudinary.class);
		uploader = mock(Uploader.class);
		when(cloudinary.uploader()).thenReturn(uploader);
		service = new CloudinaryServiceImpl(cloudinary);
	}

	@Test
	void uploadRejectsEmptyFile() {
		MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

		BadRequestException exception = assertThrows(
				BadRequestException.class, () -> service.uploadImage(file, "test"));

		assertEquals("File không được để trống", exception.getMessage());
		verify(cloudinary, never()).uploader();
	}

	@Test
	void uploadRejectsExecutableRenamedAsJpeg() {
		byte[] executableHeader = {0x4d, 0x5a, 0x00, 0x00};
		MockMultipartFile file = new MockMultipartFile(
				"file", "payload.jpg", "image/jpeg", executableHeader);

		assertThrows(BadRequestException.class, () -> service.uploadImage(file, "test"));

		verify(cloudinary, never()).uploader();
	}

	@Test
	void uploadRejectsImageAtFiveMegabytes() {
		MultipartFile file = mock(MultipartFile.class);
		when(file.isEmpty()).thenReturn(false);
		when(file.getSize()).thenReturn(5L * 1024 * 1024);

		BadRequestException exception = assertThrows(
				BadRequestException.class, () -> service.uploadImage(file, "test"));

		assertEquals("Ảnh phải có dung lượng nhỏ hơn 5MB", exception.getMessage());
		verify(cloudinary, never()).uploader();
	}

	@Test
	void uploadImageReturnsSecureUrlAndPublicIdForValidPng() throws IOException {
		byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00};
		MockMultipartFile file = new MockMultipartFile("file", "card.png", "text/plain", png);
		when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
				"secure_url", "https://res.cloudinary.com/example/image/upload/card.png",
				"public_id", "test/card"));

		UploadResult result = service.uploadImage(file, "test");

		assertEquals("https://res.cloudinary.com/example/image/upload/card.png", result.url());
		assertEquals("test/card", result.publicId());
		verify(uploader).upload(any(), anyMap());
	}

	@Test
	void uploadRejectsVideoLargerThanFiftyMegabytes() {
		MultipartFile file = mock(MultipartFile.class);
		when(file.isEmpty()).thenReturn(false);
		when(file.getSize()).thenReturn(50L * 1024 * 1024 + 1);

		BadRequestException exception = assertThrows(
				BadRequestException.class, () -> service.uploadVideo(file, "test"));

		assertEquals("Video không được vượt quá 50MB", exception.getMessage());
		verify(cloudinary, never()).uploader();
	}

	@Test
	void uploadVideoRejectsMismatchedMagicBytes() {
		byte[] invalidVideo = {0x4d, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
		MockMultipartFile file = new MockMultipartFile(
				"file", "clip.mp4", "video/mp4", invalidVideo);

		assertThrows(BadRequestException.class, () -> service.uploadVideo(file, "test"));

		verify(cloudinary, never()).uploader();
	}

	@Test
	void uploadVideoReturnsSecureUrlForValidWebmHeader() throws IOException {
		byte[] webm = {(byte) 0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0x00};
		MockMultipartFile file = new MockMultipartFile(
				"file", "clip.webm", "application/octet-stream", webm);
		when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
				"secure_url", "https://res.cloudinary.com/example/video/upload/clip.webm",
				"public_id", "test/clip"));

		UploadResult result = service.uploadVideo(file, "test");

		assertEquals("https://res.cloudinary.com/example/video/upload/clip.webm", result.url());
		assertEquals("test/clip", result.publicId());
		verify(uploader).upload(any(), anyMap());
	}

	@Test
	void cloudinaryFailureDoesNotExposeProviderMessage() throws IOException {
		byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
		MockMultipartFile file = new MockMultipartFile("file", "card.png", "image/png", png);
		when(uploader.upload(any(), anyMap())).thenThrow(new IOException("provider secret detail"));

		CloudinaryUploadException exception = assertThrows(
				CloudinaryUploadException.class, () -> service.uploadImage(file, "test"));

		assertEquals(
				"Không thể tải tệp lên dịch vụ lưu trữ. Vui lòng thử lại sau.",
				exception.getMessage());
	}
}
