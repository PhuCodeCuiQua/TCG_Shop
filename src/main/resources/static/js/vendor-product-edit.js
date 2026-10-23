"use strict";

(() => {
	const form = document.querySelector("form[data-loading-form]");
	if (form) {
		form.addEventListener("submit", (event) => {
			const submitButton = event.submitter || form.querySelector("button[type='submit']");
			if (submitButton) window.App?.setLoading(submitButton, true, { text: submitButton.dataset.loadingText || "Đang lưu..." });
		});
	}

	const input = document.querySelector("[data-preview-input]");
	const preview = document.getElementById("productEditPreview");
	const wrapper = document.getElementById("productEditPreviewWrap");
	input?.addEventListener("change", () => {
		const file = input.files?.[0];
		if (!file || !preview || !wrapper) return;
		if (window.App?.previewImage(file, preview, { alt: "Ảnh sản phẩm mới" })) {
			wrapper.classList.remove("d-none");
		}
	});
})();
