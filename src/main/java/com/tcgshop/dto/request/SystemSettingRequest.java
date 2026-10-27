package com.tcgshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class SystemSettingRequest {
    @NotBlank(message = "Vui lòng nhập tên website")
    @Size(max = 100, message = "Tên website tối đa 100 ký tự")
    private String siteName;
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "(0[0-9]{9}|\\+84[0-9]{9})", message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
    private String contactPhone;
    @NotBlank(message = "Vui lòng nhập email hỗ trợ")
    @Email(message = "Email hỗ trợ không hợp lệ")
    @Size(max = 254, message = "Email tối đa 254 ký tự")
    private String contactEmail;
    private MultipartFile logo;
}
