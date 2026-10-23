package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.UpdateProfileRequest;
import com.tcgshop.dto.response.ProfileResponse;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.CloudinaryUploadException;
import com.tcgshop.mapper.ProfileMapper;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceImplTest {

	@Mock private UserRepository userRepository;
	@Mock private CurrentUser currentUser;
	@Mock private CloudinaryService cloudinaryService;
	@Mock private ProfileMapper profileMapper;
	private ProfileServiceImpl profileService;
	private User user;

	@AfterEach
	void clearTransactionSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@BeforeEach
	void setUp() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		profileService = new ProfileServiceImpl(userRepository, currentUser, cloudinaryService, profileMapper);
		user = new User();
		user.setId(17L);
		user.setEmail("profile@example.com");
		user.setFullName("Tên ban đầu");
		user.setPhoneNumber("0912345678");
		user.setAvatarUrl("https://example.com/old.png");
		user.setAvatarPublicId("old-avatar");
		when(currentUser.getId()).thenReturn(17L);
		when(userRepository.findById(17L)).thenReturn(java.util.Optional.of(user));
		when(profileMapper.toResponse(any(User.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));
	}

	@Test
	void updatesProfileAndNormalizesNameWhitespace() {
		when(userRepository.save(user)).thenReturn(user);
		UpdateProfileRequest request = new UpdateProfileRequest();
		request.setFullName("  Nguyễn   Văn   An  ");
		request.setPhoneNumber("+84912345678");

		ProfileResponse response = profileService.updateProfile(request);

		assertEquals("Nguyễn Văn An", user.getFullName());
		assertEquals("+84912345678", user.getPhoneNumber());
		assertEquals("profile@example.com", response.email());
		verify(userRepository).save(user);
	}

	@Test
	void replacesAvatarAndDeletesOldAsset() {
		MultipartFile file = imageFile();
		when(cloudinaryService.uploadImage(file, "tcgshop/avatars"))
				.thenReturn(new UploadResult("https://example.com/new.png", "new-avatar"));
		when(userRepository.saveAndFlush(user)).thenReturn(user);

		ProfileResponse response = profileService.updateAvatar(file);

		assertEquals("https://example.com/new.png", user.getAvatarUrl());
		assertEquals("new-avatar", user.getAvatarPublicId());
		assertEquals("https://example.com/new.png", response.avatarUrl());
		verify(cloudinaryService).delete("old-avatar", "image");
	}

	@Test
	void uploadFailureDoesNotChangeDatabaseOrDeleteOldAsset() {
		MultipartFile file = imageFile();
		when(cloudinaryService.uploadImage(file, "tcgshop/avatars"))
				.thenThrow(new CloudinaryUploadException(new IllegalStateException("upload failed")));

		assertThrows(CloudinaryUploadException.class, () -> profileService.updateAvatar(file));

		assertEquals("https://example.com/old.png", user.getAvatarUrl());
		assertEquals("old-avatar", user.getAvatarPublicId());
		verify(userRepository, never()).saveAndFlush(any(User.class));
		verify(cloudinaryService, never()).delete(any(), any());
	}

	@Test
	void databaseFailureDeletesNewAssetAndKeepsOldAsset() {
		MultipartFile file = imageFile();
		when(cloudinaryService.uploadImage(file, "tcgshop/avatars"))
				.thenReturn(new UploadResult("https://example.com/new.png", "new-avatar"));
		when(userRepository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("database failed"));

		assertThrows(DataIntegrityViolationException.class, () -> profileService.updateAvatar(file));

		verify(cloudinaryService).delete("new-avatar", "image");
		verify(cloudinaryService, never()).delete("old-avatar", "image");
	}

	@Test
	void transactionRollbackDeletesNewAssetAndPreservesOldAsset() {
		TransactionSynchronizationManager.initSynchronization();
		MultipartFile file = imageFile();
		when(cloudinaryService.uploadImage(file, "tcgshop/avatars"))
				.thenReturn(new UploadResult("https://example.com/new.png", "new-avatar"));
		when(userRepository.saveAndFlush(user)).thenReturn(user);

		profileService.updateAvatar(file);
		verify(cloudinaryService, never()).delete(any(), any());
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		}

		verify(cloudinaryService).delete("new-avatar", "image");
		verify(cloudinaryService, never()).delete("old-avatar", "image");
	}

	@Test
	void skipsOldAssetDeletionWhenPublicIdIsNull() {
		user.setAvatarPublicId(null);
		MultipartFile file = imageFile();
		when(cloudinaryService.uploadImage(file, "tcgshop/avatars"))
				.thenReturn(new UploadResult("https://example.com/new.png", "new-avatar"));
		when(userRepository.saveAndFlush(user)).thenReturn(user);

		profileService.updateAvatar(file);

		verify(cloudinaryService, never()).delete(any(), any());
	}

	@Test
	void rejectsEmptyAvatarFileBeforeUpload() {
		MultipartFile file = new MockMultipartFile("file", "", "image/png", new byte[0]);

		BadRequestException error = assertThrows(BadRequestException.class, () -> profileService.updateAvatar(file));

		assertEquals("Vui lòng chọn ảnh", error.getMessage());
		verify(cloudinaryService, never()).uploadImage(any(), any());
	}

	@Test
	void removesAvatarAndDeletesOldAsset() {
		when(userRepository.saveAndFlush(user)).thenReturn(user);

		ProfileResponse response = profileService.removeAvatar();

		assertNull(user.getAvatarUrl());
		assertNull(user.getAvatarPublicId());
		assertNull(response.avatarUrl());
		verify(cloudinaryService).delete("old-avatar", "image");
	}

	private MockMultipartFile imageFile() {
		return new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
	}

	private ProfileResponse toResponse(User source) {
		return new ProfileResponse(source.getEmail(), source.getFullName(), source.getPhoneNumber(),
				source.getAvatarUrl(), Set.<RoleName>of(), source.getCreatedAt());
	}
}
