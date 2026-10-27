(function () {
	'use strict';

	const section = document.getElementById('vendor-offers-section');
	if (!section) return;

	const cardId = section.dataset.cardId;
	const SORTS = [
		{ value: 'price:asc', label: 'Giá thấp đến cao' },
		{ value: 'price:desc', label: 'Giá cao đến thấp' },
		{ value: 'rating:desc', label: 'Đánh giá shop cao nhất' },
		{ value: 'condition:asc', label: 'Tình trạng tốt nhất' }
	];

	const money = (n) => new Intl.NumberFormat('vi-VN').format(Number(n || 0)) + ' ₫';
	let requestSeq = 0;
	let conditionsBuilt = false;

	function el(tag, className, text) {
		const node = document.createElement(tag);
		if (className) node.className = className;
		if (text !== undefined) node.textContent = text;
		return node;
	}

	// Khung giao diện dựng một lần bằng DOM API (không dùng innerHTML với dữ liệu động).
	const title = el('h2', 'h5 mb-3', 'So sánh giá giữa các người bán');
	const controls = el('div', 'row g-2 mb-3');

	const conditionCol = el('div', 'col-sm-4');
	conditionCol.appendChild(el('label', 'form-label', 'Tình trạng'));
	const conditionSelect = el('select', 'form-select');
	conditionSelect.id = 'vo-condition';
	const allOption = el('option', '', 'Tất cả');
	allOption.value = '';
	conditionSelect.appendChild(allOption);
	conditionCol.appendChild(conditionSelect);

	const sortCol = el('div', 'col-sm-4');
	sortCol.appendChild(el('label', 'form-label', 'Sắp xếp'));
	const sortSelect = el('select', 'form-select');
	sortSelect.id = 'vo-sort';
	SORTS.forEach((s) => {
		const option = el('option', '', s.label);
		option.value = s.value;
		sortSelect.appendChild(option);
	});
	sortCol.appendChild(sortSelect);

	controls.appendChild(conditionCol);
	controls.appendChild(sortCol);

	const message = el('div', 'alert d-none');
	message.setAttribute('role', 'alert');
	const wrap = el('div', 'table-responsive');
	const table = el('table', 'table table-hover align-middle');
	const thead = el('thead');
	const headRow = el('tr');
	['Người bán', 'Đánh giá', 'Tình trạng', 'Ngôn ngữ', 'Giá', 'Tồn kho', ''].forEach((h) => headRow.appendChild(el('th', '', h)));
	thead.appendChild(headRow);
	const tbody = el('tbody');
	table.appendChild(thead);
	table.appendChild(tbody);
	wrap.appendChild(table);

	section.appendChild(title);
	section.appendChild(controls);
	section.appendChild(message);
	section.appendChild(wrap);

	function showMessage(text, type) {
		message.textContent = text || '';
		message.className = 'alert alert-' + (type || 'info') + (text ? '' : ' d-none');
	}

	function fillRow(tr, text, colSpan) {
		const td = el('td', 'text-center text-muted', text);
		td.colSpan = colSpan;
		tr.appendChild(td);
	}

	function buildConditions(values) {
		if (conditionsBuilt) return;
		values.forEach((c) => {
			const option = el('option', '', c);
			option.value = c;
			conditionSelect.appendChild(option);
		});
		conditionsBuilt = true;
	}

	async function addToCart(offer, quantity, button) {
		if (!Number.isInteger(quantity) || quantity < 1 || quantity > offer.stock) {
			showMessage('Số lượng không hợp lệ (tối đa ' + offer.stock + ')', 'danger');
			return;
		}
		button.disabled = true;
		try {
			if (!window.App?.fetchWithCsrf) {
				throw new Error('Không thể gửi yêu cầu an toàn. Vui lòng tải lại trang.');
			}
			const body = await window.App.fetchWithCsrf('/api/cart/items', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
				body: JSON.stringify({ productId: offer.productId, quantity: quantity }),
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
			button.disabled = false;
		}
	}

	function renderRows(offers) {
		tbody.replaceChildren();
		if (!offers.length) {
			const tr = el('tr');
			fillRow(tr, 'Không có người bán phù hợp', 7);
			tbody.appendChild(tr);
			return;
		}
		offers.forEach((offer) => {
			const tr = el('tr');
			const shopLink = el('a', '', offer.shopName);
			shopLink.href = '/shops/' + offer.shopId;
			const shopCell = el('td');
			shopCell.appendChild(shopLink);

			const rating = offer.shopRating === null || offer.shopRating === undefined
					|| !(Number(offer.shopRatingCount) > 0) || !Number.isFinite(Number(offer.shopRating))
				? 'Chưa có'
				: Number(offer.shopRating).toFixed(2) + '/5 (' + offer.shopRatingCount + ' đánh giá)';

			const priceCell = el('td', 'fw-bold text-danger', money(offer.effectivePrice));
			if (Number(offer.effectivePrice) < Number(offer.price)) {
				const old = el('div', 'small text-muted fw-normal');
				const strike = el('s', '', money(offer.price));
				old.appendChild(strike);
				priceCell.appendChild(old);
			}

			const qty = el('input', 'form-control form-control-sm');
			qty.type = 'number';
			qty.min = '1';
			qty.max = String(offer.stock);
			qty.value = '1';
			qty.style.width = '70px';
			qty.setAttribute('aria-label', 'Số lượng');
			const button = el('button', 'btn btn-sm btn-primary', 'Thêm giỏ');
			button.type = 'button';
			button.addEventListener('click', () => addToCart(offer, Number(qty.value), button));
			const actions = el('td');
			const group = el('div', 'd-flex gap-2');
			group.appendChild(qty);
			group.appendChild(button);
			actions.appendChild(group);

			tr.appendChild(shopCell);
			tr.appendChild(el('td', '', rating));
			tr.appendChild(el('td', '', offer.condition || '-'));
			tr.appendChild(el('td', '', offer.language || '-'));
			tr.appendChild(priceCell);
			tr.appendChild(el('td', '', String(offer.stock)));
			tr.appendChild(actions);
			tbody.appendChild(tr);
		});
	}

	async function load() {
		const seq = ++requestSeq;
		const parts = sortSelect.value.split(':');
		const params = new URLSearchParams({ sort: parts[0], dir: parts[1] });
		if (conditionSelect.value) params.set('condition', conditionSelect.value);

		showMessage('');
		tbody.replaceChildren();
		const loadingRow = el('tr');
		fillRow(loadingRow, 'Đang tải...', 7);
		tbody.appendChild(loadingRow);

		try {
			const response = await fetch('/api/cards/' + encodeURIComponent(cardId) + '/offers?' + params.toString(), {
				headers: { Accept: 'application/json' },
				credentials: 'same-origin'
			});
			if (seq !== requestSeq) return; // bỏ kết quả của request cũ
			if (!response.ok) {
				const body = await response.json().catch(() => ({}));
				throw new Error(body.message || 'Không tải được danh sách người bán');
			}
			const data = await response.json();
			buildConditions(data.availableConditions || []);
			renderRows(data.offers || []);
		} catch (error) {
			if (seq !== requestSeq) return;
			tbody.replaceChildren();
			showMessage(error.message, 'danger');
		}
	}

	conditionSelect.addEventListener('change', load);
	sortSelect.addEventListener('change', load);
	load();
})();
