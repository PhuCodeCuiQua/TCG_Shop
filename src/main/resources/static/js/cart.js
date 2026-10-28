"use strict";

(() => {
	const root = document.getElementById("cart-root");
	if (!root) return;

	const currency = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });
	const api = window.App?.fetchWithCsrf;
	const toast = (message, type = "error") => {
		if (window.App?.showToast) window.App.showToast(message, type);
	};
	let cartState;
	const debounceTimers = new Map();

	function element(tag, className, text) {
		const node = document.createElement(tag);
		if (className) node.className = className;
		if (text !== undefined) node.textContent = String(text);
		return node;
	}

	function button(text, className, onClick, label = text) {
		const node = element("button", className, text);
		node.type = "button";
		node.setAttribute("aria-label", label);
		node.addEventListener("click", (event) => onClick(event.currentTarget));
		return node;
	}

	function safeImageUrl(value) {
		if (typeof value !== "string" || !value.trim()) return null;
		try {
			const url = new URL(value, window.location.origin);
			return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
		} catch {
			return null;
		}
	}

	function dispatchCartUpdated(cartCount) {
		window.dispatchEvent(new CustomEvent("cart:updated", {
			detail: { cartCount: Number(cartCount) || 0 }
		}));
	}

	function reportError(error) {
		if (window.App?.handleAuthenticationError(error)) return;
		const message = error instanceof Error ? error.message : "Không thể tải giỏ hàng.";
		// fetchWithCsrf đã hiển thị toast cho lỗi API; chỉ dùng fallback nếu helper không khả dụng.
		if (!api) toast(message);
	}

	async function request(url, options, buttonElement) {
		if (!api) throw new Error("Không tìm thấy tiện ích gọi API dùng chung.");
		window.App?.setLoading(buttonElement, true, { text: "Đang xử lý..." });
		try {
			return await api(url, options);
		} finally {
			window.App?.setLoading(buttonElement, false);
		}
	}

	function confirmation(options) {
		if (window.App?.showConfirmModal) return window.App.showConfirmModal(options);
		toast("Không thể mở hộp thoại xác nhận. Vui lòng tải lại trang.", "error");
		return Promise.resolve(false);
	}

	function updateCart(response, count) {
		cartState = response;
		dispatchCartUpdated(count ?? response.totalItemCount);
		render(response);
	}

	function allItems() {
		return (cartState?.shops || []).flatMap((shop) => shop.items || []);
	}

	function renderEmpty() {
		const panel = element("div", "text-center border rounded p-5");
		panel.append(element("h2", "h5", "Giỏ hàng đang trống"));
		panel.append(element("p", "text-secondary", "Hãy khám phá sản phẩm và thêm món bạn yêu thích."));
		const link = element("a", "btn btn-primary", "Tiếp tục mua sắm");
		link.href = "/";
		panel.append(link);
		root.replaceChildren(panel);
	}

	function renderLoading() {
		root.replaceChildren(element("p", "text-secondary", "Đang tải giỏ hàng..."));
	}

	function render(response) {
		if (!response || !Array.isArray(response.shops) || response.totalItemCount === 0) {
			renderEmpty();
			return;
		}

		const layout = element("div", "row g-4");
		const listColumn = element("div", "col-12 col-lg-8");
		const summaryColumn = element("aside", "col-12 col-lg-4");
		const list = element("div", "d-grid gap-3");
		const removeUnavailable = button("Xóa các sản phẩm không còn bán", "btn btn-outline-danger btn-sm",
			removeUnavailableItems);
		removeUnavailable.classList.toggle("d-none", !response.hasUnavailableItems);
		listColumn.append(removeUnavailable);

		for (const shop of response.shops) list.append(renderShop(shop));
		listColumn.append(list);
		summaryColumn.append(renderSummary(response));
		layout.append(listColumn, summaryColumn);
		root.replaceChildren(layout);
	}

	function renderShop(shop) {
		const card = element("section", "card");
		const header = element("div", "card-header d-flex align-items-center gap-2");
		const shopCheckbox = document.createElement("input");
		shopCheckbox.type = "checkbox";
		shopCheckbox.className = "form-check-input mt-0";
		shopCheckbox.checked = Boolean(shop.allSelected);
		shopCheckbox.setAttribute("aria-label", `Chọn tất cả sản phẩm của ${shop.shopName || "chi nhánh"}`);
		shopCheckbox.addEventListener("change", () => selectAll(shopCheckbox.checked, shop.shopId));
		header.append(shopCheckbox, element("h2", "h6 mb-0", shop.shopName || "Chi nhánh"));
		const body = element("div", "card-body d-grid gap-3");
		for (const item of shop.items || []) body.append(renderItem(item));
		card.append(header, body);
		return card;
	}

	function renderItem(item) {
		const row = element("article", "row g-3 align-items-center border-bottom pb-3");
		if (!item.available) row.classList.add("opacity-50");
		const selectColumn = element("div", "col-auto");
		const checkbox = document.createElement("input");
		checkbox.type = "checkbox";
		checkbox.className = "form-check-input";
		checkbox.checked = Boolean(item.selected);
		checkbox.disabled = !item.available;
		checkbox.setAttribute("aria-label", `Chọn ${item.productName || "sản phẩm"}`);
		checkbox.addEventListener("change", () => updateSelected(item.itemId, checkbox.checked));
		selectColumn.append(checkbox);

		const media = element("div", "cart-item-thumbnail");
		const imageUrl = safeImageUrl(item.imageUrl);
		if (imageUrl) {
			const image = document.createElement("img");
			image.src = imageUrl;
			image.alt = item.productName ? `Ảnh ${item.productName}` : "Ảnh sản phẩm";
			image.loading = "lazy";
			image.addEventListener("error", () => {
				media.replaceChildren(element("span", "cart-item-thumbnail__placeholder", "✦"));
			}, { once: true });
			media.append(image);
		} else {
			media.append(element("span", "cart-item-thumbnail__placeholder", "✦"));
		}
		const detailColumn = element("div", "col d-flex align-items-center gap-3");
		const productInfo = element("div", "d-flex flex-column justify-content-center");
		productInfo.append(element("h3", "h6 mb-1", item.productName || "Sản phẩm"));
		if (item.cardCondition) productInfo.append(element("p", "small text-secondary mb-1", `Tình trạng: ${item.cardCondition}`));
		const priceLine = element("p", "mb-1");
		if (Number(item.originalPrice) > Number(item.unitPrice)) {
			priceLine.append(element("del", "text-secondary me-2", currency.format(Number(item.originalPrice))));
		}
		priceLine.append(element("span", "fw-semibold", currency.format(Number(item.unitPrice) || 0)));
		productInfo.append(priceLine);
		if (!item.available && item.unavailableReason) {
			productInfo.append(element("p", "small text-danger mb-1", item.unavailableReason));
		}
		if (item.quantityExceedsStock) {
			productInfo.append(element("p", "small text-danger mb-1", `Chỉ còn ${item.stock} sản phẩm`));
		}
		if (item.priceChanged) {
			productInfo.append(element("p", "small text-warning-emphasis mb-1",
				`Giá đã thay đổi từ ${currency.format(Number(item.previousPrice) || 0)}`));
		}
		detailColumn.append(media, productInfo);

		const actions = element("div", "col-12 col-md-auto d-flex align-items-center gap-2 flex-wrap");
		const quantityGroup = element("div", "input-group input-group-sm");
		const minus = button("−", "btn btn-outline-secondary", (clickedButton) => changeQuantity(item, quantity, -1, clickedButton), "Giảm số lượng");
		const quantity = document.createElement("input");
		quantity.type = "number";
		quantity.min = "1";
		quantity.max = "99";
		quantity.value = String(item.quantity);
		quantity.className = "form-control text-center";
		quantity.dataset.itemId = String(item.itemId);
		quantity.setAttribute("aria-label", "Số lượng");
		quantity.disabled = !item.available;
		quantity.addEventListener("input", () => scheduleQuantityUpdate(item, quantity));
		const plus = button("+", "btn btn-outline-secondary", (clickedButton) => changeQuantity(item, quantity, 1, clickedButton), "Tăng số lượng");
		minus.disabled = plus.disabled = !item.available;
		quantityGroup.append(minus, quantity, plus);
		const total = element("span", "fw-semibold", currency.format(Number(item.lineTotal) || 0));
		const remove = button("Xóa", "btn btn-outline-danger btn-sm", (clickedButton) => removeItem(item, clickedButton), `Xóa ${item.productName || "sản phẩm"}`);
		actions.append(quantityGroup, total, remove);
		row.append(selectColumn, detailColumn, actions);
		return row;
	}

	function renderSummary(response) {
		const card = element("section", "card position-sticky top-0");
		const body = element("div", "card-body");
		const allLabel = element("label", "form-check d-flex gap-2 align-items-center mb-3");
		const allCheckbox = document.createElement("input");
		allCheckbox.type = "checkbox";
		allCheckbox.className = "form-check-input mt-0";
		const items = allItems();
		const selectable = items.filter((item) => item.available);
		allCheckbox.checked = selectable.length > 0 && selectable.every((item) => item.selected);
		allCheckbox.indeterminate = selectable.some((item) => item.selected) && !allCheckbox.checked;
		allCheckbox.addEventListener("change", () => selectAll(allCheckbox.checked));
		allLabel.append(allCheckbox, element("span", "", "Chọn tất cả"));
		body.append(allLabel, element("h2", "h5", "Tóm tắt đơn hàng"));
		body.append(element("p", "mb-2", `Sản phẩm đã chọn: ${response.selectedItemCount || 0}`));
		const subtotal = element("div", "d-flex justify-content-between border-top pt-3 mb-3");
		subtotal.append(element("span", "", "Tạm tính"));
		subtotal.append(element("strong", "", currency.format(Number(response.selectedSubtotal) || 0)));
		body.append(subtotal);
		const hasSelectedOverStock = items.some((item) => item.selected && item.quantityExceedsStock);
		const checkout = element("a", "btn btn-primary w-100", "Thanh toán");
		checkout.href = "/checkout";
		checkout.setAttribute("aria-disabled", String(response.selectedItemCount === 0 || hasSelectedOverStock));
		if (response.selectedItemCount === 0 || hasSelectedOverStock) {
			checkout.classList.add("disabled");
			checkout.setAttribute("tabindex", "-1");
		}
		body.append(checkout);
		card.append(body);
		return card;
	}

	function scheduleQuantityUpdate(item, input, triggerButton) {
		const requested = Math.min(99, Math.max(1, Number.parseInt(input.value, 10) || 1));
		input.value = String(requested);
		window.clearTimeout(debounceTimers.get(item.itemId));
		debounceTimers.set(item.itemId, window.setTimeout(() => updateQuantity(item.itemId, requested, triggerButton), 400));
	}

	function changeQuantity(item, input, delta, triggerButton) {
		const current = Number.parseInt(input.value, 10) || 1;
		const next = Math.min(99, Math.max(1, current + delta));
		if (next !== current) {
			input.value = String(next);
			scheduleQuantityUpdate(item, input, triggerButton);
		}
	}

	async function updateQuantity(itemId, quantity, triggerButton) {
		debounceTimers.delete(itemId);
		lockItem(itemId, true);
		try {
			const response = await request(`/api/cart/items/${encodeURIComponent(itemId)}`, {
				method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ quantity })
			}, triggerButton);
			updateCart(response);
		} catch (error) {
			reportError(error);
			await loadCart();
		} finally {
			lockItem(itemId, false);
		}
	}

	async function updateSelected(itemId, selected) {
		try {
			const response = await request(`/api/cart/items/${encodeURIComponent(itemId)}/selected`, {
				method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ selected })
			});
			updateCart(response);
		} catch (error) {
			reportError(error);
			await loadCart();
		}
	}

	async function selectAll(selected, shopId) {
		const body = shopId == null ? { selected } : { selected, shopId };
		try {
			const response = await request("/api/cart/select-all", {
				method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body)
			});
			updateCart(response);
		} catch (error) {
			reportError(error);
			await loadCart();
		}
	}

	async function removeItem(item, triggerButton) {
		const confirmed = await confirmation({
			title: "Xóa sản phẩm",
			message: `Bạn có chắc muốn xóa “${item.productName || "sản phẩm này"}” khỏi giỏ hàng?`,
			confirmText: "Xóa", cancelText: "Giữ lại", type: "danger"
		});
		if (!confirmed) return;
		try {
			const response = await request(`/api/cart/items/${encodeURIComponent(item.itemId)}`, { method: "DELETE" }, triggerButton);
			updateCart(response);
		} catch (error) {
			reportError(error);
		}
	}

	async function removeUnavailableItems(triggerButton) {
		try {
			const response = await request("/api/cart/unavailable", { method: "DELETE" }, triggerButton);
			updateCart(response);
		} catch (error) {
			reportError(error);
		}
	}

	function lockItem(itemId, locked) {
		const row = [...root.querySelectorAll("article")].find((node) =>
			node.querySelector('input[aria-label="Số lượng"]')?.dataset.itemId === String(itemId));
		if (!row) return;
		row.querySelectorAll("button, input").forEach((control) => { control.disabled = locked; });
	}

	async function loadCart() {
		renderLoading();
		try {
			const response = await request("/api/cart");
			cartState = response;
			render(response);
		} catch (error) {
			reportError(error);
		}
	}

	loadCart();
})();
