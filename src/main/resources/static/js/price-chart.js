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
	const empty = el('div', 'text-center text-muted py-5 d-none', 'Chưa có dữ liệu giá trong khoảng thời gian này');
	const canvasWrap = el('div', '');
	canvasWrap.style.position = 'relative';
	canvasWrap.style.height = window.innerWidth < 576 ? '220px' : '300px';
	const canvas = el('canvas');
	canvasWrap.appendChild(canvas);

	section.appendChild(header);
	section.appendChild(summary);
	section.appendChild(message);
	section.appendChild(empty);
	section.appendChild(canvasWrap);

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
		currentPrice.textContent = money(data.currentPrice);
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
		const labels = data.points.map((p) => shortDate(p.date));
		chart = new Chart(canvas, {
			type: 'line',
			data: {
				labels: labels,
				datasets: [
					{
						label: 'Giá thị trường',
						data: data.points.map((p) => p.marketPrice),
						borderColor: '#0d6efd',
						backgroundColor: 'rgba(13,110,253,.12)',
						fill: true,
						tension: 0.25,
						pointRadius: data.points.length > 40 ? 0 : 3,
						spanGaps: true
					},
					{
						label: 'Giá trung bình',
						data: data.points.map((p) => p.avgPrice),
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
							title: (items) => data.points[items[0].dataIndex].date,
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
			const data = await response.json();
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
})();