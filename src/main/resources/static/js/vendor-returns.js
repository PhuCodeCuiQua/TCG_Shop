"use strict";

(() => {
	const rows = document.getElementById("vendorReturnRows");
	if (!rows) return;

	const imageModal = document.getElementById("vendorReturnImageModal");
	const largeImage = document.getElementById("vendorReturnLargeImage");
	const rejectModal = document.getElementById("vendorReturnRejectModal");
	const rejectForm = document.getElementById("vendorReturnRejectForm");
	const rejectReason = document.getElementById("vendorReturnRejectReason");
	const rejectError = document.getElementById("vendorReturnRejectError");
	const rejectSubmit = document.getElementById("vendorReturnRejectSubmit");
	let rejectingOrderId = null;

	const showToast = (message, type) => window.App?.showToast(message, type);
	const removeRow = (orderId) => {
		rows.querySelector(`[data-return-row="${String(orderId)}"]`)?.remove();
		if (!rows.querySelector("[data-return-row]")) {
			const emptyState = document.getElementById("vendorReturnEmptyState");
			if (emptyState) emptyState.classList.remove("d-none");
		}
	};

	const clearReasonError = () => {
		rejectReason.classList.remove("is-invalid");
		rejectReason.removeAttribute("aria-invalid");
		rejectError.classList.add("d-none");
	};

	imageModal?.addEventListener("show.bs.modal", (event) => {
		const trigger = event.relatedTarget;
		const imageUrl = trigger?.getAttribute("data-proof-image");
		if (imageUrl) largeImage.src = imageUrl;
	});
	imageModal?.addEventListener("hidden.bs.modal", () => largeImage.removeAttribute("src"));

	rejectModal?.addEventListener("show.bs.modal", (event) => {
		rejectingOrderId = event.relatedTarget?.getAttribute("data-order-id") || null;
		rejectForm.reset();
		clearReasonError();
	});
	rejectReason?.addEventListener("input", clearReasonError);

	rows.addEventListener("click", async (event) => {
		const button = event.target.closest(".js-return-approve");
		if (!button) return;
		const orderId = button.getAttribute("data-order-id");
		try {
			const response = await window.App.fetchWithCsrf(
				`/api/vendor/returns/${encodeURIComponent(orderId)}/approve`,
				{ method: "POST", button, loadingText: "Đang xử lý..." });
			showToast(response.message, "success");
			removeRow(orderId);
		} catch {
			// fetchWithCsrf tự hiển thị toast lỗi.
		}
	});

	rejectForm.addEventListener("submit", async (event) => {
		event.preventDefault();
		const reason = rejectReason.value.trim();
		if (!reason) {
			rejectReason.classList.add("is-invalid");
			rejectReason.setAttribute("aria-invalid", "true");
			rejectError.classList.remove("d-none");
			rejectReason.focus();
			return;
		}
		if (!rejectingOrderId) {
			showToast("Không xác định được yêu cầu trả hàng.", "error");
			return;
		}

		try {
			const response = await window.App.fetchWithCsrf(
				`/api/vendor/returns/${encodeURIComponent(rejectingOrderId)}/reject`,
				{
					method: "POST",
					headers: { "Content-Type": "application/json" },
					body: JSON.stringify({ reason }),
					button: rejectSubmit,
					loadingText: "Đang gửi..."
				});
			showToast(response.message, "success");
			removeRow(rejectingOrderId);
			window.bootstrap.Modal.getOrCreateInstance(rejectModal).hide();
		} catch {
			// fetchWithCsrf tự hiển thị toast lỗi.
		}
	});
})();
