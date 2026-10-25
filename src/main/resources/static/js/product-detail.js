(function () {
	'use strict';

	const root = document.getElementById('product-detail');
	if (!root) return;

	const $ = (id) => document.getElementById(id);
	const offers = Array.isArray(window.PRODUCT_OFFERS) ? window.PRODUCT_OFFERS : [];
	const singleCard = !!window.IS_SINGLE_CARD;

	let currentId = window.CURRENT_PRODUCT_ID;
	let currentStock = Number(window.CURRENT_STOCK || 0);

	const money = (n) => new Intl.NumberFormat('vi-VN').format(Number(n || 0)) + ' ₫';

	function showMessage(text, type) {
		const box = $('pd-message');
		box.textContent = text || '';
		box.className = 'alert alert-' + (type || 'info') + (text ? '' : ' d-none');
	}

	function distinct(values) {
		return Array.from(new Set(values.filter(Boolean)));
	}

	function fillSelect(select, values, keepFirst) {
		const first = keepFirst ? select.options[0] : null;
		select.innerHTML = '';
		if (first) select.appendChild(first);
		values.forEach((v) => {
			const option = document.createElement('option');
			option.value = v;
			option.textContent = v;
			select.appendChild(option);
		});
	}

	function applyStock(stock) {
		currentStock = stock;
		$('pd-stock').textContent = stock;
		const out = stock <= 0;
		$('pd-add').disabled = out;
		$('pd-soldout').classList.toggle('d-none', !out);
		const qty = $('pd-qty');
		qty.max = Math.max(stock, 1);
		if (Number(qty.value) > stock && stock > 0) qty.value = stock;
	}

	function selectOffer(offer) {
		currentId = offer.productId;
		$('pd-price').textContent = money(offer.effectivePrice);
		const old = $('pd-old-price');
		if (old) old.classList.add('d-none');
		applyStock(offer.stock);
	}

	function filteredOffers() {
		const condition = $('pd-condition').value;
		const language = $('pd-language').value;
		return offers.filter((o) => (!condition || o.condition === condition) && (!language || o.language === language));
	}

	function renderOfferSelect() {
		const select = $('pd-offer');
		const list = filteredOffers();
		select.innerHTML = '';
		list.forEach((o) => {
			const option = document.createElement('option');
			option.value = o.productId;
			option.textContent = o.shopName + ' - ' + (o.condition || '') + ' ' + (o.language || '')
				+ ' - ' + money(o.effectivePrice) + (o.stock > 0 ? ' (còn ' + o.stock + ')' : ' (hết hàng)');
			if (o.productId === currentId) option.selected = true;
			select.appendChild(option);
		});
		if (list.length) {
			const chosen = list.find((o) => o.productId === Number(select.value)) || list[0];
			selectOffer(chosen);
		} else {
			applyStock(0);
		}
	}

	async function addToCart() {
		const quantity = Number($('pd-qty').value);
		if (!Number.isInteger(quantity) || quantity < 1) {
			showMessage('Số lượng không hợp lệ', 'danger');
			return;
		}
		if (quantity > currentStock) {
			showMessage('Số lượng vượt quá tồn kho', 'danger');
			return;
		}

		const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
		const token = document.querySelector('meta[name="_csrf"]');
		const header = document.querySelector('meta[name="_csrf_header"]');
		if (token && header) headers[header.content] = token.content;

		const button = $('pd-add');
		button.disabled = true;
		try {
			const response = await fetch('/api/cart/items', {
				method: 'POST',
				headers: headers,
				credentials: 'same-origin',
				body: JSON.stringify({ productId: currentId, quantity: quantity })
			});
			if (response.status === 401) {
				window.location.href = '/auth/login?returnUrl=' + encodeURIComponent(window.location.pathname);
				return;
			}
			const body = await response.json().catch(() => ({}));
			if (!response.ok) {
				showMessage(body.message || 'Không thêm được vào giỏ hàng', 'danger');
				return;
			}
			showMessage(body.message || 'Đã thêm vào giỏ hàng', 'success');
			const badge = document.getElementById('cart-count');
			if (badge && body.cartCount !== undefined) badge.textContent = body.cartCount;
		} catch (e) {
			showMessage('Lỗi kết nối, vui lòng thử lại', 'danger');
		} finally {
			button.disabled = currentStock <= 0;
		}
	}

	if (singleCard) {
		fillSelect($('pd-condition'), distinct(offers.map((o) => o.condition)), true);
		fillSelect($('pd-language'), distinct(offers.map((o) => o.language)), true);
		$('pd-condition').addEventListener('change', renderOfferSelect);
		$('pd-language').addEventListener('change', renderOfferSelect);
		$('pd-offer').addEventListener('change', function () {
			const chosen = offers.find((o) => o.productId === Number(this.value));
			if (chosen) selectOffer(chosen);
		});
		renderOfferSelect();
	} else {
		applyStock(currentStock);
	}

	$('pd-add').addEventListener('click', addToCart);
})();