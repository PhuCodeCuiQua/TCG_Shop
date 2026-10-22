package com.tcgshop.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
	private final String cloudName;
	private final String apiKey;
	private final String apiSecret;

	public CloudinaryConfig(
			@Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:k6q4mnph}}") String cloudName,
			@Value("${cloudinary.api-key:${CLOUDINARY_API_KEY:241672199656985}}") String apiKey,
			@Value("${cloudinary.api-secret:${CLOUDINARY_API_SECRET:}}") String apiSecret) {
		this.cloudName = cloudName;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	@Bean
	public Cloudinary cloudinary() {
		return new Cloudinary(ObjectUtils.asMap(
				"cloud_name", cloudName,
				"api_key", apiKey,
				"api_secret", apiSecret));
	}
}
