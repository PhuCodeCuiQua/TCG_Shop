(function () {
	'use strict';

	const root = document.getElementById('dashboard');
	if (!root) return;

	const base = root.dataset.apiBase;
	const charts = {};
	const $ = (id) => document.getElementById(id);
	const money = (n) => new Intl.NumberFormat('vi-VN').format(Number(n || 0)) + ' ₫';
	const number = (n) => new Intl.NumberFormat('vi-VN').format(Number(n || 0));

	function formatDate(d) {
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const day = String(d.getDate()).padStart(2, '0');
		return d.getFullYear() + '-' + m + '-' + day;
	}

	function showError(message) {
		const box = $('dash-error');
		box.textContent = message || '';
		box.classList.toggle('d-none', !message);
	}

	async function getJson(path, params) {
		const response = await fetch(base + path + '?' + params.toString(), {
			headers: { Accept: 'application/json' },
			credentials: 'same-origin'
		});
		if (!response.ok) {
			let message = 'Không tải được dữ liệu thống kê';
			try {
				const body = await response.json();
				if (body && body.message) message = body.message;
			} catch (e) { /* giữ thông báo mặc định */ }
			throw new Error(message);
		}
		return response.json();
	}

	function draw(key, canvasId, config) {
		if (charts[key]) charts[key].destroy();
		charts[key] = new Chart($(canvasId), config);
	}

	function renderSummary(s) {
		$('card-users').textContent = number(s.totalUsers);
		$('card-shops').textContent = number(s.totalShops);
		$('card-orders').textContent = number(s.totalOrders);
		$('card-revenue').textContent = money(s.revenue);
		const hasCommission = s.commission !== null && s.commission !== undefined;
		$('card-commission-wrap').classList.toggle('d-none', !hasCommission);
		if (hasCommission) $('card-commission').textContent = money(s.commission);
	}

	function renderRevenue(points) {
		const datasets = [{
			label: 'Doanh thu',
			data: points.map((p) => p.revenue),
			borderColor: '#0d6efd',
			backgroundColor: 'rgba(13,110,253,.15)',
			fill: true,
			tension: 0.25
		}];
		if (points.length && points[0].commission !== null && points[0].commission !== undefined) {
			datasets.push({
				label: 'Hoa hồng',
				data: points.map((p) => p.commission),
				borderColor: '#198754',
				backgroundColor: 'rgba(25,135,84,.15)',
				tension: 0.25
			});
		}
		draw('revenue', 'chart-revenue', {
			type: 'line',
			data: { labels: points.map((p) => p.label), datasets: datasets },
			options: {
				responsive: true,
				plugins: { tooltip: { callbacks: { label: (c) => c.dataset.label + ': ' + money(c.parsed.y) } } },
				scales: { y: { beginAtZero: true, ticks: { callback: (v) => number(v) } } }
			}
		});
	}

	function renderStatus(rows) {
		draw('status', 'chart-status', {
			type: 'doughnut',
			data: {
				labels: rows.map((r) => r.label),
				datasets: [{ data: rows.map((r) => r.count) }]
			},
			options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
		});
	}

	function renderShops(rows) {
		draw('shops', 'chart-shops', {
			type: 'bar',
			data: {
				labels: rows.map((r) => r.shopName),
				datasets: [{ label: 'Doanh thu', data: rows.map((r) => r.revenue), backgroundColor: '#0d6efd' }]
			},
			options: {
				indexAxis: 'y',
				responsive: true,
				plugins: { legend: { display: false },
					tooltip: { callbacks: { label: (c) => money(c.parsed.x) } } },
				scales: { x: { beginAtZero: true, ticks: { callback: (v) => number(v) } } }
			}
		});
	}

	function renderProducts(rows) {
		draw('products', 'chart-products', {
			type: 'bar',
			data: {
				labels: rows.map((r) => r.productName),
				datasets: [{ label: 'Số lượng bán', data: rows.map((r) => r.quantity), backgroundColor: '#fd7e14' }]
			},
			options: {
				indexAxis: 'y',
				responsive: true,
				plugins: { legend: { display: false } },
				scales: { x: { beginAtZero: true, ticks: { precision: 0 } } }
			}
		});
	}

	async function load() {
		const from = $('dash-from').value;
		const to = $('dash-to').value;
		if (!from || !to) {
			showError('Vui lòng chọn đủ khoảng ngày');
			return;
		}
		if (from > to) {
			showError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc');
			return;
		}
		showError('');

		const range = new URLSearchParams({ from: from, to: to });
		const revenueParams = new URLSearchParams({ from: from, to: to, group: $('dash-group').value });
		const topParams = new URLSearchParams({ from: from, to: to, limit: '5' });
		const button = $('dash-apply');
		button.disabled = true;
		try {
			const [summary, revenue, status, shops, products] = await Promise.all([
				getJson('/summary', range),
				getJson('/revenue', revenueParams),
				getJson('/orders-by-status', range),
				getJson('/top-shops', topParams),
				getJson('/top-products', topParams)
			]);
			renderSummary(summary);
			renderRevenue(revenue);
			renderStatus(status);
			renderShops(shops);
			renderProducts(products);
		} catch (error) {
			showError(error.message);
		} finally {
			button.disabled = false;
		}
	}

	const today = new Date();
	const start = new Date();
	start.setDate(today.getDate() - 29);
	$('dash-from').value = formatDate(start);
	$('dash-to').value = formatDate(today);

	$('dash-filter').addEventListener('submit', function (event) {
		event.preventDefault();
		load();
	});
	$('dash-group').addEventListener('change', load);

	load();
})();