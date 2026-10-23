package com.tcgshop.service.impl;

import com.tcgshop.dto.request.UpdateProfileRequest;
import com.tcgshop.dto.response.ProfileResponse;
import com.tcgshop.entity.User;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ProfileMapper;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.ProfileService;
import com.tcgshop.service.UploadResult;
import com.tcgshop.util.TransactionUtils;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

	private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);
	private static final String AVATAR_FOLDER = "tcgshop/avatars";
	private static final String IMAGE_RESOURCE_TYPE = "image";

	private final UserRepository userRepository;
	private final CurrentUser currentUser;
	private final CloudinaryService cloudinaryService;
	private final ProfileMapper profileMapper;

	public ProfileServiceImpl(UserRepository userRepository, CurrentUser currentUser,
			CloudinaryService cloudinaryService, ProfileMapper profileMapper) {
		this.userRepository = userRepository;
		this.currentUser = currentUser;
		this.cloudinaryService = cloudinaryService;
		this.profileMapper = profileMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public ProfileResponse getProfile() {
		return profileMapper.toResponse(getCurrentUser());
	}

	@Override
	public ProfileResponse updateProfile(UpdateProfileRequest request) {
		User user = getCurrentUser();
		user.setFullName(normalizeFullName(request.getFullName()));
		user.setPhoneNumber(request.getPhoneNumber().trim());
		return profileMapper.toResponse(userRepository.save(user));
	}

	@Override
	public ProfileResponse updateAvatar(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("Vui lòng chọn ảnh");
		}
		User user = getCurrentUser();
		UploadResult uploaded = cloudinaryService.uploadImage(file, AVATAR_FOLDER);
		String oldPublicId = user.getAvatarPublicId();

		// Upload ảnh mới trước rồi lưu URL/public ID trong DB. Chỉ xóa ảnh cũ sau commit;
		// nếu rollback thì xóa ảnh mới vừa upload để DB và Cloudinary cùng giữ trạng thái cũ.
		user.setAvatarUrl(uploaded.url());
		user.setAvatarPublicId(uploaded.publicId());
		try {
			User saved = userRepository.saveAndFlush(user);
			ProfileResponse response = profileMapper.toResponse(saved);
			TransactionUtils.runAfterCompletion(committed -> {
				if (committed) {
					deleteOldAvatar(oldPublicId, uploaded.publicId());
				} else {
					deleteBestEffort(uploaded.publicId());
				}
			});
			return response;
		} catch (RuntimeException exception) {
			deleteBestEffort(uploaded.publicId());
			throw exception;
		}
	}

	@Override
	public ProfileResponse removeAvatar() {
		User user = getCurrentUser();
		String oldPublicId = user.getAvatarPublicId();
		user.setAvatarUrl(null);
		user.setAvatarPublicId(null);
		User saved = userRepository.saveAndFlush(user);
		deleteOldAvatarAfterCommit(oldPublicId, null);
		return profileMapper.toResponse(saved);
	}

	private User getCurrentUser() {
		return userRepository.findById(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Người dùng", currentUser.getId()));
	}

	private String normalizeFullName(String fullName) {
		return fullName.trim().replaceAll("\\s+", " ");
	}

	private void deleteOldAvatarAfterCommit(String oldPublicId, String newPublicId) {
		if (oldPublicId == null || oldPublicId.isBlank() || Objects.equals(oldPublicId, newPublicId)) {
			return;
		}
		TransactionUtils.runAfterCommit(() -> cloudinaryService.delete(oldPublicId, IMAGE_RESOURCE_TYPE));
	}

	private void deleteOldAvatar(String oldPublicId, String newPublicId) {
		if (oldPublicId == null || oldPublicId.isBlank() || Objects.equals(oldPublicId, newPublicId)) {
			return;
		}
		try {
			cloudinaryService.delete(oldPublicId, IMAGE_RESOURCE_TYPE);
		} catch (Exception exception) {
			log.warn("Không thể dọn ảnh đại diện cũ sau khi cập nhật hồ sơ", exception);
		}
	}

	private void deleteBestEffort(String publicId) {
		if (publicId == null || publicId.isBlank()) {
			return;
		}
		try {
			cloudinaryService.delete(publicId, IMAGE_RESOURCE_TYPE);
		} catch (Exception exception) {
			log.warn("Không thể dọn ảnh đại diện mới sau khi lưu hồ sơ thất bại", exception);
		}
	}
}
