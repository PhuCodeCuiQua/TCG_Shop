"use strict";

document.addEventListener("DOMContentLoaded", () => {
	const form = document.getElementById("shopRegistrationForm");
	const logoInput = document.getElementById("logo");
	const preview = document.getElementById("logoPreview");
	const placeholder = document.getElementById("logoPlaceholder");
	const submitButton = document.getElementById("shopSubmitButton");

	logoInput?.addEventListener("change", () => {
		const file = logoInput.files?.[0];
		if (!file) return;
		if (window.App?.previewImage(file, preview, { maxSizeBytes: 5 * 1024 * 1024, alt: "Xem trước logo chi nhánh" })) {
			placeholder?.classList.add("d-none");
		}
	});

	form?.addEventListener("submit", () => {
		if (window.App?.setLoading) {
			window.App.setLoading(submitButton, true, { text: "Đang gửi hồ sơ..." });
		}
	});
});
