"use strict";

(() => {
	const form = document.querySelector(".platform-marketing-form");
	if (!form) return;

	const type = form.querySelector("#type");
	const applyToAll = form.querySelector("#applyToAll");
	const scopePanel = form.querySelector("#promotionScopePanel");
	const targetFields = form.querySelector("#promotionTargetFields");
	const discountAmount = form.querySelector("#discountAmount");
	const minOrderValue = form.querySelector("#minOrderValue");
	const game = form.querySelector("#gameId");
	const series = form.querySelector("#seriesId");
	const products = form.querySelector("#productIds");
	const targetError = form.querySelector("#promotionTargetError");
	const allTargets = [game, series, products];

	function showError(message) {
		targetError.textContent = message;
		targetError.hidden = false;
	}

	function clearError() {
		targetError.textContent = "";
		targetError.hidden = true;
	}

	async function fetchOptions(url) {
		const response = await fetch(url, {
			credentials: "same-origin",
			headers: { Accept: "application/json" }
		});
		if (!response.ok) {
			throw new Error(`Không thể tải dữ liệu phạm vi khuyến mãi (HTTP ${response.status}).`);
		}
		return response.json();
	}

	function replaceOptions(select, options, emptyLabel, selectedValues = []) {
		select.replaceChildren();
		if (!select.multiple) {
			select.add(new Option(emptyLabel, ""));
		} else if (options.length === 0) {
			const emptyOption = new Option("Không có sản phẩm phù hợp", "");
			emptyOption.disabled = true;
			select.add(emptyOption);
			return;
		}
		options.forEach((option) => {
			const label = select === products
				? `${option.name} - ${option.detail}`
				: `${option.name}${option.detail ? ` (${option.detail})` : ""}`;
			const item = new Option(label, option.id);
			item.selected = selectedValues.includes(String(option.id));
			select.add(item);
		});
	}

	async function refreshSeries() {
		const selected = series.value;
		const query = game.value ? `?gameId=${encodeURIComponent(game.value)}` : "";
		try {
			const options = await fetchOptions(`/api/series${query}`);
			replaceOptions(series, options, "-- Tất cả series --", selected ? [selected] : []);
		} catch (error) {
			showError(error.message);
		}
	}

	async function refreshProducts() {
		const selected = Array.from(products.selectedOptions, (option) => option.value);
		const query = series.value ? `?seriesId=${encodeURIComponent(series.value)}` : "";
		try {
			const options = await fetchOptions(`/api/products${query}`);
			replaceOptions(products, options, "Chọn sản phẩm (không bắt buộc)", selected);
		} catch (error) {
			showError(error.message);
		}
	}

	function updateScopeVisibility() {
		const isProductPromotion = type.value === "PRODUCT_DISCOUNT";
		scopePanel.hidden = !isProductPromotion;
		targetFields.hidden = !isProductPromotion || applyToAll.checked;
		allTargets.forEach((element) => {
			element.disabled = !isProductPromotion || applyToAll.checked;
		});
		applyToAll.disabled = !isProductPromotion;
	}

	game.addEventListener("change", async () => {
		clearError();
		series.value = "";
		await refreshSeries();
		await refreshProducts();
	});

	series.addEventListener("change", () => {
		clearError();
		void refreshProducts();
	});
	applyToAll.addEventListener("change", () => {
		clearError();
		updateScopeVisibility();
		if (applyToAll.checked) {
			game.value = "";
			series.value = "";
			products.selectedIndex = -1;
		}
	});
	type.addEventListener("change", () => {
		clearError();
		if (type.value === "PRODUCT_DISCOUNT") {
			discountAmount.value = "";
			minOrderValue.value = "";
		}
		updateScopeVisibility();
	});

	updateScopeVisibility();
	if (type.value === "PRODUCT_DISCOUNT" && !applyToAll.checked) {
		clearError();
		void refreshSeries();
		void refreshProducts();
	}
})();
