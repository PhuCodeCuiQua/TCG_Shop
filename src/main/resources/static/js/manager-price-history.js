document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll("[data-overwrite-form]").forEach((form) => {
		form.addEventListener("submit", (event) => {
			const overwrite = form.querySelector('input[name="overwriteExisting"]');
			if (overwrite?.checked
				&& !window.confirm("Bản ghi cùng thẻ và ngày đã tồn tại. Bạn có chắc muốn ghi đè không?")) {
				event.preventDefault();
			}
		});
	});
});
