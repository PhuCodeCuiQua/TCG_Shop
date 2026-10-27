"use strict";

(() => {
	if (window.bootstrap?.Tooltip) document.querySelectorAll('[data-bs-toggle=tooltip]').forEach((node) => window.bootstrap.Tooltip.getOrCreateInstance(node));
	const codeInput = document.querySelector("#code");
	codeInput?.addEventListener("input", () => {
		const start = codeInput.selectionStart;
		const end = codeInput.selectionEnd;
		codeInput.value = codeInput.value.toLocaleUpperCase("en-US");
		codeInput.setSelectionRange(start, end);
	});

	document.querySelectorAll("form[data-loading-form]").forEach((form) => {
		form.addEventListener("submit", (event) => {
			if (form.hasAttribute("data-confirm-coupon-delete") && form.dataset.confirmed !== "true") return;
			const button = event.submitter || form.querySelector("button[type='submit']");
			if (button && window.App?.setLoading) {
				window.App.setLoading(button, true, { text: button.dataset.loadingText || "Đang xử lý..." });
			}
		});
	});

	document.querySelectorAll("form[data-confirm-coupon-delete]").forEach((form) => {
		form.addEventListener("submit", async (event) => {
			if (form.dataset.confirmed === "true") return;
			event.preventDefault();
			const confirmed = await window.App?.showConfirmModal({
				title: "Tạm dừng mã giảm giá?",
				message: "Mã sẽ không thể được sử dụng nữa. Bạn vẫn có thể xem lại trong danh sách.",
				confirmText: "Tạm dừng",
				cancelText: "Hủy",
				type: "danger"
			});
			if (!confirmed) return;
			form.dataset.confirmed = "true";
			form.requestSubmit();
		});
	});
})();
