"use strict";

(() => {
	const form = document.getElementById("contactForm");
	if (!form) return;

	form.addEventListener("submit", (event) => {
		event.preventDefault();
		window.App?.showToast(
			"Cảm ơn bạn đã liên hệ. Chúng tôi sẽ phản hồi sớm nhất!",
			"success"
		);
		form.reset();
	});
})();
