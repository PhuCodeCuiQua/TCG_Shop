"use strict";

(() => {
	const statusLabels = { ACTIVE: "Đang bán", INACTIVE: "Ngừng bán" };
	const updateStatusBadge = (row, status) => {
		const badge = row.querySelector(".shop-product-status");
		if (!badge) return;
		badge.classList.remove("shop-status-active", "shop-status-inactive", "shop-status-stockout", "shop-status-banned");
		badge.classList.add(status === "ACTIVE" ? "shop-status-active" : "shop-status-inactive");
		badge.textContent = statusLabels[status] || status;
	};
	const requestJson = (url, method) => window.App.fetchWithCsrf(url, {
		method,
		headers: { Accept: "application/json" },
		showErrorToast: true
	});

	document.querySelectorAll('[data-product-action="toggle"]').forEach((button) => {
		button.addEventListener("click", async () => {
			const id = button.dataset.productId;
			try {
				const response = await requestJson(`/vendor/products/${encodeURIComponent(id)}/toggle-status`, "POST");
				const row = button.closest("[data-product-row]");
				if (row) updateStatusBadge(row, response.status);
				window.App.showToast(response.message || "Đã cập nhật trạng thái sản phẩm", "success");
			} catch { /* fetchWithCsrf already shows the error toast. */ }
		});
	});

	document.querySelectorAll('[data-product-action="delete"]').forEach((button) => {
		button.addEventListener("click", async () => {
			const confirmed = await window.App.showConfirmModal({
				title: "Ẩn sản phẩm khỏi gian hàng?",
				message: "Sản phẩm sẽ chuyển sang trạng thái Ngừng bán. Bạn có thể bật lại sau.",
				confirmText: "Xóa",
				cancelText: "Hủy",
				type: "danger"
			});
			if (!confirmed) return;
			try {
				const response = await requestJson(`/vendor/products/${encodeURIComponent(button.dataset.productId)}`, "DELETE");
				window.App.showToast(response.message || "Đã ẩn sản phẩm", "success");
				button.closest("[data-product-row]")?.remove();
				if (!document.querySelector("[data-product-row]")) window.location.reload();
			} catch { /* fetchWithCsrf already shows the error toast. */ }
		});
	});

	document.querySelectorAll("#vendorProductFilterForm, #vendorProductFilterOptionsForm").forEach((form) => {
		form.addEventListener("submit", (event) => {
			const button = event.submitter || form.querySelector("button[type='submit']");
			if (button && window.App?.setLoading) {
				window.App.setLoading(button, true, { text: button.dataset.loadingText || "Đang tải..." });
			}
		});
	});
})();
