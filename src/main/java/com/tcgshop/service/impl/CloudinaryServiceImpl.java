package com.tcgshop.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.CloudinaryUploadException;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {
	private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
	private static final long MAX_VIDEO_SIZE_BYTES = 50L * 1024 * 1024;
	private static final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);

	private final Cloudinary cloudinary;

	public CloudinaryServiceImpl(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	@Override
	public UploadResult uploadImage(MultipartFile file, String folder) {
		validateFile(file);
		if (file.getSize() >= MAX_IMAGE_SIZE_BYTES) {
			throw new BadRequestException("Ảnh phải có dung lượng nhỏ hơn 5MB");
		}
		validateImageContent(file);
		return upload(file, folder, "image");
	}

	@Override
	public UploadResult uploadVideo(MultipartFile file, String folder) {
		validateFile(file);
		if (file.getSize() > MAX_VIDEO_SIZE_BYTES) {
			throw new BadRequestException("Video không được vượt quá 50MB");
		}
		validateVideoContent(file);
		return upload(file, folder, "video");
	}

	@Override
	public void delete(String publicId, String resourceType) {
		if (publicId == null || publicId.isBlank()) {
			throw new BadRequestException("publicId không được để trống");
		}
		if (!"image".equals(resourceType) && !"video".equals(resourceType)) {
			throw new BadRequestException("Loại tài nguyên chỉ được là image hoặc video");
		}
		try {
			cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
		} catch (Exception exception) {
			log.error("Cloudinary delete failed for resource type {}", resourceType, exception);
			throw new CloudinaryUploadException(exception);
		}
	}

	private UploadResult upload(MultipartFile file, String folder, String resourceType) {
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"folder", folder,
					"resource_type", resourceType));
			Object url = result.get("secure_url");
			Object publicId = result.get("public_id");
			if (url == null || publicId == null) {
				throw new IllegalStateException("Cloudinary response is missing upload fields");
			}
			return new UploadResult(url.toString(), publicId.toString());
		} catch (Exception exception) {
			log.error("Cloudinary upload failed for resource type {}", resourceType, exception);
			throw new CloudinaryUploadException(exception);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("File không được để trống");
		}
	}

	private void validateImageContent(MultipartFile file) {
		String extension = getExtension(file);
		byte[] header = readHeader(file);

		// Không tin Content-Type do trình duyệt gửi: đối chiếu phần mở rộng với chữ ký nhị phân thật của file.
		boolean valid = switch (extension) {
			case "jpg", "jpeg" -> hasJpegSignature(header);
			case "png" -> hasPngSignature(header);
			default -> false;
		};
		if (!valid) {
			throw new BadRequestException("Ảnh chỉ chấp nhận định dạng JPG, JPEG hoặc PNG hợp lệ");
		}
	}

	private void validateVideoContent(MultipartFile file) {
		String extension = getExtension(file);
		byte[] header = readHeader(file);

		// MP4 phải có box ftyp và WebM phải có EBML magic bytes; đổi tên file không thể giả mạo chữ ký này.
		boolean valid = switch (extension) {
			case "mp4" -> hasMp4Signature(header);
			case "webm" -> hasWebmSignature(header);
			default -> false;
		};
		if (!valid) {
			throw new BadRequestException("Video chỉ chấp nhận định dạng MP4 hoặc WebM hợp lệ");
		}
	}

	private String getExtension(MultipartFile file) {
		String filename = file.getOriginalFilename();
		if (filename == null) {
			return "";
		}
		int dotIndex = filename.lastIndexOf('.');
		return dotIndex < 0 ? "" : filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
	}

	private byte[] readHeader(MultipartFile file) {
		try (InputStream input = file.getInputStream()) {
			return input.readNBytes(12);
		} catch (IOException exception) {
			throw new BadRequestException("Không thể đọc nội dung tệp tải lên");
		}
	}

	private boolean hasJpegSignature(byte[] header) {
		return header.length >= 3
				&& (header[0] & 0xff) == 0xff
				&& (header[1] & 0xff) == 0xd8
				&& (header[2] & 0xff) == 0xff;
	}

	private boolean hasPngSignature(byte[] header) {
		byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
		if (header.length < signature.length) {
			return false;
		}
		for (int index = 0; index < signature.length; index++) {
			if (header[index] != signature[index]) {
				return false;
			}
		}
		return true;
	}

	private boolean hasMp4Signature(byte[] header) {
		return header.length >= 8
				&& header[4] == 'f'
				&& header[5] == 't'
				&& header[6] == 'y'
				&& header[7] == 'p';
	}

	private boolean hasWebmSignature(byte[] header) {
		return header.length >= 4
				&& (header[0] & 0xff) == 0x1a
				&& (header[1] & 0xff) == 0x45
				&& (header[2] & 0xff) == 0xdf
				&& (header[3] & 0xff) == 0xa3;
	}
}
