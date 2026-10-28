document.addEventListener("DOMContentLoaded", () => {
	const form = document.querySelector(".shop-filter-panel[data-series-endpoint]");
	const gameSelect = document.getElementById("shopGameFilter");
	const seriesSelect = document.getElementById("shopSeriesFilter");
	const errorMessage = form?.querySelector(".shop-series-load-error");

	if (!form || !gameSelect || !seriesSelect || !errorMessage) {
		return;
	}

	let activeRequest;

	gameSelect.addEventListener("change", async () => {
		activeRequest?.abort();
		seriesSelect.replaceChildren(new Option("Tất cả Series", ""));
		seriesSelect.disabled = true;
		errorMessage.hidden = true;
		errorMessage.textContent = "";

		const request = new AbortController();
		activeRequest = request;
		const endpoint = new URL(form.dataset.seriesEndpoint, window.location.origin);
		if (gameSelect.value) {
			endpoint.searchParams.set("gameId", gameSelect.value);
		}

		try {
			const response = await fetch(endpoint, {
				headers: { Accept: "application/json" },
				signal: request.signal
			});
			if (!response.ok) {
				throw new Error(`Series options request failed with status ${response.status}`);
			}

			const seriesOptions = await response.json();
			seriesOptions.forEach((series) => {
				seriesSelect.add(new Option(series.name, series.id));
			});
		} catch (error) {
			if (error.name !== "AbortError") {
				console.error("Không thể tải danh sách Series:", error);
				errorMessage.textContent = "Không tải được danh sách Series. Vui lòng thử lại.";
				errorMessage.hidden = false;
			}
		} finally {
			if (activeRequest === request) {
				seriesSelect.disabled = false;
				activeRequest = undefined;
			}
		}
	});
});
