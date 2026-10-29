"use strict";

(() => {
	if (window.bootstrap?.Tooltip) document.querySelectorAll('[data-bs-toggle=tooltip]').forEach((node) => window.bootstrap.Tooltip.getOrCreateInstance(node));
	const updateSelectedCount = () => {
		const target = document.querySelector("[data-selected-count]");
		const checkboxes = [...document.querySelectorAll("[data-product-checkbox]")];
		const selectedCount = checkboxes.filter((checkbox) => checkbox.checked).length;
		const selectAll = document.querySelector("[data-select-all-products]");
		if (selectAll) {
			selectAll.checked = checkboxes.length > 0 && selectedCount === checkboxes.length;
			selectAll.indeterminate = selectedCount > 0 && selectedCount < checkboxes.length;
		}
		if (target) {
			target.textContent = checkboxes.length > 0 && selectedCount === checkboxes.length
				? "Tất cả đã chọn"
				: `${selectedCount} đã chọn`;
		}
	};

	const searchInput = document.querySelector("[data-promotion-search]");
	searchInput?.addEventListener("input", () => {
		const query = searchInput.value.trim().toLocaleLowerCase("vi");
		let visibleCount = 0;
		document.querySelectorAll("[data-product-name]").forEach((option) => {
			const visible = option.dataset.productName.includes(query);
			option.classList.toggle("d-none", !visible);
			if (visible) visibleCount += 1;
		});
		document.querySelector("[data-search-empty]")?.classList.toggle("d-none", visibleCount > 0);
	});

	document.querySelectorAll("[data-product-checkbox]").forEach((checkbox) => {
		checkbox.addEventListener("change", updateSelectedCount);
	});
	document.querySelector("[data-select-all-products]")?.addEventListener("change", (event) => {
		const checked = event.currentTarget.checked;
		document.querySelectorAll("[data-product-checkbox]").forEach((checkbox) => {
			checkbox.checked = checked;
		});
		updateSelectedCount();
	});
	updateSelectedCount();

	document.querySelectorAll("form[data-loading-form]").forEach((form) => {
		form.addEventListener("submit", (event) => {
			if (form.hasAttribute("data-confirm-promotion-delete") && form.dataset.confirmed !== "true") return;
			const button = event.submitter || form.querySelector("button[type='submit']");
			if (button && window.App?.setLoading) {
				window.App.setLoading(button, true, { text: button.dataset.loadingText || "Đang xử lý..." });
			}
		});
	});

	document.querySelectorAll("form[data-confirm-promotion-delete]").forEach((form) => {
		form.addEventListener("submit", async (event) => {
			if (form.dataset.confirmed === "true") return;
			event.preventDefault();
			const confirmed = await window.App?.showConfirmModal({
				title: "Ngừng chương trình khuyến mãi?",
				message: "Chương trình sẽ được chuyển sang trạng thái tạm dừng.",
				confirmText: "Xóa",
				cancelText: "Hủy",
				type: "danger"
			});
			if (!confirmed) return;
			form.dataset.confirmed = "true";
			form.requestSubmit();
		});
	});
})();
