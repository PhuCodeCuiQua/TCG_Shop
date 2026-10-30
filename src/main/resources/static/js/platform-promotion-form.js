"use strict";

(() => {
	const form = document.querySelector(".platform-marketing-form");
	if (!form) return;

	const type = form.querySelector("#type");
	const scopePanel = form.querySelector("#promotionScopePanel");
	const scopeRadios = Array.from(form.querySelectorAll('input[name="scope"]'));
	const gameSeriesFields = form.querySelector("#gameSeriesFields");
	const productsFields = form.querySelector("#productsFields");
	const shopFields = form.querySelector("#shopFields");
	const discountAmount = form.querySelector("#discountAmount");
	const minOrderValue = form.querySelector("#minOrderValue");
	const game = form.querySelector("#gameId");
	const series = form.querySelector("#seriesId");
	const products = form.querySelector("#productIds");
	const productSearch = form.querySelector("#productSearch");
	const productShopFilter = form.querySelector("#productShopFilter");
	const productSearchEmpty = form.querySelector("#productSearchEmpty");
	const shop = form.querySelector("#shopId");
	const scopeProductsPreview = form.querySelector("#scopeProductsPreview");
	const shopProductsPreview = form.querySelector("#shopProductsPreview");
	const targetError = form.querySelector("#promotionTargetError");
	const saveButton = form.querySelector(".promotion-submit-btn[type='submit']");
	let productSearchTimer;
	let productRequestSequence = 0;

	function showError(error) {
		const message = error instanceof Error
			? error.message
			: String(error || "Không thể tải dữ liệu phạm vi khuyến mãi.");
		targetError.textContent = message;
		targetError.hidden = false;
		window.App?.showToast(message, "error");
	}

	function clearError() {
		targetError.textContent = "";
		targetError.hidden = true;
	}

	async function fetchOptions(url) {
		if (typeof window.App?.fetchWithCsrf !== "function") {
			throw new Error("Không thể tải dữ liệu phạm vi khuyến mãi. Vui lòng tải lại trang.");
		}
		return window.App.fetchWithCsrf(url, {
			headers: { Accept: "application/json" },
			showErrorToast: false
		});
	}

	function replaceProductOptions(options) {
		const selectedOptions = new Map(
			Array.from(products.selectedOptions)
				.filter((option) => option.value)
				.map((option) => [option.value, option.textContent])
		);
		const renderedIds = new Set();
		const fragment = document.createDocumentFragment();
		options.forEach((option) => {
			const item = new Option(`${option.name} - ${option.detail}`, option.id);
			item.selected = selectedOptions.has(String(option.id));
			renderedIds.add(String(option.id));
			fragment.append(item);
		});
		selectedOptions.forEach((label, id) => {
			if (!renderedIds.has(id)) {
				fragment.append(new Option(label, id, true, true));
			}
		});
		if (options.length === 0) {
			const emptyOption = new Option("Không tìm thấy sản phẩm phù hợp", "");
			emptyOption.disabled = true;
			fragment.append(emptyOption);
		}
		products.replaceChildren(fragment);
		productSearchEmpty.hidden = options.length > 0;
	}

	function renderPreview(list, options) {
		list.replaceChildren();
		if (options.length === 0) {
			const item = document.createElement("li");
			item.textContent = "Chưa có sản phẩm ACTIVE phù hợp.";
			list.append(item);
			return;
		}
		options.forEach((option) => {
			const item = document.createElement("li");
			item.textContent = `${option.name} - ${option.detail}`;
			list.append(item);
		});
	}

	function productOptionsUrl() {
		const params = new URLSearchParams();
		if (selectedScope() === "GAME_SERIES") {
			if (game.value) params.set("gameId", game.value);
			if (series.value) params.set("seriesId", series.value);
		} else if (selectedScope() === "SHOP" && shop.value) {
			params.set("shopId", shop.value);
		} else if (selectedScope() === "PRODUCTS") {
			if (productShopFilter.value) params.set("shopId", productShopFilter.value);
			if (productSearch.value.trim()) params.set("keyword", productSearch.value.trim());
		}
		const query = params.toString();
		return `/api/products${query ? `?${query}` : ""}`;
	}

	async function loadProductsForScope() {
		const requestSequence = ++productRequestSequence;
		clearError();
		try {
			productSearchEmpty.hidden = true;
			const options = await fetchOptions(productOptionsUrl());
			if (requestSequence !== productRequestSequence) return;
			if (!Array.isArray(options)) {
				throw new Error("Máy chủ trả về danh sách sản phẩm không hợp lệ.");
			}
			if (selectedScope() === "GAME_SERIES") {
				renderPreview(scopeProductsPreview, options);
			} else if (selectedScope() === "SHOP") {
				renderPreview(shopProductsPreview, options);
			} else if (selectedScope() === "PRODUCTS") {
				replaceProductOptions(options);
			}
		} catch (error) {
			if (requestSequence !== productRequestSequence) return;
			showError(error);
		}
	}

	async function refreshSeries() {
		const selected = series.value;
		const query = game.value ? `?gameId=${encodeURIComponent(game.value)}` : "";
		try {
			const options = await fetchOptions(`/api/series${query}`);
			series.replaceChildren(new Option("-- Chọn Series (không bắt buộc) --", ""));
			options.forEach((option) => {
				const item = new Option(`${option.name}${option.detail ? ` (${option.detail})` : ""}`, option.id);
				item.selected = String(option.id) === selected;
				series.add(item);
			});
		} catch (error) {
			showError(error);
		}
	}

	function selectedScope() {
		return scopeRadios.find((radio) => radio.checked)?.value || "ALL";
	}

	function updateScopeVisibility() {
		const productPromotion = type.value === "PRODUCT_DISCOUNT";
		const scope = selectedScope();
		scopePanel.hidden = !productPromotion;
		scopeRadios.forEach((radio) => {
			radio.disabled = !productPromotion;
		});

		gameSeriesFields.hidden = !productPromotion || scope !== "GAME_SERIES";
		productsFields.hidden = !productPromotion || scope !== "PRODUCTS";
		shopFields.hidden = !productPromotion || scope !== "SHOP";
		game.disabled = !productPromotion || scope !== "GAME_SERIES";
		series.disabled = !productPromotion || scope !== "GAME_SERIES";
		products.disabled = !productPromotion || scope !== "PRODUCTS";
		shop.disabled = !productPromotion || scope !== "SHOP";
	}

	form.addEventListener("submit", (event) => {
		if (event.submitter !== saveButton) {
			event.preventDefault();
		}
	});

	game.addEventListener("change", async () => {
		clearError();
		series.value = "";
		await refreshSeries();
		if (selectedScope() === "GAME_SERIES") await loadProductsForScope();
	});

	series.addEventListener("change", () => {
		clearError();
		if (selectedScope() === "GAME_SERIES") void loadProductsForScope();
	});

	shop.addEventListener("change", () => {
		clearError();
		if (selectedScope() === "SHOP") void loadProductsForScope();
	});

	scopeRadios.forEach((radio) => {
		radio.addEventListener("change", () => {
			clearError();
			updateScopeVisibility();
			if (radio.value === "GAME_SERIES") {
				void refreshSeries().then(loadProductsForScope);
			} else if (radio.value === "SHOP" || radio.value === "PRODUCTS") {
				void loadProductsForScope();
			}
		});
	});

	productSearch.addEventListener("input", () => {
		window.clearTimeout(productSearchTimer);
		productSearchTimer = window.setTimeout(() => {
			if (selectedScope() === "PRODUCTS") void loadProductsForScope();
		}, 300);
	});

	productShopFilter.addEventListener("change", () => {
		window.clearTimeout(productSearchTimer);
		if (selectedScope() === "PRODUCTS") void loadProductsForScope();
	});

	type.addEventListener("change", () => {
		clearError();
		if (type.value === "PRODUCT_DISCOUNT") {
			discountAmount.value = "";
			minOrderValue.value = "";
		} else {
			const allScope = scopeRadios.find((radio) => radio.value === "ALL");
			if (allScope) allScope.checked = true;
			game.value = "";
			series.value = "";
			shop.value = "";
			products.selectedIndex = -1;
		}
		updateScopeVisibility();
	});

	updateScopeVisibility();
	if (selectedScope() === "GAME_SERIES") {
		void refreshSeries().then(loadProductsForScope);
	} else if (selectedScope() === "PRODUCTS" || selectedScope() === "SHOP") {
		void loadProductsForScope();
	}
})();
