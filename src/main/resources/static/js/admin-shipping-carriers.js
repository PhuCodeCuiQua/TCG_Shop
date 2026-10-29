document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-confirm-form]").forEach((form) => {
		form.addEventListener("submit", async (event) => {
			if (form.dataset.confirmed === "true") return;
			event.preventDefault();
			const confirmed = await window.App?.showConfirmModal({
				title: "Xóa nhà vận chuyển",
				message: form.dataset.confirmMessage || "Bạn có chắc muốn xóa hoặc ẩn nhà vận chuyển này?",
				confirmText: "Xóa / Ẩn",
				cancelText: "Hủy",
				type: "danger"
			});
			if (confirmed) {
				form.dataset.confirmed = "true";
				form.requestSubmit();
			}
		});
	});

	document.querySelectorAll("[data-loading-form]").forEach((form) => {
		form.addEventListener("submit", () => {
			if (!form.checkValidity()) {
				return;
			}
			window.App?.setLoading(form.querySelector('button[type="submit"]'), true, { text: "Đang lưu..." });
		});
	});
});
