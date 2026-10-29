"use strict";

document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-demo-toast]").forEach((button) => {
		button.addEventListener("click", () => {
			window.App.showToast(`Thông báo loại ${button.dataset.demoToast}.`, button.dataset.demoToast);
		});
	});

	const confirmButton = document.querySelector("[data-demo-confirm]");
	if (confirmButton) {
		confirmButton.addEventListener("click", async () => {
			const confirmed = await window.App.showConfirmModal({
				title: "Xác nhận thao tác",
				message: "Đây là modal dùng chung. Bạn có muốn tiếp tục?",
				confirmText: "Đồng ý",
				cancelText: "Hủy",
				type: "danger"
			});
			window.App.showToast(confirmed ? "Bạn đã xác nhận." : "Bạn đã hủy thao tác.", "info");
		});
	}

	const imageInput = document.querySelector("#demoImage");
	const imagePreview = document.querySelector("[data-image-preview]");
	imageInput?.addEventListener("change", () => {
		const file = imageInput.files?.[0];
		if (file && window.App.previewImage(file, imagePreview)) {
			imagePreview.classList.remove("d-none");
		}
	});

	const spinnerButton = document.querySelector("[data-demo-spinner]");
	spinnerButton?.addEventListener("click", () => {
		window.App.showSpinner(spinnerButton, { text: "Đang thử..." });
		window.setTimeout(() => window.App.hideSpinner(spinnerButton), 1500);
	});

	const fetchButton = document.querySelector("[data-demo-fetch]");
	const fetchResult = document.querySelector("[data-fetch-result]");
	fetchButton?.addEventListener("click", async () => {
		fetchButton.disabled = true;
		try {
			const result = await window.App.fetchWithCsrf(fetchButton.dataset.url, {
				button: fetchButton,
				loadingText: "Đang gọi...",
				showErrorToast: true
			});
			fetchResult.textContent = JSON.stringify(result, null, 2);
			window.App.showToast("Nhận JSON thành công.", "success");
		} catch (error) {
			fetchResult.textContent = error instanceof Error ? error.message : "Yêu cầu thất bại.";
		} finally {
			fetchButton.disabled = false;
		}
	});
});
