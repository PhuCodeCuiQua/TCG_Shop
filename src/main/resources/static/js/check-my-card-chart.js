(function () {
	'use strict';

	const canvas = document.getElementById('check-card-chart');
	if (!canvas || !window.Chart) return;

	const points = Array.isArray(window.CHECK_MY_CARD_POINTS) ? window.CHECK_MY_CARD_POINTS : [];
	const formatter = new Intl.NumberFormat('vi-VN');
	new window.Chart(canvas, {
		type: 'line',
		data: {
			labels: points.map((point) => point.date),
			datasets: [{
				label: 'Giá thị trường',
				data: points.map((point) => point.marketPrice),
				borderColor: '#2563eb',
				backgroundColor: 'rgba(37, 99, 235, 0.12)',
				fill: true,
				tension: 0.25,
				pointRadius: 2
			}]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			plugins: {
				legend: { display: false },
				tooltip: {
					callbacks: {
						label: (context) => formatter.format(Number(context.parsed.y)) + ' ₫'
					}
				}
			},
			scales: {
				x: { grid: { display: false } },
				y: {
					ticks: {
						callback: (value) => formatter.format(Number(value)) + ' ₫'
					}
				}
			}
		}
	});
})();
