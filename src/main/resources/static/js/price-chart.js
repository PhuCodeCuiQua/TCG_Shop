(function () {
	'use strict';

	const section = document.getElementById('price-chart-section');
	if (!section) return;

	const cardId = section.dataset.cardId;
	const RANGES = [7, 30, 90];
	const money = (n) => new Intl.NumberFormat('vi-VN').format(Number(n || 0)) + ' ₫';
	const shortDate = (iso) => iso.slice(8, 10) + '/' + iso.slice(5, 7);

	let chart = null;
	let requestSeq = 0;
	let currentRange = 30;
	let currentResponse = null;
	let latestLiveEvent = null;
	const liveEvents = [];
	let stompClient = null;
	let priceSubscription = null;
	let reconnectTimer = null;
	let leavingPage = false;
	let toastTimer = null;

	function el(tag, className, text) {
		const node = document.createElement(tag);
		if (className) node.className = className;
		if (text !== undefined) node.textContent = text;
		return node;
	}

	// Khung giao diện dựng một lần bằng DOM API (không dùng innerHTML với dữ liệu động).
	const header = el('div', 'd-flex flex-wrap justify-content-between align-items-center gap-2 mb-2');
	header.appendChild(el('h2', 'h5 mb-0', 'Biểu đồ giá thị trường'));
	const group = el('div', 'btn-group btn-group-sm');
	group.setAttribute('role', 'group');
	group.setAttribute('aria-label', 'Chọn khoảng ngày');
	const buttons = {};
	RANGES.forEach((r) => {
		const button = el('button', 'btn btn-outline-primary', r + ' ngày');
		button.type = 'button';
		button.addEventListener('click', () => {
			currentRange = r;
			load();
		});
		buttons[r] = button;
		group.appendChild(button);
	});
	header.appendChild(group);

	const summary = el('div', 'mb-2');
	const currentPrice = el('span', 'h4 me-2 mb-0');
	const change = el('span', 'fw-semibold');
	const changeNote = el('span', 'text-muted small ms-1');
	summary.appendChild(currentPrice);
	summary.appendChild(change);
	summary.appendChild(changeNote);

	const message = el('div', 'alert d-none');
	message.setAttribute('role', 'alert');
	const empty = el('div', 'text-center text-muted py-5 d-none',
		section.dataset.emptyMessage || 'Chưa có dữ liệu giá cho sản phẩm này.');
	const canvasWrap = el('div', '');
	canvasWrap.style.position = 'relative';
	canvasWrap.style.height = window.innerWidth < 576 ? '220px' : '300px';
	const canvas = el('canvas');
	canvasWrap.appendChild(canvas);
	const liveToast = el('div');
	liveToast.setAttribute('role', 'status');
	liveToast.setAttribute('aria-live', 'polite');
	Object.assign(liveToast.style, {
		position: 'fixed',
		top: '1rem',
		right: '1rem',
		zIndex: '1080',
		maxWidth: 'min(24rem, calc(100vw - 2rem))',
		padding: '0.875rem 1rem',
		borderRadius: '0.5rem',
		background: '#198754',
		color: '#fff',
		boxShadow: '0 0.5rem 1.5rem rgba(0, 0, 0, 0.2)',
		opacity: '0',
		pointerEvents: 'none',
		transition: 'opacity 150ms ease'
	});

	section.replaceChildren();
	section.appendChild(header);
	section.appendChild(summary);
	section.appendChild(message);
	section.appendChild(empty);
	section.appendChild(canvasWrap);
	section.appendChild(liveToast);

	function showMessage(text, type) {
		message.textContent = text || '';
		message.className = 'alert alert-' + (type || 'info') + (text ? '' : ' d-none');
	}

	function markActive() {
		RANGES.forEach((r) => buttons[r].classList.toggle('active', r === currentRange));
	}

	function setBusy(busy) {
		RANGES.forEach((r) => { buttons[r].disabled = busy; });
	}

	function showEmpty(isEmpty) {
		empty.classList.toggle('d-none', !isEmpty);
		canvasWrap.classList.toggle('d-none', isEmpty);
		summary.classList.toggle('d-none', isEmpty);
	}

	function renderSummary(data) {
		currentPrice.textContent = displayMoney(data.currentPrice);
		change.className = 'fw-semibold';
		changeNote.textContent = '';
		if (data.changePercent === null || data.changePercent === undefined) {
			change.textContent = '';
			return;
		}
		const value = Number(data.changePercent);
		const sign = value > 0 ? '+' : '';
		change.textContent = (value > 0 ? '▲ ' : value < 0 ? '▼ ' : '') + sign + value.toFixed(2) + '%';
		change.classList.add(value > 0 ? 'text-success' : value < 0 ? 'text-danger' : 'text-muted');
		changeNote.textContent = data.comparedToPreviousPeriod
			? 'so với ' + data.range + ' ngày trước'
			: 'so với đầu kỳ';
	}

	function renderChart(data) {
		if (typeof Chart === 'undefined') {
			showMessage('Không tải được thư viện biểu đồ', 'danger');
			return;
		}
		if (chart) chart.destroy();
		const labels = (data.labels || data.points.map((point) => point.date)).map(shortDate);
		const marketPrices = data.data || data.points.map((point) => point.marketPrice);
		const averagePrices = data.averageData || data.points.map((point) => point.avgPrice);
		chart = new Chart(canvas, {
			type: 'line',
			data: {
				labels: labels,
				datasets: [
					{
						label: 'Giá thị trường',
						data: marketPrices,
						borderColor: '#0d6efd',
						backgroundColor: 'rgba(13,110,253,.12)',
						fill: true,
						tension: 0.25,
						pointRadius: data.points.length > 40 ? 0 : 3,
						spanGaps: true
					},
					{
						label: 'Giá trung bình',
						data: averagePrices,
						borderColor: '#fd7e14',
						borderDash: [6, 4],
						fill: false,
						tension: 0.25,
						pointRadius: 0,
						spanGaps: true
					}
				]
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				interaction: { mode: 'index', intersect: false },
				plugins: {
					legend: { position: 'bottom' },
					tooltip: {
						callbacks: {
							title: (items) => currentResponse.points[items[0].dataIndex].date,
							label: (c) => c.dataset.label + ': ' + money(c.parsed.y)
						}
					}
				},
				scales: {
					x: { ticks: { autoSkip: true, maxTicksLimit: 8, maxRotation: 0 } },
					y: { ticks: { callback: (v) => new Intl.NumberFormat('vi-VN').format(v) } }
				}
			}
		});
	}

	function displayMoney(value) {
		return value === null || value === undefined ? 'Chưa có' : money(value);
	}

	function updateMarketPrice(value) {
		const marketPrice = document.getElementById('pd-market-price');
		if (marketPrice) marketPrice.textContent = displayMoney(value);
		currentPrice.textContent = displayMoney(value);
	}

	function showLiveToast(event) {
		const oldText = displayMoney(event.oldPrice);
		const newText = displayMoney(event.newPrice);
		const percent = event.changePercent === null || event.changePercent === undefined
			? 'không xác định'
			: (Number(event.changePercent) > 0 ? '+' : '') + Number(event.changePercent).toFixed(2) + '%';
		let updatedText = event.updatedAt || '';
		if (updatedText) {
			const updatedAt = new Date(updatedText);
			if (!Number.isNaN(updatedAt.getTime())) {
				updatedText = new Intl.DateTimeFormat('vi-VN', {
					dateStyle: 'short',
					timeStyle: 'medium'
				}).format(updatedAt);
			}
		}
		liveToast.textContent = 'Giá thẻ vừa thay đổi: ' + oldText + ' → ' + newText
			+ ' (' + percent + ')' + (updatedText ? ' lúc ' + updatedText : '');
		liveToast.style.opacity = '1';
		window.clearTimeout(toastTimer);
		toastTimer = window.setTimeout(() => { liveToast.style.opacity = '0'; }, 6000);
	}

	function mergeLiveEvents(data) {
		const points = data.points.map((point) => ({ ...point }));
		liveEvents.forEach((event) => {
			if (!isInResponseRange(event.recordDate, data)) return;
			const existingIndex = points.findIndex((point) => point.date === event.recordDate);
			const livePoint = {
				date: event.recordDate,
				marketPrice: event.newPrice,
				avgPrice: existingIndex >= 0 ? points[existingIndex].avgPrice : null
			};
			if (existingIndex >= 0) points[existingIndex] = livePoint;
			else points.push(livePoint);
		});
		points.sort((left, right) => left.date.localeCompare(right.date));
		return {
			...data,
			points: points,
			labels: points.map((point) => point.date),
			data: points.map((point) => point.marketPrice),
			averageData: points.map((point) => point.avgPrice),
			currentPrice: latestLiveEvent ? latestLiveEvent.newPrice : data.currentPrice
		};
	}

	function isInResponseRange(date, data) {
		return !!date && (!data.from || date >= data.from) && (!data.to || date <= data.to);
	}

	function applyLiveEventsToChart() {
		if (!currentResponse) return;
		currentResponse = mergeLiveEvents(currentResponse);
		if (!currentResponse.points.length) {
			showEmpty(true);
			return;
		}
		showEmpty(false);
		renderSummary(currentResponse);
		if (chart) {
			chart.data.labels = currentResponse.labels.map(shortDate);
			chart.data.datasets[0].data = currentResponse.data;
			chart.data.datasets[1].data = currentResponse.averageData;
			chart.update('none');
		} else {
			renderChart(currentResponse);
		}
	}

	function handlePriceUpdate(event) {
		if (!event || Number(event.cardId) !== Number(cardId)
				|| typeof event.recordDate !== 'string') {
			showMessage('Không thể đọc dữ liệu biến động giá.', 'warning');
			return;
		}
		latestLiveEvent = event;
		const existingIndex = liveEvents.findIndex((item) => item.recordDate === event.recordDate);
		if (existingIndex >= 0) liveEvents[existingIndex] = event;
		else liveEvents.push(event);
		if (liveEvents.length > 100) liveEvents.shift();

		updateMarketPrice(event.newPrice);
		showLiveToast(event);
		applyLiveEventsToChart();
	}

	function connectToPriceUpdates() {
		if (leavingPage) return;
		if (!window.SockJS || !window.Stomp) {
			showMessage('Không tải được thư viện cập nhật giá trực tiếp.', 'warning');
			return;
		}
		stompClient = window.Stomp.over(new window.SockJS('/ws'));
		stompClient.debug = null;
		stompClient.connect({}, () => {
			if (leavingPage) {
				stompClient.disconnect();
				return;
			}
			priceSubscription = stompClient.subscribe('/topic/price-update/' + encodeURIComponent(cardId), (frame) => {
				try {
					handlePriceUpdate(JSON.parse(frame.body));
				} catch (error) {
					showMessage('Không thể xử lý cập nhật giá trực tiếp.', 'warning');
					console.error('Failed to process the price update message.', error);
				}
			});
		}, () => {
			if (!leavingPage) reconnectTimer = window.setTimeout(connectToPriceUpdates, 5000);
		});
	}

	function disconnectFromPriceUpdates() {
		leavingPage = true;
		window.clearTimeout(reconnectTimer);
		if (priceSubscription) {
			priceSubscription.unsubscribe();
			priceSubscription = null;
		}
		if (stompClient && stompClient.connected) stompClient.disconnect();
	}

	async function load() {
		const seq = ++requestSeq;
		markActive();
		setBusy(true);
		showMessage('');
		try {
			const response = await fetch('/api/cards/' + encodeURIComponent(cardId)
				+ '/price-history?range=' + currentRange, {
				headers: { Accept: 'application/json' },
				credentials: 'same-origin'
			});
			if (seq !== requestSeq) return; // bỏ kết quả của request cũ
			if (!response.ok) {
				const body = await response.json().catch(() => ({}));
				throw new Error(body.message || 'Không tải được dữ liệu giá');
			}
			currentResponse = await response.json();
			const data = mergeLiveEvents(currentResponse);
			currentResponse = data;
			if (!data.points || !data.points.length) {
				if (chart) { chart.destroy(); chart = null; }
				showEmpty(true);
				return;
			}
			showEmpty(false);
			renderSummary(data);
			renderChart(data);
		} catch (error) {
			if (seq !== requestSeq) return;
			showMessage(error.message, 'danger');
		} finally {
			if (seq === requestSeq) setBusy(false);
		}
	}

	load();
	connectToPriceUpdates();
	window.addEventListener('pagehide', disconnectFromPriceUpdates);
	window.addEventListener('pageshow', (event) => {
		if (event.persisted) {
			leavingPage = false;
			connectToPriceUpdates();
		}
	});
})();
