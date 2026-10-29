document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-confirm-form]").forEach((form) => {
		form.addEventListener("submit", (event) => {
			const message = form.dataset.confirmMessage || "Bạn có chắc muốn thực hiện thao tác này?";
			if (!window.confirm(message)) {
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
				submitButton.textContent = "Đang xử lý...";
			}
		});
	});
});
