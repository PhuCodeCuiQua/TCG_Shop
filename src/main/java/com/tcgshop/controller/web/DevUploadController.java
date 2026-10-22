package com.tcgshop.controller.web;

import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.CloudinaryUploadException;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dev/upload")
public class DevUploadController {
	private static final String TEST_FOLDER = "tcg-shop/dev-upload";

	private final CloudinaryService cloudinaryService;

	public DevUploadController(CloudinaryService cloudinaryService) {
		this.cloudinaryService = cloudinaryService;
	}

	// TODO: Xóa trước khi merge.
	@GetMapping
	public String showUploadPage() {
		return "dev/upload";
	}

	// TODO: Xóa trước khi merge.
	@PostMapping
	public String upload(@RequestParam("file") MultipartFile file,
			@RequestParam("resourceType") String resourceType, RedirectAttributes redirectAttributes) {
		try {
			UploadResult result = switch (resourceType) {
				case "image" -> cloudinaryService.uploadImage(file, TEST_FOLDER);
				case "video" -> cloudinaryService.uploadVideo(file, TEST_FOLDER);
				default -> throw new BadRequestException("Loại tệp tải lên không hợp lệ");
			};
			redirectAttributes.addFlashAttribute("successMessage", "Tải tệp lên thành công.");
			redirectAttributes.addFlashAttribute("uploadedUrl", result.url());
		} catch (BadRequestException | CloudinaryUploadException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		}
		return "redirect:/dev/upload";
	}
}
