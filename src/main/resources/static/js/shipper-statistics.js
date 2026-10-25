"use strict";

(() => {
	const panel = document.getElementById("shipperDeliveriesChartPanel");
	const canvas = document.getElementById("shipperDeliveriesChart");
	const emptyState = document.getElementById("shipperDeliveriesChartEmpty");
	if (!panel || !canvas || !emptyState) return;

	const numberFormat = new Intl.NumberFormat("vi-VN");
	const setEmpty = (isEmpty) => {
		canvas.classList.toggle("d-none", isEmpty);
		emptyState.classList.toggle("d-none", !isEmpty);
	};

	async function loadDeliveries() {
		const params = new URLSearchParams();
		if (panel.dataset.from) params.set("from", panel.dataset.from);
		if (panel.dataset.to) params.set("to", panel.dataset.to);
		const separator = panel.dataset.apiUrl.includes("?") ? "&" : "?";
		const url = `${panel.dataset.apiUrl}${separator}${params.toString()}`;

		try {
			const result = await window.App.fetchWithCsrf(url, {
				headers: { Accept: "application/json" },
				showErrorToast: false
			});
			const labels = Array.isArray(result.labels) ? result.labels : [];
			const counts = Array.isArray(result.orderCounts) ? result.orderCounts.map(Number) : [];
			if (!labels.length || !counts.length || labels.length !== counts.length) {
				setEmpty(true);
				return;
			}
			if (!window.Chart) {
				setEmpty(true);
				window.App.showToast("Không tải được thư viện biểu đồ Chart.js.", "error");
				return;
			}

			setEmpty(false);
			const chartLabels = labels.map((value) => {
				const date = new Date(`${value}T00:00:00`);
				return Number.isNaN(date.getTime())
					? value
					: date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
			});
			new window.Chart(canvas, {
				type: "bar",
				data: {
					labels: chartLabels,
					datasets: [{
						label: "Đơn giao thành công",
						data: counts,
						backgroundColor: "rgba(245, 197, 24, .78)",
						borderColor: "#f5c518",
						borderWidth: 1,
						borderRadius: 7,
						maxBarThickness: 36
					}]
				},
				options: {
					responsive: true,
					maintainAspectRatio: false,
					plugins: {
						legend: { display: false },
						tooltip: {
							callbacks: {
								label: (context) => `${context.dataset.label}: ${numberFormat.format(context.parsed.y)} đơn`
							}
						}
					},
					scales: {
						x: { grid: { display: false }, ticks: { color: "#d2cce3", maxRotation: 0, autoSkip: true } },
						y: {
							beginAtZero: true,
							ticks: { color: "#d2cce3", precision: 0, callback: (value) => numberFormat.format(value) },
							grid: { color: "rgba(245, 242, 252, .1)" }
						}
					}
				}
			});
		} catch (error) {
			setEmpty(true);
			window.App.showToast(error instanceof Error ? error.message : "Không tải được thống kê giao hàng.", "error");
		}
	}

	loadDeliveries();
})();
