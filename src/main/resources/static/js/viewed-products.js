"use strict";

(() => {
	const productId = window.CURRENT_PRODUCT_ID
		|| document.getElementById("product-detail")?.dataset.productId;
	if (!productId || !window.App?.fetchWithCsrf) return;

	window.App.fetchWithCsrf(`/api/viewed-products/record/${encodeURIComponent(productId)}`, {
		method: "POST",
		showErrorToast: false
	}).catch(() => {
		// Ghi lịch sử xem là thao tác nền; lỗi không làm gián đoạn trang chi tiết.
	});
})();
