"use strict";

(() => {
	const vnd = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });
	const countFormat = new Intl.NumberFormat("vi-VN");
	const revenueCanvas = document.getElementById("vendorRevenueTrendChart");
	if (!revenueCanvas) return;

	const charts = {};
	const palette = ["#f5c518", "#a890ff", "#62d49d", "#ff806d", "#6fc8e8", "#f19bd2", "#8b93ff", "#ffae57", "#c1e36d", "#b7a6ff", "#e87979", "#70c6a0"];
	let groupBy = "day";

	const setChartVisibility = (key, hasData) => {
		const canvas = document.querySelector(`[data-chart-panel="${key}"] canvas`);
		const empty = document.querySelector(`[data-chart-empty="${key}"]`);
		canvas?.classList.toggle("d-none", !hasData);
		empty?.classList.toggle("d-none", hasData);
	};

	const updateChart = (key, chart, labels, values, datasetLabel, type = "bar", options = {}) => {
		const hasData = labels.length > 0 && values.length > 0;
		setChartVisibility(key, hasData);
		if (!hasData) {
			if (chart) chart.data.datasets[0].data = [];
			chart?.update();
			return chart;
		}
		const data = {
			labels,
			datasets: [{
				label: datasetLabel,
				data: values,
				backgroundColor: options.colors || "rgba(168, 144, 255, .72)",
				borderColor: options.borderColor || "#a890ff",
				borderWidth: type === "line" ? 3 : 1,
				borderRadius: type === "bar" ? 7 : 0,
				fill: type === "line",
				tension: .32,
				pointRadius: type === "line" ? 4 : 0,
				pointHoverRadius: 6
			}]
		};
		if (chart) {
			chart.data = data;
			chart.update();
			return chart;
		}
		const canvas = document.querySelector(`[data-chart-panel="${key}"] canvas`);
		return new Chart(canvas, {
			type,
			data,
			options: {
				responsive: true,
				maintainAspectRatio: false,
				indexAxis: options.indexAxis || "x",
				plugins: {
					legend: { display: options.legend || false, labels: { color: "#f5f2fc", usePointStyle: true, padding: 18 } },
					tooltip: { callbacks: { label: context => `${context.dataset.label || context.label}: ${options.money ? vnd.format(context.parsed.y ?? context.parsed) : countFormat.format(context.parsed.x ?? context.parsed)}` } }
				},
				scales: type === "doughnut" ? {} : type === "line" ? {
					x: { grid: { color: "rgba(245,242,252,.09)" }, ticks: { color: "#d2cce3" } },
					y: { beginAtZero: true, grid: { color: "rgba(245,242,252,.09)" }, ticks: { color: "#d2cce3", callback: value => vnd.format(value) } }
				} : {
					x: { beginAtZero: true, grid: { color: "rgba(245,242,252,.09)" }, ticks: { color: "#d2cce3", callback: value => countFormat.format(value) } },
					y: { grid: { color: "rgba(245,242,252,.09)" }, ticks: { color: "#d2cce3" } }
				}
			}
		});
	};

	const loadCharts = async () => {
		const params = new URLSearchParams({ groupBy });
		const from = document.getElementById("revenueFrom")?.value;
		const to = document.getElementById("revenueTo")?.value;
		if (from) params.set("from", from);
		if (to) params.set("to", to);
		try {
			const payload = await window.App.fetchWithCsrf(`/api/vendor/revenue?${params.toString()}`, { showErrorToast: false });
			charts.revenue = updateChart("revenue", charts.revenue,
				(payload.revenue || []).map(point => point.label),
				(payload.revenue || []).map(point => Number(point.amount)),
				"Doanh thu thực nhận", "line", { money: true, borderColor: "#f5c518", colors: "rgba(245,197,24,.18)" });
			charts.products = updateChart("products", charts.products,
				(payload.topProducts || []).map(product => product.productName),
				(payload.topProducts || []).map(product => Number(product.quantitySold)),
				"Số lượng đã bán", "bar", { indexAxis: "y", colors: "rgba(168,144,255,.76)" });

			const statuses = payload.orderStatuses || [];
			const statusLabels = statuses.map(status => status.label);
			const statusValues = statuses.map(status => Number(status.orderCount));
			setChartVisibility("statuses", statusLabels.length > 0 && statusValues.length > 0);
			if (statusLabels.length && statusValues.length) {
				const data = { labels: statusLabels, datasets: [{ label: "Số đơn", data: statusValues, backgroundColor: statusLabels.map((_, index) => palette[index % palette.length]), borderColor: "#1a1a2e", borderWidth: 3, hoverOffset: 8 }] };
				if (charts.statuses) { charts.statuses.data = data; charts.statuses.update(); }
				else charts.statuses = new Chart(document.getElementById("vendorRevenueStatusesChart"), {
					type: "doughnut", data,
					options: { responsive: true, maintainAspectRatio: false, cutout: "62%", plugins: {
						legend: { display: true, position: "bottom", labels: { color: "#f5f2fc", usePointStyle: true, padding: 16 } },
						tooltip: { callbacks: { label: context => `${context.label}: ${countFormat.format(context.parsed)} đơn` } }
					} }
				});
			} else if (charts.statuses) { charts.statuses.data.datasets[0].data = []; charts.statuses.update(); }
		} catch (error) {
			window.App.showToast(error instanceof Error ? error.message : "Không thể tải biểu đồ doanh thu.", "error");
		}
	};

	document.querySelectorAll("[data-revenue-group]").forEach(button => {
		button.addEventListener("click", () => {
			groupBy = button.dataset.revenueGroup;
			document.querySelectorAll("[data-revenue-group]").forEach(item => {
				const active = item === button;
				item.classList.toggle("active", active);
				item.setAttribute("aria-pressed", String(active));
			});
			loadCharts();
		});
	});

	document.getElementById("revenueFrom")?.addEventListener("change", loadCharts);
	document.getElementById("revenueTo")?.addEventListener("change", loadCharts);
	if (window.Chart) loadCharts();
	else window.App.showToast("Không tải được thư viện biểu đồ Chart.js.", "error");
})();
