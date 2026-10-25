document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-overwrite-form], [data-confirm-form]").forEach((form) => {
		form.addEventListener("submit", async (event) => {
			if (form.dataset.confirmed === "true") return;
			const overwrite = form.querySelector('input[name="overwriteExisting"]');
			const requiresConfirmation = form.hasAttribute("data-confirm-form") || overwrite?.checked;
			if (!requiresConfirmation) {
				window.App?.setLoading(form.querySelector('button[type="submit"]'), true, { text: "Đang lưu..." });
				return;
			}

			event.preventDefault();
			const confirmed = await window.App?.showConfirmModal({
				title: form.hasAttribute("data-confirm-form") ? "Xóa bản ghi" : "Xác nhận ghi đè",
				message: form.dataset.confirmMessage
					|| "Bản ghi cùng thẻ và ngày đã tồn tại. Bạn có chắc muốn ghi đè không?",
				confirmText: form.hasAttribute("data-confirm-form") ? "Xóa" : "Ghi đè",
				cancelText: "Hủy",
				type: form.hasAttribute("data-confirm-form") ? "danger" : "warning"
			});
			if (confirmed) {
				form.dataset.confirmed = "true";
				window.App?.setLoading(form.querySelector('button[type="submit"]'), true, {
					text: form.hasAttribute("data-confirm-form") ? "Đang xóa..." : "Đang lưu..."
				});
				form.requestSubmit();
			}
		});
	});
});
