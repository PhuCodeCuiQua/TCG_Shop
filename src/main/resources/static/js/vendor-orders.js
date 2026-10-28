"use strict";

(() => {
	const detail = document.querySelector("main[data-order-id][data-status-url]");
	if (!detail) return;

	const actionPanel = detail.querySelector("[data-order-actions]");
	const returnActionPanel = detail.querySelector("[data-return-actions]");
	const statusBadge = detail.querySelector("[data-order-status-badge]");
	const timeline = detail.querySelector(".order-timeline");
	const rejectModalElement = document.querySelector("#rejectOrderModal");
	const rejectReason = document.querySelector("#rejectReason");
	const rejectReturnModalElement = document.querySelector("#rejectReturnModal");
	const rejectReturnForm = document.querySelector("#rejectReturnForm");
	const rejectReturnReason = document.querySelector("#rejectReturnReason");
	const rejectReturnSubmit = document.querySelector("#rejectReturnSubmit");
	const rejectReturnReasonError = document.querySelector("#rejectReturnReasonError");
	let pendingRejectReason = null;

	const confirmAndChange = async (newStatus, note, button) => {
		const isReject = newStatus === "CANCELLED";
		const accepted = await window.App?.showConfirmModal({
			title: isReject ? "Xác nhận từ chối đơn?" : "Xác nhận đơn hàng?",
			message: isReject ? `Lý do: ${note}` : "Đơn hàng sẽ được chuyển sang trạng thái đã xác nhận.",
			confirmText: isReject ? "Từ chối đơn" : "Xác nhận đơn",
			cancelText: "Quay lại",
			type: isReject ? "danger" : "success"
		});
		if (!accepted) return;

		try {
			const result = await window.App.fetchWithCsrf(detail.dataset.statusUrl, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ newStatus, note }),
				button,
				loadingText: "Đang cập nhật..."
			});
			updateOrderView(result, note);
			window.App.showToast(isReject ? "Đã từ chối đơn hàng" : "Đã xác nhận đơn hàng", "success");
		} catch {
			// fetchWithCsrf hiển thị thông báo lỗi từ response JSON qua toast chung.
		}
	};

	const updateOrderView = (result, note) => {
		if (!result || !statusBadge) return;
		statusBadge.textContent = result.statusLabel;
		statusBadge.dataset.status = result.status;
		statusBadge.className = `badge order-status-badge order-detail-status text-bg-${result.statusColor}`;
		actionPanel?.remove();
		if (timeline) {
			const entry = document.createElement("li");
			entry.className = "order-timeline-entry order-timeline-entry-new";
			const marker = document.createElement("span");
			marker.className = `order-timeline-marker order-timeline-${result.statusColor}`;
			marker.setAttribute("aria-hidden", "true");
			const body = document.createElement("div");
			const label = document.createElement("strong");
			label.textContent = result.statusLabel;
			const time = document.createElement("time");
			time.className = "d-block order-timeline-time";
			time.dateTime = new Date().toISOString();
			time.textContent = new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date());
			body.append(label, time);
			if (note) {
				const noteText = document.createElement("p");
				noteText.className = "order-timeline-note mb-0";
				noteText.textContent = note;
				body.append(noteText);
			}
			entry.append(marker, body);
			timeline.querySelector(".order-timeline-empty")?.remove();
			timeline.append(entry);
		}
	};

	const updateReturnStatus = (status, statusLabel, statusColor, note) => {
		updateOrderView({ status, statusLabel, statusColor }, note);
		actionPanel?.remove();
		returnActionPanel?.remove();
	};

	const clearReturnReasonError = () => {
		rejectReturnReason?.classList.remove("is-invalid");
		rejectReturnReason?.removeAttribute("aria-invalid");
		rejectReturnReasonError?.classList.remove("d-block");
	};

	detail.querySelector("[data-approve-return]")?.addEventListener("click", async (event) => {
		const button = event.currentTarget;
		try {
			const response = await window.App.fetchWithCsrf(detail.dataset.returnApproveUrl, {
				method: "POST",
				button,
				loadingText: "Đang xử lý..."
			});
			updateReturnStatus("RETURN_APPROVED", "Đã duyệt trả hàng", "info", null);
			window.App.showToast(response.message, "success");
		} catch {
			// fetchWithCsrf hiển thị thông báo lỗi từ response JSON qua toast chung.
		}
	});

	detail.querySelector("[data-open-reject-return-modal]")?.addEventListener("click", () => {
		if (!window.bootstrap?.Modal || !rejectReturnModalElement) {
			window.App.showToast("Không thể mở biểu mẫu từ chối yêu cầu trả hàng.", "error");
			return;
		}
		rejectReturnForm?.reset();
		clearReturnReasonError();
		window.bootstrap.Modal.getOrCreateInstance(rejectReturnModalElement).show();
	});

	rejectReturnReason?.addEventListener("input", clearReturnReasonError);
	rejectReturnForm?.addEventListener("submit", async (event) => {
		event.preventDefault();
		const reason = rejectReturnReason?.value.trim() || "";
		if (!reason) {
			rejectReturnReason?.classList.add("is-invalid");
			rejectReturnReason?.setAttribute("aria-invalid", "true");
			rejectReturnReasonError?.classList.add("d-block");
			rejectReturnReason?.focus();
			return;
		}

		try {
			const response = await window.App.fetchWithCsrf(detail.dataset.returnRejectUrl, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ reason }),
				button: rejectReturnSubmit,
				loadingText: "Đang gửi..."
			});
			updateReturnStatus("RETURN_REJECTED", "Đã từ chối trả hàng", "danger", reason);
			window.bootstrap.Modal.getOrCreateInstance(rejectReturnModalElement).hide();
			window.App.showToast(response.message, "success");
		} catch {
			// fetchWithCsrf hiển thị thông báo lỗi từ response JSON qua toast chung.
		}
	});

	detail.querySelectorAll("[data-order-change]").forEach((button) => {
		button.addEventListener("click", () => confirmAndChange(button.dataset.orderChange, null, button));
	});

	document.querySelector("[data-open-reject-modal]")?.addEventListener("click", () => {
		if (!window.bootstrap?.Modal || !rejectModalElement) {
			window.App.showToast("Không thể mở biểu mẫu từ chối đơn hàng.", "error");
			return;
		}
		rejectReason?.classList.remove("is-invalid");
		window.bootstrap.Modal.getOrCreateInstance(rejectModalElement).show();
	});

	document.querySelector("[data-confirm-reject]")?.addEventListener("click", () => {
		const note = rejectReason?.value.trim() || "";
		if (!note) {
			rejectReason?.classList.add("is-invalid");
			rejectReason?.focus();
			return;
		}
		rejectReason?.classList.remove("is-invalid");
		pendingRejectReason = note;
		window.bootstrap.Modal.getOrCreateInstance(rejectModalElement).hide();
	});

	rejectModalElement?.addEventListener("hidden.bs.modal", () => {
		if (!pendingRejectReason) return;
		const note = pendingRejectReason;
		pendingRejectReason = null;
		confirmAndChange("CANCELLED", note, document.querySelector("[data-confirm-reject]"));
	});
})();
