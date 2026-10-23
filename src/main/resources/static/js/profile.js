"use strict";

document.addEventListener("DOMContentLoaded", () => {
	const fileInput = document.querySelector("#avatar");
	const preview = document.querySelector("#avatar-preview");
	const initial = document.querySelector("#avatar-initial");
	const avatarForm = document.querySelector("#avatar-form");
	const deleteForm = document.querySelector("#delete-avatar-form");
	const maxBytes = 5 * 1024 * 1024;

	fileInput?.addEventListener("change", () => {
		const file = fileInput.files?.[0];
		if (!file) return;
		if (file.size > maxBytes) {
			fileInput.value = "";
			window.App.showToast("Ảnh phải có dung lượng không quá 5 MB.", "error");
			return;
		}
		if (!window.App.previewImage(file, preview, { maxSizeBytes: maxBytes + 1 })) {
			fileInput.value = "";
			return;
		}
		preview.addEventListener("load", () => {
			preview.classList.remove("d-none");
			initial?.classList.add("d-none");
		}, { once: true });
	});

	avatarForm?.addEventListener("submit", (event) => {
		const file = fileInput?.files?.[0];
		if (!file) {
			event.preventDefault();
			window.App.showToast("Vui lòng chọn ảnh.", "error");
		} else if (file.size > maxBytes) {
			event.preventDefault();
			window.App.showToast("Ảnh phải có dung lượng không quá 5 MB.", "error");
		}
	});

	deleteForm?.addEventListener("submit", async (event) => {
		event.preventDefault();
		let confirmed;
		try {
			confirmed = await window.App.showConfirmModal({
				title: "Xóa ảnh đại diện",
				message: "Bạn có chắc chắn muốn xóa ảnh đại diện?",
				confirmText: "Xóa ảnh",
				type: "danger"
			});
		} catch {
			confirmed = window.confirm("Bạn có chắc chắn muốn xóa ảnh đại diện?");
		}
		if (confirmed) deleteForm.submit();
	});
});
