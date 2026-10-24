(() => {
	"use strict";

	const form = document.querySelector("[data-return-form]");
	if (!form) return;
	const detail = form.querySelector("[data-return-detail]");
	const count = form.querySelector("[data-detail-count]");
	const imageInput = form.querySelector("[data-return-image]");
	const videoInput = form.querySelector("[data-return-video]");
	const preview = form.querySelector("[data-image-preview]");
	const imageError = form.querySelector("[data-image-error]");
	const videoError = form.querySelector("[data-video-error]");
	const submit = form.querySelector("[data-return-submit]");
	const label = form.querySelector("[data-submit-label]");
	const spinner = form.querySelector("[data-submit-spinner]");
	const maxImageBytes = 5 * 1024 * 1024;
	const maxVideoBytes = 50 * 1024 * 1024;
	let submitting = false;

	function setFileError(element, message) {
		element.textContent = message;
	}

	function updateSubmit() {
		if (count) count.textContent = String(detail.value.length);
		const validImage = imageInput.files?.length === 1 && !imageError.textContent;
		const length = detail.value.trim().replace(/\s+/g, " ").length;
		const reasonSelected = Boolean(form.querySelector("input[name='reason']:checked"));
		submit.disabled = submitting || !validImage || !reasonSelected || length < 10 || length > 500 || Boolean(videoError.textContent);
	}

	function validateImage() {
		const file = imageInput.files?.[0];
		setFileError(imageError, "");
		if (!file) {
			preview.classList.add("d-none");
			updateSubmit();
			return;
		}
		if (!["image/jpeg", "image/png"].includes(file.type)) {
			imageInput.value = "";
			setFileError(imageError, "Ảnh phải là JPG, JPEG hoặc PNG.");
			preview.classList.add("d-none");
		} else if (file.size >= maxImageBytes) {
			imageInput.value = "";
			setFileError(imageError, "Ảnh phải nhỏ hơn 5 MB.");
			preview.classList.add("d-none");
		} else if (window.App?.previewImage(file, preview, { maxSizeBytes: maxImageBytes, alt: "Ảnh bằng chứng xem trước" })) {
			preview.classList.remove("d-none");
		} else {
			imageInput.value = "";
			setFileError(imageError, "Không thể xem trước ảnh này.");
		}
		updateSubmit();
	}

	function validateVideo() {
		const file = videoInput.files?.[0];
		setFileError(videoError, "");
		const validName = file && /\.(mp4|webm)$/i.test(file.name);
		if (file && (!validName || !["video/mp4", "video/webm"].includes(file.type))) {
			videoInput.value = "";
			setFileError(videoError, "Video phải là MP4 hoặc WebM.");
		} else if (file && file.size > maxVideoBytes) {
			videoInput.value = "";
			setFileError(videoError, "Video không được vượt quá 50 MB.");
		}
		updateSubmit();
	}

	form.querySelectorAll("input[name='reason']").forEach((input) => input.addEventListener("change", updateSubmit));
	detail.addEventListener("input", updateSubmit);
	imageInput.addEventListener("change", validateImage);
	videoInput.addEventListener("change", validateVideo);
	form.addEventListener("submit", (event) => {
		validateImage();
		validateVideo();
		updateSubmit();
		if (submit.disabled || submitting) {
			event.preventDefault();
			return;
		}
		submitting = true;
		submit.disabled = true;
		label.textContent = "Đang gửi...";
		spinner.classList.remove("d-none");
	});
	updateSubmit();
})();
