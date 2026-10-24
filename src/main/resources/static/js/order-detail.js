(() => {
	"use strict";

	const modal = document.getElementById("cancelOrderModal");
	const form = modal?.querySelector("[data-cancel-form]");
	if (!modal || !form) return;

	const radios = [...form.querySelectorAll("[data-cancel-reason]")];
	const detailArea = form.querySelector("[data-other-detail]");
	const detail = form.querySelector("[data-cancel-detail]");
	const counter = form.querySelector("[data-detail-count]");
	const submit = form.querySelector("[data-cancel-submit]");
	const submitLabel = form.querySelector("[data-submit-label]");
	const spinner = form.querySelector("[data-submit-spinner]");
	let submitting = false;

	function updateForm() {
		const selected = radios.find((radio) => radio.checked)?.value;
		const needsDetail = selected === "OTHER";
		detailArea.hidden = !needsDetail;
		detail.required = needsDetail;
		counter.textContent = String(detail.value.length);
		const normalizedLength = detail.value.trim().replace(/\s+/g, " ").length;
		submit.disabled = submitting || !selected || (needsDetail && normalizedLength < 10);
	}

	radios.forEach((radio) => radio.addEventListener("change", updateForm));
	detail.addEventListener("input", updateForm);
	form.addEventListener("submit", (event) => {
		updateForm();
		if (submit.disabled || submitting) {
			event.preventDefault();
			return;
		}
		submitting = true;
		submit.disabled = true;
		submitLabel.textContent = "Đang hủy...";
		spinner.classList.remove("d-none");
	});
	modal.addEventListener("hidden.bs.modal", () => {
		submitting = false;
		form.reset();
		submitLabel.textContent = "Xác nhận hủy";
		spinner.classList.add("d-none");
		updateForm();
	});
	updateForm();
})();
