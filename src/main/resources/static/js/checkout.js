"use strict";

(() => {
	const root = document.getElementById("checkout-root");
	if (!root) return;

	const currency = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });
	const api = window.App?.fetchWithCsrf;
	const state = {
		addressId: null,
		shops: new Map(),
		platformCouponCode: "",
		paymentMethod: "COD"
	};
	const idempotencyTokenField = document.getElementById("idempotency-token");
	let lastPreview = null;
	let isPlacing = false;
	let sellerNote = "";
	let requestSequence = 0;
	let activeRequest = null;
	let loading = false;

	function element(tag, className, text) {
		const node = document.createElement(tag);
		if (className) node.className = className;
		if (text !== undefined && text !== null) node.textContent = String(text);
		return node;
	}

	function button(label, className, onClick) {
		const node = element("button", className, label);
		node.type = "button";
		node.addEventListener("click", onClick);
		return node;
	}

	function formatMoney(value) {
		return currency.format(Number(value) || 0);
	}

	function showToast(message, type = "error") {
		if (window.App?.showToast) window.App.showToast(message, type);
		else window.alert(String(message || "Đã xảy ra lỗi."));
	}

	function errorMessage(error) {
		return error instanceof Error ? error.message : "Không thể tải dữ liệu thanh toán.";
	}

	function apiStatus(error) {
		if (Number(error?.status)) return Number(error.status);
		const match = errorMessage(error).match(/HTTP\s+(\d{3})/i);
		return match ? Number(match[1]) : 0;
	}

	function reportApiError(error) {
		if (window.App?.handleAuthenticationError(error)) return;
		const message = errorMessage(error);
		showToast(message, "error");
	}

	async function requestJson(url, options = {}) {
		if (!api) throw new Error("Không tìm thấy tiện ích gọi API dùng chung.");
		return api(url, { ...options, showErrorToast: false });
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

	function renderInitialLoading() {
		const skeleton = element("div", "row g-4");
		const left = element("div", "col-12 col-lg-8");
		const right = element("div", "col-12 col-lg-4");
		left.append(element("div", "placeholder-glow border rounded p-4", "Đang tải thông tin thanh toán…"));
		right.append(element("div", "placeholder-glow border rounded p-4", "Đang tính tổng đơn hàng…"));
		root.replaceChildren(skeleton);
		skeleton.append(left, right);
	}

	function setLoading(value) {
		loading = value;
		root.setAttribute("aria-busy", String(value));
		const placeOrder = root.querySelector("#place-order");
		if (placeOrder) placeOrder.disabled = value || isPlacing || !lastPreview?.canPlaceOrder;

		let overlay = root.querySelector(".checkout-loading-overlay");
		if (value && !overlay) {
			root.classList.add("position-relative");
			overlay = element("div", "checkout-loading-overlay position-absolute top-0 start-0 w-100 h-100 bg-white bg-opacity-75 d-flex justify-content-center align-items-start pt-4 rounded");
			overlay.setAttribute("aria-label", "Đang cập nhật thanh toán");
			overlay.append(element("span", "spinner-border text-primary", ""));
			overlay.firstElementChild.setAttribute("role", "status");
			root.append(overlay);
		} else if (!value && overlay) {
			overlay.remove();
		}
	}

	function buildRequest() {
		const shops = [];
		for (const [shopId, choice] of state.shops.entries()) {
			const option = { shopId: Number(shopId) };
			if (choice.carrierId != null && choice.carrierId !== "") option.carrierId = Number(choice.carrierId);
			const couponCode = String(choice.couponCode || "").trim();
			if (couponCode) option.couponCode = couponCode;
			shops.push(option);
		}
		const request = {
			addressId: state.addressId,
			shops,
			paymentMethod: state.paymentMethod
		};
		const platformCouponCode = String(state.platformCouponCode || "").trim();
		if (platformCouponCode) request.platformCouponCode = platformCouponCode;
		return request;
	}

	function rememberDefaultCarriers(preview) {
		for (const shop of preview.shops || []) {
			const key = String(shop.shopId);
			const choice = state.shops.get(key) || { carrierId: null, couponCode: "" };
			if (choice.carrierId == null && shop.carrierId != null) choice.carrierId = shop.carrierId;
			state.shops.set(key, choice);
		}
	}

	function isAddressNotFound(error) {
		const message = errorMessage(error);
		return state.addressId != null && /địa chỉ/i.test(message)
			&& /(không tìm thấy|not found|404)/i.test(message);
	}

	async function loadPreview() {
		const sequence = ++requestSequence;
		if (activeRequest) activeRequest.abort();
		activeRequest = new AbortController();
		setLoading(true);
		removeInlineError();

		try {
			const preview = await requestJson("/api/checkout/preview", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(buildRequest()),
				signal: activeRequest.signal
			});
			if (sequence !== requestSequence) return;
			lastPreview = preview;
			rememberDefaultCarriers(preview);
			render(preview);
			loadCouponSuggestions(sequence, activeRequest.signal);
		} catch (error) {
			if (sequence !== requestSequence || error?.name === "AbortError") return;
			lastPreview = null;
			if (isPlacing) {
				showToast(errorMessage(error), "error");
				showInlineError(errorMessage(error));
				return;
			}
			const placeOrder = root.querySelector("#place-order");
			if (placeOrder) placeOrder.disabled = true;
			if (isAddressNotFound(error)) {
				state.addressId = null;
				await loadPreview();
				return;
			}
			reportApiError(error);
			showInlineError(errorMessage(error));
		} finally {
			if (sequence === requestSequence) {
				activeRequest = null;
				setLoading(false);
			}
		}
	}

	function removeInlineError() {
		root.querySelector("[data-checkout-load-error]")?.remove();
	}

	function showInlineError(message) {
		removeInlineError();
		const alert = element("div", "alert alert-danger d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-2");
		alert.dataset.checkoutLoadError = "true";
		alert.append(element("span", "", message));
		alert.append(button("Thử lại", "btn btn-outline-danger btn-sm align-self-start align-self-sm-auto", loadPreview));
		root.prepend(alert);
	}

	function render(preview) {
		removeInlineError();
		const layout = element("div", "row g-4 align-items-start");
		const shopsColumn = element("div", "col-12 col-lg-8 d-grid gap-3");
		const summaryColumn = element("aside", "col-12 col-lg-4");
		shopsColumn.append(renderAddress(preview.address));
		for (const shop of preview.shops || []) shopsColumn.append(renderShop(shop, preview.availableCarriers || []));
		if (!(preview.shops || []).length) {
			shopsColumn.append(element("div", "alert alert-info", "Chưa có sản phẩm được chọn để thanh toán."));
		}
		summaryColumn.append(renderSummary(preview));
		layout.append(shopsColumn, summaryColumn);
		root.replaceChildren(layout);
	}

	function addAddressLink(parent) {
		const link = element("a", "btn btn-outline-primary btn-sm", "Thêm địa chỉ mới");
		link.href = "/profile/addresses/new?returnUrl=/checkout";
		parent.append(link);
	}

	function renderAddress(address) {
		const card = element("section", "card");
		const header = element("div", "card-header d-flex align-items-center justify-content-between gap-2");
		header.append(element("h2", "h5 mb-0", "Địa chỉ nhận hàng"));
		if (address) header.append(button("Thay đổi", "btn btn-outline-primary btn-sm", openAddressPicker));
		const body = element("div", "card-body");
		if (!address) {
			const alert = element("div", "alert alert-warning mb-0 d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2");
			alert.append(element("span", "", "Bạn chưa có địa chỉ nhận hàng. Hãy thêm địa chỉ để tiếp tục."));
			addAddressLink(alert);
			body.append(alert);
		} else {
			body.append(element("div", "fw-semibold", address.receiverName || ""));
			body.append(element("div", "text-secondary", address.phoneNumber || ""));
			body.append(element("div", "", address.fullAddress || ""));
		}
		card.append(header, body);
		return card;
	}

	function renderShop(shop, carriers) {
		const card = element("section", "card");
		const header = element("div", "card-header");
		header.append(element("h2", "h5 mb-0", shop.shopName || "Chi nhánh"));
		const body = element("div", "card-body d-grid gap-3");
		for (const line of shop.lines || []) body.append(renderLine(line));
		body.append(renderCarrierChoices(shop, carriers));
		body.append(renderShopCoupon(shop));
		body.append(renderShopTotals(shop));
		if ((shop.issues || []).length) body.append(renderIssueList(shop.issues, "warning"));
		card.append(header, body);
		return card;
	}

	function renderLine(line) {
		const row = element("article", "row g-3 align-items-center border-bottom pb-3");
		const imageColumn = element("div", "col-auto");
		const image = document.createElement("img");
		image.className = "rounded border";
		image.width = 72;
		image.height = 72;
		image.loading = "lazy";
		image.alt = line.productName ? `Ảnh ${line.productName}` : "Ảnh sản phẩm";
		const imageUrl = safeImageUrl(line.imageUrl);
		if (imageUrl) image.src = imageUrl;
		else image.classList.add("d-none");
		imageColumn.append(image);

		const detail = element("div", "col");
		detail.append(element("h3", "h6 mb-1", line.productName || "Sản phẩm"));
		if (line.cardCondition) detail.append(element("div", "small text-secondary", `Tình trạng: ${line.cardCondition}`));
		detail.append(element("div", "small", `Đơn giá: ${formatMoney(line.unitPrice)}`));
		detail.append(element("div", "small", `Số lượng: ${Number(line.quantity) || 0}`));
		detail.append(element("div", "fw-semibold", `Thành tiền: ${formatMoney(line.lineTotal)}`));
		if (line.problem) {
			const problem = element("div", "text-danger small mt-2");
			problem.append(element("span", "d-block", line.problem));
			const cartLink = element("a", "link-danger", "Quay lại giỏ hàng để chỉnh sửa");
			cartLink.href = "/cart";
			problem.append(cartLink);
			row.classList.add("border-danger", "bg-danger-subtle", "rounded", "p-2");
			detail.append(problem);
		}
		row.append(imageColumn, detail);
		return row;
	}

	function getShopChoice(shopId) {
		const key = String(shopId);
		if (!state.shops.has(key)) state.shops.set(key, { carrierId: null, couponCode: "" });
		return state.shops.get(key);
	}

	function renderCarrierChoices(shop, carriers) {
		const section = element("fieldset", "border rounded p-3");
		section.append(element("legend", "float-none w-auto px-1 fs-6 fw-semibold", "Đơn vị vận chuyển"));
		if (!carriers.length) {
			section.append(element("div", "small text-warning", "Hiện chưa có đơn vị vận chuyển khả dụng."));
			return section;
		}
		const choice = getShopChoice(shop.shopId);
		for (const carrier of carriers) {
			const label = element("label", "form-check border-bottom py-2");
			const radio = document.createElement("input");
			radio.type = "radio";
			radio.className = "form-check-input";
			radio.name = `carrier-${String(shop.shopId)}`;
			radio.value = String(carrier.id);
			radio.checked = choice.carrierId != null && String(choice.carrierId) === String(carrier.id);
			radio.addEventListener("change", () => {
				choice.carrierId = carrier.id;
				loadPreview();
			});
			const labelText = element("span", "form-check-label ms-2");
			labelText.append(element("span", "d-block fw-semibold", carrier.name || "Đơn vị vận chuyển"));
			labelText.append(element("span", "small text-secondary", `Phí cơ bản: ${formatMoney(carrier.baseFee)}`));
			if (carrier.freeShipThreshold != null) {
				labelText.append(element("span", "small text-success d-block",
					`Miễn phí vận chuyển khi đơn từ ${formatMoney(carrier.freeShipThreshold)}`));
			}
			label.append(radio, labelText);
			section.append(label);
		}
		return section;
	}

	function renderShopCoupon(shop) {
		const section = element("section", "border rounded p-3");
		section.append(element("h3", "h6", "Mã giảm giá Chi nhánh"));
		const choice = getShopChoice(shop.shopId);
		const controls = element("div", "input-group input-group-sm");
		const input = document.createElement("input");
		input.type = "text";
		input.className = "form-control";
		input.maxLength = 50;
		input.autocomplete = "off";
		input.placeholder = "Nhập mã giảm giá";
		input.value = choice.couponCode || "";
		input.setAttribute("aria-label", `Mã giảm giá của ${shop.shopName || "chi nhánh"}`);
		input.addEventListener("keydown", (event) => {
			if (event.key === "Enter") {
				event.preventDefault();
				applyShopCoupon(shop.shopId, input.value);
			}
		});
		controls.append(input, button("Áp dụng", "btn btn-outline-primary", () => applyShopCoupon(shop.shopId, input.value)));
		section.append(controls);
		const suggestions = element("section", "checkout-coupon-suggestions");
		suggestions.dataset.couponShopId = String(shop.shopId);
		suggestions.hidden = true;
		section.append(suggestions);
		const coupon = shop.shopCoupon;
		if (coupon) {
			const result = element("div", `small mt-2 ${coupon.applied ? "text-success" : "text-warning"}`);
			if (coupon.applied) {
				result.append(element("span", "me-2", `Đã áp dụng -${formatMoney(coupon.discountAmount)}`));
				result.append(button("Gỡ mã", "btn btn-link btn-sm p-0 align-baseline", () => applyShopCoupon(shop.shopId, "")));
			} else {
				result.textContent = String(coupon.message || "Mã giảm giá không hợp lệ.");
			}
			section.append(result);
		}
		return section;
	}

	function applyShopCoupon(shopId, code) {
		const choice = getShopChoice(shopId);
		choice.couponCode = String(code || "").trim();
		loadPreview();
	}

	async function loadCouponSuggestions(sequence, signal) {
		try {
			const coupons = await requestJson("/api/checkout/coupons", { signal });
			if (sequence !== requestSequence) return;
			for (const host of root.querySelectorAll("[data-coupon-shop-id]")) {
				const matching = coupons.filter((coupon) => String(coupon.shopId) === host.dataset.couponShopId);
				host.replaceChildren();
				host.hidden = matching.length === 0;
				if (!matching.length) continue;
				host.append(element("h4", "mb-2", "Mã giảm giá có thể áp dụng"));
				const list = element("div", "checkout-coupon-list");
				for (const coupon of matching) {
					const ticket = element("article", "checkout-coupon-ticket");
					const icon = element("i", "bi bi-ticket-perforated");
					icon.setAttribute("aria-hidden", "true");
					const copy = element("div", "checkout-coupon-copy");
					copy.append(element("strong", "checkout-coupon-code", coupon.code));
					const discount = coupon.discountType === "PERCENTAGE"
						? `Giảm ${coupon.discountValue}%` : `Giảm ${formatMoney(coupon.discountValue)}`;
					copy.append(element("span", "d-block small", discount));
					let condition = `Đơn từ ${formatMoney(coupon.minOrderValue)}`;
					if (coupon.maxDiscountAmount != null) condition += ` · Tối đa ${formatMoney(coupon.maxDiscountAmount)}`;
					copy.append(element("span", "checkout-coupon-condition", condition));
					const selected = getShopChoice(coupon.shopId).couponCode.toUpperCase() === coupon.code.toUpperCase();
					const apply = button(selected ? "Đã chọn" : "Áp dụng", "btn btn-sm checkout-coupon-apply", () => {
						const input = host.parentElement.querySelector('input[type="text"]');
						if (input) input.value = coupon.code;
						applyShopCoupon(coupon.shopId, coupon.code);
					});
					apply.disabled = selected;
					apply.setAttribute("aria-label", `${selected ? "Đã chọn" : "Áp dụng"} mã ${coupon.code}`);
					ticket.append(icon, copy, apply);
					list.append(ticket);
				}
				host.append(list);
			}
		} catch (error) {
			if (sequence !== requestSequence || error?.name === "AbortError") return;
			if (window.App?.handleAuthenticationError(error)) return;
			showToast("Chưa tải được mã gợi ý. Bạn vẫn có thể nhập mã thủ công.", "info");
		}
	}

	function renderShopTotals(shop) {
		const totals = element("dl", "row small mb-0 border-top pt-3");
		appendMoneyRow(totals, "Tạm tính", shop.subtotal);
		appendMoneyRow(totals, "Giảm giá Chi nhánh", shop.shopCoupon?.applied ? shop.shopCoupon.discountAmount : 0);
		appendMoneyRow(totals, "Mã hệ thống được chia", shop.platformCouponShare);
		appendMoneyRow(totals, "Phí vận chuyển", shop.shippingFee);
		appendMoneyRow(totals, "Tổng Chi nhánh", shop.totalAmount, true);
		return totals;
	}

	function appendMoneyRow(parent, label, amount, emphasize = false) {
		const term = element("dt", `col-7 ${emphasize ? "fw-bold" : "fw-normal"}`, label);
		const value = element("dd", `col-5 text-end ${emphasize ? "fw-bold" : ""}`, formatMoney(amount));
		parent.append(term, value);
	}

	function renderSummary(preview) {
		const card = element("section", "card position-sticky top-0");
		const body = element("div", "card-body");
		body.append(element("h2", "h5", "Tóm tắt đơn hàng"));
		body.append(renderPlatformCoupon(preview.platformCoupon));
		body.append(renderPaymentMethods());
		body.append(renderNoteField());
		const totals = element("dl", "row small border-top pt-3 mt-3");
		appendMoneyRow(totals, "Tiền hàng", preview.itemsSubtotal);
		appendMoneyRow(totals, "Giảm giá", preview.totalDiscount);
		appendMoneyRow(totals, "Phí vận chuyển", preview.totalShippingFee);
		appendMoneyRow(totals, "TỔNG THANH TOÁN", preview.grandTotal, true);
		const grandLabel = totals.querySelector("dt:last-of-type");
		const grandAmount = totals.querySelector("dd:last-of-type");
		grandLabel?.classList.add("fs-6", "text-primary");
		grandAmount?.classList.add("fs-6", "text-primary");
		body.append(totals);
		if ((preview.issues || []).length) body.append(renderIssueList(preview.issues, "warning"));
		const orderLabel = state.paymentMethod === "MOMO" ? "Thanh toán ngay với MoMo" : "Đặt hàng";
		const orderButton = button(orderLabel, "btn btn-primary w-100 mt-3", placeOrder);
		orderButton.id = "place-order";
		orderButton.disabled = loading || isPlacing || !preview.canPlaceOrder;
		body.append(orderButton);
		card.append(body);
		return card;
	}

	function renderPlatformCoupon(coupon) {
		const section = element("section", "mb-3");
		section.append(element("h3", "h6", "Mã giảm giá hệ thống"));
		const controls = element("div", "input-group input-group-sm");
		const input = document.createElement("input");
		input.type = "text";
		input.className = "form-control";
		input.maxLength = 50;
		input.autocomplete = "off";
		input.placeholder = "Nhập mã giảm giá";
		input.value = state.platformCouponCode || "";
		input.setAttribute("aria-label", "Mã giảm giá hệ thống");
		input.addEventListener("keydown", (event) => {
			if (event.key === "Enter") {
				event.preventDefault();
				applyPlatformCoupon(input.value);
			}
		});
		controls.append(input, button("Áp dụng", "btn btn-outline-primary", () => applyPlatformCoupon(input.value)));
		section.append(controls);
		if (coupon) {
			const result = element("div", `small mt-2 ${coupon.applied ? "text-success" : "text-warning"}`);
			if (coupon.applied) {
				result.append(element("span", "me-2", `Đã áp dụng -${formatMoney(coupon.discountAmount)}`));
				result.append(button("Gỡ mã", "btn btn-link btn-sm p-0 align-baseline", () => applyPlatformCoupon("")));
			} else {
				result.textContent = String(coupon.message || "Mã giảm giá không hợp lệ.");
			}
			section.append(result);
		}
		return section;
	}

	function applyPlatformCoupon(code) {
		state.platformCouponCode = String(code || "").trim();
		loadPreview();
	}

	function renderPaymentMethods() {
		const fieldset = element("fieldset", "border rounded p-3");
		fieldset.append(element("legend", "float-none w-auto px-1 fs-6 fw-semibold", "Phương thức thanh toán"));
		for (const [value, labelText] of [["COD", "Thanh toán khi nhận hàng (COD)"], ["MOMO", "Thanh toán qua ví MoMo"]]) {
			const label = element("label", "form-check mb-2");
			const radio = document.createElement("input");
			radio.type = "radio";
			radio.name = "payment-method";
			radio.value = value;
			radio.className = "form-check-input";
			radio.checked = state.paymentMethod === value;
			radio.addEventListener("change", () => {
				state.paymentMethod = value;
				loadPreview();
			});
			label.append(radio, element("span", "form-check-label ms-2", labelText));
			fieldset.append(label);
		}
		if (state.paymentMethod === "MOMO") {
			fieldset.append(element("p", "small text-secondary mb-0 mt-2",
				"Đây là quy trình MoMo giả lập để phục vụ trình diễn đồ án."));
		}
		return fieldset;
	}

	function openMockMomo(response) {
		const modal = element("div", "modal fade mock-momo-modal");
		modal.tabIndex = -1;
		modal.setAttribute("aria-hidden", "true");
		const dialog = element("div", "modal-dialog modal-dialog-centered");
		const content = element("div", "modal-content");
		const header = element("div", "modal-header border-0 justify-content-center pb-0");
		const logo = element("div", "mock-momo-logo", "MoMo");
		logo.setAttribute("aria-label", "MoMo giả lập");
		header.append(logo);
		const body = element("div", "modal-body text-center px-4 pt-3");
		body.append(element("p", "text-secondary small mb-1", "CỔNG THANH TOÁN GIẢ LẬP"));
		body.append(element("h2", "h5 mb-3", "Thanh toán bằng MoMo"));
		body.append(element("p", "mock-momo-amount", formatMoney(response.grandTotal)));
		const code = element("p", "small text-secondary mb-4", `Mã đơn hàng: ${response.groupCode}`);
		body.append(code);
		const qrSection = element("div", "mock-momo-qr-section");
		const qrCode = element("div", "mock-momo-qr");
		qrCode.id = "mock-momo-qrcode";
		qrCode.setAttribute("role", "img");
		qrCode.setAttribute("aria-label", "Mã QR thanh toán MoMo giả lập");
		qrSection.append(qrCode, element("p", "small text-secondary mt-2 mb-3", "Mở ứng dụng MoMo để quét mã QR"));
		body.append(qrSection);
		const actions = element("div", "d-grid gap-2");
		const confirm = button("Xác nhận thanh toán", "btn mock-momo-confirm", async () => {
			confirm.disabled = true;
			cancel.disabled = true;
			try {
				const result = await requestJson(`/api/checkout/${encodeURIComponent(response.groupCode)}/mock-momo/confirm`, {
					method: "POST"
				});
				window.location.replace(result.redirectUrl);
			} catch (error) {
				reportApiError(error);
				confirm.disabled = false;
				cancel.disabled = false;
			}
		});
		const cancel = button("Hủy", "btn btn-outline-secondary", async () => {
			confirm.disabled = true;
			cancel.disabled = true;
			try {
				const result = await requestJson(`/api/checkout/${encodeURIComponent(response.groupCode)}/mock-momo/cancel`, {
					method: "POST"
				});
				window.location.replace(result.redirectUrl);
			} catch (error) {
				reportApiError(error);
				confirm.disabled = false;
				cancel.disabled = false;
			}
		});
		actions.append(confirm, cancel);
		body.append(actions);
		content.append(header, body);
		dialog.append(content);
		modal.append(dialog);
		modal.addEventListener("hidden.bs.modal", () => modal.remove(), { once: true });
		document.body.append(modal);
		const instance = window.bootstrap?.Modal?.getOrCreateInstance(modal, { backdrop: "static", keyboard: false });
		if (!instance) {
			modal.remove();
			showToast("Không thể mở màn hình thanh toán MoMo giả lập.");
			return;
		}
		if (window.QRCode) {
			new window.QRCode(qrCode, {
				text: `TCGSHOP-MOMO|${response.groupCode}|${response.grandTotal}`,
				width: 180,
				height: 180,
				colorDark: "#171522",
				colorLight: "#ffffff",
				correctLevel: window.QRCode.CorrectLevel.M
			});
		} else {
			qrCode.classList.add("mock-momo-qr--unavailable");
			qrCode.textContent = `Mã thanh toán: ${response.groupCode}`;
		}
		instance.show();
	}

	function renderNoteField() {
		const section = element("section", "mb-3");
		const label = element("label", "form-label fw-semibold", "Ghi chú cho chủ chi nhánh");
		label.htmlFor = "checkout-note-input";
		const input = document.createElement("textarea");
		input.id = "checkout-note-input";
		input.className = "form-control";
		input.rows = 3;
		input.maxLength = 500;
		input.placeholder = "Ví dụ: vui lòng gọi trước khi giao hàng";
		input.value = sellerNote;
		const counter = element("div", "form-text text-end", `${input.value.length}/500`);
		input.addEventListener("input", () => {
			sellerNote = input.value;
			counter.textContent = `${input.value.length}/500`;
		});
		sellerNote = input.value;
		section.append(label, input, counter);
		return section;
	}

	function renderIssueList(issues, type) {
		const alert = element("div", `alert alert-${type} small mt-3 mb-0`);
		const list = element("ul", "mb-0 ps-3");
		for (const issue of issues) list.append(element("li", "", issue));
		alert.append(list);
		return alert;
	}

	async function openAddressPicker() {
		const modal = createAddressModal();
		const modalInstance = window.bootstrap?.Modal?.getOrCreateInstance(modal);
		if (!modalInstance) {
			showToast("Không thể mở danh sách địa chỉ.");
			modal.remove();
			return;
		}
		modalInstance.show();
		const content = modal.querySelector("[data-address-list]");
		content.replaceChildren(element("div", "text-secondary", "Đang tải địa chỉ…"));
		try {
			const addresses = await requestJson("/api/profile/addresses");
			if (!Array.isArray(addresses) || addresses.length === 0) {
				const empty = element("div", "alert alert-warning mb-0");
				empty.append(element("p", "mb-2", "Bạn chưa lưu địa chỉ nào."));
				addAddressLink(empty);
				content.replaceChildren(empty);
				return;
			}
			renderAddressOptions(content, addresses, modal, modalInstance);
		} catch (error) {
			reportApiError(error);
			content.replaceChildren(element("div", "alert alert-danger mb-0", errorMessage(error)));
		}
	}

	function createAddressModal() {
		const modal = element("div", "modal fade");
		modal.tabIndex = -1;
		modal.setAttribute("aria-hidden", "true");
		const dialog = element("div", "modal-dialog modal-dialog-centered modal-dialog-scrollable");
		const content = element("div", "modal-content");
		const header = element("div", "modal-header");
		header.append(element("h2", "modal-title fs-5", "Chọn địa chỉ nhận hàng"));
		const close = button("", "btn-close", () => window.bootstrap?.Modal.getOrCreateInstance(modal).hide());
		close.setAttribute("aria-label", "Đóng");
		header.append(close);
		const body = element("div", "modal-body");
		body.dataset.addressList = "true";
		const footer = element("div", "modal-footer");
		const addLink = element("a", "btn btn-outline-secondary btn-sm", "Thêm địa chỉ mới");
		addLink.href = "/profile/addresses/new?returnUrl=/checkout";
		footer.append(addLink, button("Đóng", "btn btn-secondary", () => window.bootstrap?.Modal.getOrCreateInstance(modal).hide()));
		content.append(header, body, footer);
		dialog.append(content);
		modal.append(dialog);
		modal.addEventListener("hidden.bs.modal", () => modal.remove(), { once: true });
		document.body.append(modal);
		return modal;
	}

	function renderAddressOptions(container, addresses, modal, modalInstance) {
		let selectedAddressId = state.addressId;
		const group = element("div", "list-group");
		for (const address of addresses) {
			const label = element("label", "list-group-item d-flex gap-2 align-items-start");
			const radio = document.createElement("input");
			radio.type = "radio";
			radio.name = "checkout-address";
			radio.className = "form-check-input mt-1";
			radio.value = String(address.id);
			radio.checked = state.addressId == null ? Boolean(address.isDefault) : String(state.addressId) === String(address.id);
			radio.addEventListener("change", () => { selectedAddressId = address.id; });
			const details = element("span", "flex-grow-1");
			details.append(element("span", "d-block fw-semibold", address.receiverName || ""));
			details.append(element("span", "d-block small text-secondary", address.phoneNumber || ""));
			details.append(element("span", "d-block", address.fullAddress || ""));
			if (address.isDefault) details.append(element("span", "badge text-bg-primary mt-1", "Mặc định"));
			label.append(radio, details);
			group.append(label);
		}
		const useAddress = button("Dùng địa chỉ này", "btn btn-primary w-100 mt-3", () => {
			if (selectedAddressId == null) return;
			state.addressId = selectedAddressId;
			modalInstance.hide();
			loadPreview();
		});
		container.replaceChildren(group, useAddress);
	}

	async function placeOrder() {
		const orderButton = root.querySelector("#place-order");
		if (!orderButton || orderButton.disabled || isPlacing || !lastPreview?.canPlaceOrder) return;
		const token = idempotencyTokenField?.value || "";
		if (!token || !lastPreview) return;
		isPlacing = true;
		orderButton.disabled = true;
		orderButton.replaceChildren(element("span", "spinner-border spinner-border-sm me-2", ""),
			element("span", "", "Đang đặt hàng..."));
		const request = {
			...buildRequest(),
			idempotencyToken: token,
			expectedGrandTotal: lastPreview.grandTotal,
			note: sellerNote
		};
		try {
			const response = await requestJson("/api/checkout/place", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(request)
			});
			if (response?.redirectUrl) {
				if (response.paymentMethod === "MOMO") openMockMomo(response);
				else window.location.replace(response.redirectUrl);
				return;
			}
			showToast("Kết nối bị gián đoạn. Bạn có thể bấm Đặt hàng lại, đơn sẽ không bị tạo trùng", "error");
		} catch (error) {
			const status = apiStatus(error);
			if (window.App?.handleAuthenticationError(error)) return;
			if (status === 409 || status === 422) {
				showToast(errorMessage(error), "error");
				try {
					await loadPreview();
				} catch {
					// loadPreview tự thông báo lỗi và khóa đặt hàng nếu không lấy được số liệu mới.
				}
			} else if (status === 400) {
				showToast(errorMessage(error), "error");
			} else if (!status) {
				// Giữ nguyên token sau lỗi mạng vì máy chủ dùng token này để trả lại kết quả cũ, tránh tạo đơn trùng.
				showToast("Kết nối bị gián đoạn. Bạn có thể bấm Đặt hàng lại, đơn sẽ không bị tạo trùng", "error");
			} else {
				showToast(errorMessage(error), "error");
			}
		} finally {
			isPlacing = false;
			const currentButton = root.querySelector("#place-order");
			if (currentButton) currentButton.disabled = loading || !lastPreview?.canPlaceOrder;
			if (currentButton && currentButton.textContent.includes("Đang đặt hàng")) {
				currentButton.textContent = state.paymentMethod === "MOMO" ? "Thanh toán ngay với MoMo" : "Đặt hàng";
			}
		}
	}

	window.CheckoutPage = window.CheckoutPage || {};
	window.CheckoutPage.buildRequest = buildRequest;
	window.CheckoutPage.idempotencyToken = idempotencyTokenField?.value || "";

	renderInitialLoading();
	loadPreview();
})();
