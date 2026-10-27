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
		updateSeller(offer);
		$('pd-price').textContent = money(offer.effectivePrice);
		const old = $('pd-old-price');
		if (old) old.classList.add('d-none');
		applyStock(offer.stock);
	}

	function updateSeller(offer) {
		const link = $('pd-shop-link');
		if (!link || !offer) return;
		link.href = '/shops/' + encodeURIComponent(offer.shopId);
		$('pd-shop-name').textContent = offer.shopName || 'Shop';
		const logo = $('pd-shop-logo');
		const avatar = $('pd-shop-avatar');
		if (offer.shopLogoUrl) {
			if (logo) {
				logo.src = offer.shopLogoUrl;
				logo.alt = (offer.shopName || 'Shop') + ' logo';
				logo.classList.remove('d-none');
			} else {
				const image = document.createElement('img');
				image.id = 'pd-shop-logo';
				image.className = 'product-shop-logo';
				image.src = offer.shopLogoUrl;
				image.alt = (offer.shopName || 'Shop') + ' logo';
				avatar?.replaceWith(image);
			}
			avatar?.classList.add('d-none');
		} else {
			logo?.classList.add('d-none');
			if (avatar) {
				avatar.textContent = (offer.shopName || '?').trim().charAt(0).toLocaleUpperCase('vi');
				avatar.classList.remove('d-none');
			} else {
				const initial = document.createElement('span');
				initial.id = 'pd-shop-avatar';
				initial.className = 'product-shop-avatar';
				initial.textContent = (offer.shopName || '?').trim().charAt(0).toLocaleUpperCase('vi');
				logo?.replaceWith(initial);
			}
		}
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

		const button = $('pd-add');
		button.disabled = true;
		try {
			if (!window.App?.fetchWithCsrf) {
				throw new Error('Không thể gửi yêu cầu an toàn. Vui lòng tải lại trang.');
			}
			const body = await window.App.fetchWithCsrf('/api/cart/items', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
				body: JSON.stringify({ productId: currentId, quantity: quantity }),
				showErrorToast: false
			});
			showMessage(body.message || 'Đã thêm vào giỏ hàng', 'success');
			const badge = document.getElementById('cart-count');
			if (badge && body.cartCount !== undefined) badge.textContent = body.cartCount;
		} catch (error) {
			if (window.App?.handleAuthenticationError(error)) return;
			if (error?.status === 403) {
				showMessage('', 'danger');
				window.App?.showToast(error.message, 'error');
				return;
			}
			showMessage(error?.message || 'Lỗi kết nối, vui lòng thử lại', 'danger');
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
