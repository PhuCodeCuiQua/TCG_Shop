package com.tcgshop.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
	UploadResult uploadImage(MultipartFile file, String folder);

	UploadResult uploadVideo(MultipartFile file, String folder);

	void delete(String publicId, String resourceType);
}
