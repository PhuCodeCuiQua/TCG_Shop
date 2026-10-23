"use strict";

document.addEventListener("DOMContentLoaded", () => {
	const form = document.getElementById("vendorShopManageForm");
	const submitButton = document.getElementById("manageSubmitButton");
	const imageInputs = [
		{ input: document.getElementById("logo"), preview: document.getElementById("logoPreview"), placeholder: document.getElementById("logoPlaceholder"), alt: "Xem trước logo shop" },
		{ input: document.getElementById("banner"), preview: document.getElementById("bannerPreview"), placeholder: document.getElementById("bannerPlaceholder"), alt: "Xem trước banner shop" }
	];

	imageInputs.forEach(({ input, preview, placeholder, alt }) => {
		input?.addEventListener("change", () => {
			const file = input.files?.[0];
			if (!file) return;
			const isValid = window.App?.previewImage(file, preview, {
				maxSizeBytes: 5 * 1024 * 1024,
				alt
			});
			if (isValid) {
				placeholder?.classList.add("d-none");
			} else if (placeholder) {
				placeholder.classList.remove("d-none");
			}
		});
	});

	form?.addEventListener("submit", () => {
		window.App?.setLoading(submitButton, true, { text: "Đang lưu thay đổi..." });
	});
});
