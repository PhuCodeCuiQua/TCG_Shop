"use strict";

document.addEventListener("DOMContentLoaded", () => {
	const fileInput = document.querySelector("#avatar");
	const preview = document.querySelector("#avatar-preview");
	const initial = document.querySelector("#avatar-initial");
	const avatarForm = document.querySelector("#avatar-form");
	const deleteForm = document.querySelector("#delete-avatar-form");
	const feedback = document.querySelector("#avatar-feedback");
	const maxBytes = 5 * 1024 * 1024;

	fileInput?.addEventListener("change", () => {
		const file = fileInput.files?.[0];
		if (!file) return;
		if (file.size >= maxBytes) {
			fileInput.value = "";
			window.App.showToast("Ảnh phải có dung lượng nhỏ hơn 5 MB.", "error");
			return;
		}
		if (!window.App.previewImage(file, preview, { maxSizeBytes: maxBytes })) {
			fileInput.value = "";
			return;
		}
		preview.addEventListener("load", () => {
			preview.classList.remove("d-none");
			initial?.classList.add("d-none");
		}, { once: true });
	});

	avatarForm?.addEventListener("submit", async (event) => {
		event.preventDefault();
		if (avatarForm.getAttribute("aria-busy") === "true") return;
		const file = fileInput?.files?.[0];
		if (!file) {
			window.App.showToast("Vui lòng chọn ảnh.", "error");
			return;
		}
		if (file.size >= maxBytes) {
			window.App.showToast("Ảnh phải có dung lượng nhỏ hơn 5 MB.", "error");
			return;
		}
		const button = avatarForm.querySelector('button[type="submit"]');
		avatarForm.setAttribute("aria-busy", "true");
		feedback?.classList.add("d-none");
		try {
			// Gửi token bằng header để Security kiểm tra trước khi MVC phân tích multipart body.
			const profile = await window.App.fetchWithCsrf(avatarForm.action, {
				method: "POST",
				headers: { Accept: "application/json", "X-Requested-With": "XMLHttpRequest" },
				body: new FormData(avatarForm),
				button,
				loadingText: "Đang tải ảnh...",
				showErrorToast: false
			});
			if (profile.avatarUrl && preview) {
				preview.src = profile.avatarUrl;
				preview.classList.remove("d-none");
				preview.hidden = false;
				initial?.classList.add("d-none");
			}
			window.location.assign("/profile?avatarUpdated=true#avatar-heading");
		} catch (error) {
			if (window.App?.handleAuthenticationError(error)) return;
			const message = error?.status === 403
				? "Không thể xác thực yêu cầu đổi ảnh. Vui lòng tải lại trang rồi thử lại."
				: error?.message || "Không thể đổi ảnh đại diện. Vui lòng thử lại.";
			if (feedback) {
				feedback.textContent = message;
				feedback.className = "alert alert-danger mt-3 mb-0";
			}
			window.App?.showToast(message, "error");
		} finally {
			avatarForm.removeAttribute("aria-busy");
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
			window.App?.showToast("Không thể mở hộp thoại xác nhận. Vui lòng tải lại trang.", "error");
			confirmed = false;
		}
		if (confirmed) {
			window.App?.setLoading(deleteForm.querySelector('button[type="submit"]'), true, { text: "Đang xóa..." });
			deleteForm.submit();
		}
	});
});
