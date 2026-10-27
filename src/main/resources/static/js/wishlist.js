"use strict";

(() => {
	const buttons = [...document.querySelectorAll("[data-wishlist-button]")];
	const resolveProductId = (button) => {
		const ownId = button.getAttribute("data-product-id")?.trim();
		const parentId = button.parentElement?.closest("[data-product-id]")?.getAttribute("data-product-id")?.trim();
		const productId = [ownId, parentId].find((id) => /^[1-9]\d*$/.test(id || ""));
		if (productId) button.dataset.productId = productId;
		return productId;
	};
	buttons.forEach(resolveProductId);
	const pendingProducts = new Set();

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
	if (authenticatedButtons.some((button) => resolveProductId(button))) {
		const ids = [...new Set(authenticatedButtons.map(resolveProductId).filter(Boolean))];
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

	document.addEventListener("click", async function(event) {
		if (!(event.target instanceof Element)) return;
		const button = event.target.closest('.js-wishlist-toggle');
		if (!button) return;
		const productId = resolveProductId(button);
		console.log("Đã bấm nút tim", productId);
		event.preventDefault();
		if (button.dataset.needsLogin === "true") {
			window.App?.promptLogin();
			return;
		}

		if (!productId) {
			window.App?.showToast("Không xác định được sản phẩm cần thêm vào yêu thích.", "error");
			return;
		}
		if (!window.App?.fetchWithCsrf) {
			window.App?.showToast("Không thể gửi yêu cầu. Vui lòng tải lại trang.", "error");
			return;
		}
		if (pendingProducts.has(productId)) return;
		pendingProducts.add(productId);
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
					document.querySelectorAll("[data-wishlist-count]").forEach((count) => { count.textContent = String(total); });
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
			if (window.App?.handleAuthenticationError(error)) return;
			if (error?.status === 403) {
				window.App.showToast(
					"Bạn chưa thể thực hiện thao tác này. Vui lòng tải lại trang hoặc kiểm tra quyền tài khoản.",
					"error");
			} else {
				window.App.showToast(error?.message || "Không thể cập nhật yêu thích", "error");
			}
		} finally {
			pendingProducts.delete(productId);
			window.App?.setLoading(button, false);
			window.setTimeout(() => button.classList.remove("is-pressed"), 180);
		}
	});
})();
