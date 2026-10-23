package com.tcgshop.service;

import com.tcgshop.dto.request.UpdateProfileRequest;
import com.tcgshop.dto.response.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
	ProfileResponse getProfile();
	ProfileResponse updateProfile(UpdateProfileRequest request);
	ProfileResponse updateAvatar(MultipartFile file);
	ProfileResponse removeAvatar();
}
