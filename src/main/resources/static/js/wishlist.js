"use strict";

(() => {
	const buttons = [...document.querySelectorAll("[data-wishlist-button]")];
	if (buttons.length === 0) return;

	const setState = (productId, wishlisted) => {
		document.querySelectorAll(`[data-wishlist-button][data-product-id="${CSS.escape(String(productId))}"]`)
			.forEach((button) => {
				button.classList.toggle("is-wishlisted", wishlisted);
				button.setAttribute("aria-pressed", String(wishlisted));
				button.setAttribute("aria-label", wishlisted ? "Bỏ khỏi yêu thích" : "Thêm vào yêu thích");
				const icon = button.querySelector("i");
				if (icon) icon.className = wishlisted ? "bi bi-heart-fill" : "bi bi-heart";
			});
	};

	const authenticatedButtons = buttons.filter((button) => button.dataset.needsLogin !== "true");
	if (authenticatedButtons.length > 0) {
		const ids = [...new Set(authenticatedButtons.map((button) => button.dataset.productId).filter(Boolean))];
		const params = new URLSearchParams();
		ids.forEach((id) => params.append("productIds", id));
		fetch(`/api/wishlist/status?${params.toString()}`, { credentials: "same-origin" })
			.then((response) => response.ok ? response.json() : null)
			.then((statuses) => {
				if (statuses && typeof statuses === "object") {
					Object.entries(statuses).forEach(([id, wishlisted]) => setState(id, wishlisted === true));
				}
			})
			.catch(() => {});
	}

	buttons.forEach((button) => {
		button.addEventListener("click", async () => {
			if (button.dataset.needsLogin === "true") {
				window.App?.showToast("Vui lòng đăng nhập để thêm vào yêu thích", "info");
				window.setTimeout(() => {
					const returnUrl = `${location.pathname}${location.search}`;
					location.assign(`/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`);
				}, 650);
				return;
			}

			const productId = button.dataset.productId;
			if (!productId || !window.App?.fetchWithCsrf) return;
			window.App?.setLoading(button, true, { text: "Đang cập nhật..." });
			button.classList.add("is-pressed");
			try {
				const response = await window.App.fetchWithCsrf(`/api/wishlist/toggle/${encodeURIComponent(productId)}`, {
					method: "POST",
					showErrorToast: false
				});
				window.App?.setLoading(button, false);
				setState(productId, response.wishlisted === true);
				if (response.favoriteCount != null) {
					document.querySelectorAll(`[data-product-favorite-count="${CSS.escape(String(productId))}"]`)
						.forEach((count) => { count.textContent = String(response.favoriteCount); });
				}
				window.App.showToast(response.wishlisted ? "Đã thêm vào yêu thích" : "Đã bỏ khỏi yêu thích", "success");
				if (!response.wishlisted) {
					const card = button.closest("[data-wishlist-card]");
					card?.remove();
					const grid = document.querySelector("[data-wishlist-grid]");
					if (card && grid) {
						const page = document.querySelector("#wishlist-page");
						const total = Math.max(0, Number(page?.dataset.wishlistTotal || 0) - 1);
						if (page) page.dataset.wishlistTotal = String(total);
						if (total === 0) {
							grid.classList.add("d-none");
							document.querySelector("[data-wishlist-empty-client]")?.classList.remove("d-none");
						} else if (grid.querySelectorAll("[data-wishlist-card]").length === 0) {
							grid.classList.add("d-none");
							document.querySelector("[data-wishlist-page-empty]")?.classList.remove("d-none");
						}
					}
				}
			} catch (error) {
				if (error?.status === 401) {
					const returnUrl = `${location.pathname}${location.search}`;
					location.assign(`/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`);
				} else {
					window.App.showToast(error?.message || "Không thể cập nhật yêu thích", "error");
				}
			} finally {
				window.App?.setLoading(button, false);
				window.setTimeout(() => button.classList.remove("is-pressed"), 180);
			}
		});
	});
})();
