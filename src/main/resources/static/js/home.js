"use strict";

(() => {
	const form = document.querySelector("#homeQuickSearch");
	const input = document.querySelector("#homeQuickSearchInput");
	const cards = Array.from(document.querySelectorAll("[data-home-action]"));
	const emptyState = document.querySelector("#homeSearchEmpty");

	if (!form || !input || !emptyState || cards.length === 0) {
		return;
	}

	const filterActions = () => {
		const keyword = input.value.trim().toLocaleLowerCase("vi");
		let visibleCount = 0;

		cards.forEach((card) => {
			const searchableText = `${card.dataset.search || ""} ${card.textContent}`.toLocaleLowerCase("vi");
			const matches = keyword.length === 0 || searchableText.includes(keyword);
			card.classList.toggle("d-none", !matches);
			visibleCount += matches ? 1 : 0;
		});

		emptyState.classList.toggle("d-none", visibleCount !== 0);
	};

	form.addEventListener("submit", (event) => {
		event.preventDefault();
		filterActions();
		document.querySelector("#homeActionList")?.scrollIntoView({ behavior: "smooth", block: "start" });
	});

	input.addEventListener("input", filterActions);
})();
