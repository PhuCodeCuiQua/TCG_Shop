(() => {
	"use strict";

	const form = document.querySelector("#otherProductForm");
	const typeSelect = document.querySelector("#type");
	const sealedFields = document.querySelector(".js-sealed-fields");
	const supplyFields = document.querySelector(".js-supply-fields");
	const selectedTypeLabel = document.querySelector(".js-selected-type-label");
	const imageInput = document.querySelector("#image");
	const imagePreview = document.querySelector("#otherImagePreview");
	const imagePlaceholder = document.querySelector("#otherImagePlaceholder");
	const sealedTypes = new Set(["BOOSTER_BOX", "BOOSTER_PACK", "BUNDLE"]);
	const typeLabels = new Map([
		["BOOSTER_BOX", "Booster Box"],
		["BOOSTER_PACK", "Booster Pack"],
		["BUNDLE", "Bundle"],
		["SUPPLY", "Phụ kiện (Supply)"]
	]);
	let previousType = "";

	const setGroupEnabled = (group, enabled) => {
		if (!group) return;
		group.hidden = !enabled;
		group.querySelectorAll("input, select, textarea").forEach((field) => {
			field.disabled = !enabled;
		});
	};

	const clearGroup = (group) => {
		group?.querySelectorAll("input, select, textarea").forEach((field) => {
			if (field.type === "file") {
				field.value = "";
			} else if (field.tagName === "SELECT") {
				field.selectedIndex = 0;
			} else {
				field.value = "";
			}
		});
	};

	const updateProductTypeFields = (clearPrevious = false) => {
		if (!typeSelect) return;
		const selectedType = typeSelect.value;
		const isSealed = sealedTypes.has(selectedType);
		const isSupply = selectedType === "SUPPLY";

		if (clearPrevious && previousType && previousType !== selectedType) {
			clearGroup(sealedFields);
			clearGroup(supplyFields);
		}
		setGroupEnabled(sealedFields, isSealed);
		setGroupEnabled(supplyFields, isSupply);
		if (selectedTypeLabel) {
			selectedTypeLabel.textContent = typeLabels.get(selectedType) || "Chưa chọn";
		}
		previousType = selectedType;
	};

	updateProductTypeFields();
	typeSelect?.addEventListener("change", () => updateProductTypeFields(true));

	imageInput?.addEventListener("change", () => {
		const file = imageInput.files?.[0];
		if (!file) {
			imagePreview?.classList.add("d-none");
			imagePreview?.removeAttribute("src");
			imagePlaceholder?.classList.remove("d-none");
			return;
		}

		const accepted = window.App?.previewImage(file, imagePreview, { alt: "Ảnh sản phẩm xem trước" });
		if (accepted) {
			imagePlaceholder?.classList.add("d-none");
		} else {
			imageInput.value = "";
			imagePlaceholder?.classList.remove("d-none");
		}
	});

	form?.addEventListener("submit", () => {
		const button = form.querySelector('button[type="submit"]');
		window.App?.setLoading(button, true, { text: button?.dataset.loadingText || "Đang đăng sản phẩm..." });
	});
})();
