"use strict";

(() => {
	const showComingSoon = (event) => {
		event.preventDefault();
		window.App?.showToast("Tính năng đăng bán sản phẩm đang được phát triển", "info");
	};

	document.querySelectorAll(".js-coming-soon").forEach((button) => {
		button.addEventListener("click", showComingSoon);
	});

	document.querySelectorAll("#vendorProductFilterForm, #vendorProductFilterOptionsForm").forEach((form) => {
		form.addEventListener("submit", (event) => {
			const button = event.submitter || form.querySelector("button[type='submit']");
			if (button && window.App?.setLoading) {
				window.App.setLoading(button, true, { text: button.dataset.loadingText || "Đang tải..." });
			}
		});
	});
})();
