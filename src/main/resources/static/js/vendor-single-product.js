(() => {
	"use strict";

	const imageInput = document.querySelector("#image");
	const preview = document.querySelector("#singleImagePreview");
	const placeholder = document.querySelector("#singleImagePlaceholder");
	const form = document.querySelector("#singleListingForm");

	if (imageInput && preview && placeholder) {
		imageInput.addEventListener("change", () => {
			const file = imageInput.files?.[0];
			if (!file) {
				preview.classList.add("d-none");
				preview.removeAttribute("src");
				placeholder.classList.remove("d-none");
				return;
			}

			const accepted = window.App?.previewImage(file, preview, { alt: "Ảnh thực tế của thẻ" });
			if (accepted) {
				placeholder.classList.add("d-none");
			} else {
				imageInput.value = "";
				placeholder.classList.remove("d-none");
			}
		});
	}

	form?.addEventListener("submit", () => {
		const button = form.querySelector('button[type="submit"]');
		window.App?.setLoading(button, true, { text: button?.dataset.loadingText || "Đang đăng bán..." });
	});
})();
