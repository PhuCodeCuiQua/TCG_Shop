document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-confirm-form]").forEach((form) => {
		form.addEventListener("submit", (event) => {
			if (!window.confirm("Bạn có chắc muốn xóa hoặc ẩn nhà vận chuyển này?")) {
				event.preventDefault();
			}
		});
	});

	document.querySelectorAll("[data-loading-form]").forEach((form) => {
		form.addEventListener("submit", () => {
			if (!form.checkValidity()) {
				return;
			}
			const submitButton = form.querySelector('button[type="submit"]');
			if (submitButton) {
				submitButton.disabled = true;
				submitButton.textContent = "Đang lưu...";
			}
		});
	});
});
