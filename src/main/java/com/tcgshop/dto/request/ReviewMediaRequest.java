package com.tcgshop.dto.request;

import com.tcgshop.validation.MinCommentLength;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ReviewMediaRequest {
	@NotNull(message = "Vui lòng chọn số sao")
	@Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao")
	@Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao")
	private Integer rating;

	@Size(max = 2000, message = "Nhận xét không được vượt quá 2.000 ký tự")
	@MinCommentLength
	private String comment;

	@Size(max = 5, message = "Mỗi đánh giá được đính kèm tối đa 5 ảnh")
	private List<MultipartFile> images = new ArrayList<>();

	private MultipartFile video;
}
