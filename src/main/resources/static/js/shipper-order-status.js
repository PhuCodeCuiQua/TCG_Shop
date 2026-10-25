"use strict";

(() => {
	const actions = document.getElementById("shipperOrderActions");
	if (!actions || !window.App?.fetchWithCsrf) return;

	const codCheckbox = document.getElementById("shipperCodCollected");
	const codError = document.getElementById("shipperCodError");
	const failureReason = document.getElementById("shipperFailureReason");
	const failureError = document.getElementById("shipperFailureError");
	const codModal = document.getElementById("shipperCodModal");
	const failureModal = document.getElementById("shipperFailureModal");

	async function submitStatus(status, note, codCollectedConfirmed, button) {
		try {
			const result = await window.App.fetchWithCsrf(actions.dataset.endpoint, {
				method: "POST",
				headers: {
					"Content-Type": "application/json",
					Accept: "application/json"
				},
				body: JSON.stringify({ newStatus: status, note, codCollectedConfirmed }),
				button,
				loadingText: "Đang cập nhật...",
				showErrorToast: false
			});
			window.App.showToast(result.message || "Đã cập nhật trạng thái đơn hàng.", "success");
			window.setTimeout(() => window.location.reload(), 700);
		} catch (error) {
			window.App.showToast(error instanceof Error ? error.message : "Không thể cập nhật đơn hàng.", "error");
		}
	}

	document.querySelectorAll(".js-shipper-status").forEach((button) => {
		button.addEventListener("click", () => submitStatus(button.dataset.status, null, false, button));
	});

	document.getElementById("shipperCodSubmit")?.addEventListener("click", (event) => {
		if (!codCheckbox?.checked) {
			if (codError) codError.hidden = false;
			codCheckbox?.classList.add("is-invalid");
			codCheckbox?.focus();
			return;
		}
		if (codError) codError.hidden = true;
		codCheckbox.classList.remove("is-invalid");
		submitStatus("DELIVERED", null, true, event.currentTarget);
	});

	codCheckbox?.addEventListener("change", () => {
		if (codCheckbox.checked && codError) codError.hidden = true;
		codCheckbox.classList.toggle("is-invalid", !codCheckbox.checked && codError && !codError.hidden);
	});

	document.getElementById("shipperFailureSubmit")?.addEventListener("click", (event) => {
		const note = failureReason?.value.trim() || "";
		if (!note) {
			if (failureError) failureError.hidden = false;
			failureReason?.classList.add("is-invalid");
			failureReason?.focus();
			return;
		}
		if (failureError) failureError.hidden = true;
		failureReason?.classList.remove("is-invalid");
		submitStatus("DELIVERY_FAILED", note, false, event.currentTarget);
	});

	failureReason?.addEventListener("input", () => {
		if (failureReason.value.trim() && failureError) failureError.hidden = true;
		failureReason.classList.toggle("is-invalid", !failureReason.value.trim() && failureError && !failureError.hidden);
	});

	codModal?.addEventListener("hidden.bs.modal", () => {
		if (codCheckbox) {
			codCheckbox.checked = false;
			codCheckbox.classList.remove("is-invalid");
		}
		if (codError) codError.hidden = true;
	});

	failureModal?.addEventListener("hidden.bs.modal", () => {
		if (failureReason) {
			failureReason.value = "";
			failureReason.classList.remove("is-invalid");
		}
		if (failureError) failureError.hidden = true;
	});
})();
