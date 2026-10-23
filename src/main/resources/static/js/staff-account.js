document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-loading-form]").forEach((form) => {
		form.addEventListener("submit", () => {
			if (!form.checkValidity()) {
				return;
			}
			const submitButton = form.querySelector('button[type="submit"]');
			if (submitButton) {
				submitButton.disabled = true;
				submitButton.textContent = "Đang tạo...";
			}
		});
	});
});
