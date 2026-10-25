"use strict";

document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-confirm-catalog-delete]").forEach((form) => {
		form.addEventListener("submit", async (event) => {
			if (form.dataset.confirmed === "true") return;
			event.preventDefault();
			const confirmed = await window.App?.showConfirmModal({
				title: "Xác nhận xóa",
				message: form.dataset.confirmMessage || "Bạn có chắc muốn xóa mục này?",
				confirmText: "Xóa",
				cancelText: "Hủy",
				type: "danger"
			});
			if (!confirmed) return;

			form.dataset.confirmed = "true";
			window.App?.setLoading(form.querySelector('button[type="submit"]'), true, { text: "Đang xóa..." });
			form.requestSubmit();
		});
	});
});
