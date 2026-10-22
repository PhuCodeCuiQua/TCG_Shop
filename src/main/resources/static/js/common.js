"use strict";

(() => {
	const loadingStates = new WeakMap();
	let confirmModal;
	let confirmModalInstance;
	let pendingConfirmation;

	const readCookie = (name) => {
		const prefix = `${name}=`;
		const cookie = document.cookie.split("; ").find((entry) => entry.startsWith(prefix));
		if (!cookie) {
			return null;
		}

		try {
			return decodeURIComponent(cookie.substring(prefix.length));
		} catch {
			return cookie.substring(prefix.length);
		}
	};

	const getCsrf = () => {
		const token = document.querySelector('meta[name="_csrf"]')?.content || readCookie("XSRF-TOKEN");
		const header = document.querySelector('meta[name="_csrf_header"]')?.content || "X-XSRF-TOKEN";
		return { token, header };
	};

	const createToastContainer = () => {
		let container = document.querySelector(".toast-container");
		if (!container) {
			container = document.createElement("div");
			container.className = "toast-container position-fixed top-0 end-0 p-3";
			container.setAttribute("aria-live", "polite");
			container.setAttribute("aria-atomic", "true");
			document.body.append(container);
		}
		return container;
	};

	/**
	 * Hiển thị toast Bootstrap trong container chung của layout, hoặc tạo container nếu trang chưa có.
	 * @param {string} message Nội dung hiển thị dưới dạng text an toàn.
	 * @param {"success"|"error"|"warning"|"info"} [type="info"] Loại thông báo.
	 * @returns {void}
	 */
	function showToast(message, type = "info") {
		const variants = {
			success: { className: "text-bg-success", role: "status", live: "polite" },
			error: { className: "text-bg-danger", role: "alert", live: "assertive" },
			warning: { className: "text-bg-warning", role: "alert", live: "assertive" },
			info: { className: "text-bg-primary", role: "status", live: "polite" }
		};
		const variant = variants[type] || variants.info;
		const toast = document.createElement("div");
		toast.className = `toast ${variant.className} border-0 mb-2`;
		toast.setAttribute("role", variant.role);
		toast.setAttribute("aria-live", variant.live);
		toast.setAttribute("aria-atomic", "true");

		const row = document.createElement("div");
		row.className = "d-flex";
		const body = document.createElement("div");
		body.className = "toast-body";
		body.textContent = String(message ?? "");
		const close = document.createElement("button");
		close.type = "button";
		close.className = `btn-close${type === "warning" ? "" : " btn-close-white"} me-2 m-auto`;
		close.setAttribute("data-bs-dismiss", "toast");
		close.setAttribute("aria-label", "Đóng");
		row.append(body, close);
		toast.append(row);
		createToastContainer().append(toast);

		if (!window.bootstrap?.Toast) {
			toast.classList.add("show");
			window.setTimeout(() => toast.remove(), 3000);
			return;
		}

		toast.addEventListener("hidden.bs.toast", () => toast.remove(), { once: true });
		window.bootstrap.Toast.getOrCreateInstance(toast, { delay: 3000, autohide: true }).show();
	}

	/**
	 * Gửi fetch cùng CSRF header cho request cùng origin có thay đổi dữ liệu; mặc định báo lỗi bằng toast.
	 * @param {RequestInfo|URL} url URL hoặc Request cần gọi.
	 * @param {RequestInit & {button?: HTMLElement, loadingText?: string, showErrorToast?: boolean}} [options={}] Tùy chọn fetch, nút loading và toast lỗi.
	 * @returns {Promise<unknown>} JSON đã parse; reject nếu response lỗi hoặc không phải JSON.
	 */
	async function fetchWithCsrf(url, options = {}) {
		const {
			button,
			loadingText,
			showErrorToast = true,
			...fetchOptions
		} = options;
		const method = (fetchOptions.method || (url instanceof Request ? url.method : "GET")).toUpperCase();
		const isMutation = !["GET", "HEAD", "OPTIONS"].includes(method);
		const requestUrl = url instanceof Request ? url.url : url.toString();
		const isSameOrigin = new URL(requestUrl, window.location.href).origin === window.location.origin;
		const headers = new Headers(url instanceof Request ? url.headers : undefined);
		new Headers(fetchOptions.headers).forEach((value, name) => headers.set(name, value));

		if (isMutation && isSameOrigin) {
			const csrf = getCsrf();
			if (!csrf.token) {
				const error = new Error("Không tìm thấy CSRF token. Hãy tải lại trang hoặc kiểm tra cấu hình CSRF.");
				if (showErrorToast) {
					showToast(error.message, "error");
				}
				throw error;
			}
			headers.set(csrf.header, csrf.token);
		}

		if (button) {
			setLoading(button, true, { text: loadingText });
		}

		try {
			const response = await window.fetch(url, {
				...fetchOptions,
				headers,
				credentials: fetchOptions.credentials || "same-origin"
			});
			const contentType = response.headers.get("content-type") || "";
			const isJson = /(?:application\/json|[+]json)(?:\s*;|$)/i.test(contentType);
			let data;

			if (isJson) {
				try {
					data = await response.json();
				} catch {
					throw new Error("Máy chủ trả về JSON không hợp lệ.");
				}
			}

			if (!response.ok) {
				const message = data && typeof data.message === "string"
					? data.message
					: `Yêu cầu thất bại (HTTP ${response.status}).`;
				throw new Error(message);
			}

			if (!isJson) {
				throw new Error("Máy chủ trả về dữ liệu không phải JSON.");
			}

			return data;
		} catch (error) {
			if (showErrorToast) {
				showToast(error instanceof Error ? error.message : "Không thể hoàn tất yêu cầu.", "error");
			}
			throw error;
		} finally {
			if (button) {
				setLoading(button, false);
			}
		}
	}

	const createConfirmModal = () => {
		const modal = document.createElement("div");
		modal.className = "modal fade";
		modal.tabIndex = -1;
		modal.setAttribute("aria-hidden", "true");

		const dialog = document.createElement("div");
		dialog.className = "modal-dialog modal-dialog-centered";
		const content = document.createElement("div");
		content.className = "modal-content";

		const header = document.createElement("div");
		header.className = "modal-header";
		const title = document.createElement("h5");
		title.className = "modal-title";
		title.id = "commonConfirmModalTitle";
		const close = document.createElement("button");
		close.type = "button";
		close.className = "btn-close";
		close.setAttribute("data-bs-dismiss", "modal");
		close.setAttribute("aria-label", "Đóng");
		header.append(title, close);

		const body = document.createElement("div");
		body.className = "modal-body";
		const message = document.createElement("p");
		message.className = "mb-0";
		body.append(message);

		const footer = document.createElement("div");
		footer.className = "modal-footer";
		const cancel = document.createElement("button");
		cancel.type = "button";
		cancel.className = "btn btn-outline-secondary";
		cancel.setAttribute("data-bs-dismiss", "modal");
		const confirm = document.createElement("button");
		confirm.type = "button";
		footer.append(cancel, confirm);
		content.append(header, body, footer);
		dialog.append(content);
		modal.append(dialog);
		modal.setAttribute("aria-labelledby", title.id);
		modal.confirmTitle = title;
		modal.confirmMessage = message;
		modal.confirmCancel = cancel;
		modal.confirmAccept = confirm;
		document.body.append(modal);
		return modal;
	};

	/**
	 * Mở modal xác nhận Bootstrap và resolve true khi xác nhận, false khi hủy/đóng.
	 * @param {{title?: string, message?: string, confirmText?: string, cancelText?: string, type?: "danger"|"warning"|"primary"|"success"|"info"}} [options={}] Nội dung và kiểu nút xác nhận.
	 * @returns {Promise<boolean>} Kết quả xác nhận của người dùng.
	 */
	function showConfirmModal(options = {}) {
		if (!window.bootstrap?.Modal) {
			return Promise.reject(new Error("Bootstrap Modal chưa được tải."));
		}
		if (pendingConfirmation) {
			return Promise.resolve(false);
		}

		const modal = confirmModal || (confirmModal = createConfirmModal());
		const confirmButtonClasses = {
			danger: "btn-danger",
			warning: "btn-warning",
			primary: "btn-primary",
			success: "btn-success",
			info: "btn-info"
		};
		modal.confirmTitle.textContent = options.title || "Xác nhận thao tác";
		modal.confirmMessage.textContent = options.message || "Bạn có chắc chắn muốn tiếp tục?";
		modal.confirmCancel.textContent = options.cancelText || "Hủy";
		modal.confirmAccept.textContent = options.confirmText || "Xác nhận";
		modal.confirmAccept.className = `btn ${confirmButtonClasses[options.type] || confirmButtonClasses.danger}`;
		confirmModalInstance = window.bootstrap.Modal.getOrCreateInstance(modal);

		return new Promise((resolve) => {
			pendingConfirmation = { resolve, result: false };
			const finish = (result) => {
				if (!pendingConfirmation) {
					return;
				}
				pendingConfirmation.result = result;
				confirmModalInstance.hide();
			};
			const handleHidden = () => {
				if (pendingConfirmation) {
					pendingConfirmation.resolve(pendingConfirmation.result);
					pendingConfirmation = null;
				}
				modal.confirmAccept.removeEventListener("click", handleConfirm);
				modal.removeEventListener("hidden.bs.modal", handleHidden);
			};
			const handleConfirm = () => finish(true);
			modal.confirmAccept.addEventListener("click", handleConfirm);
			modal.addEventListener("hidden.bs.modal", handleHidden);
			confirmModalInstance.show();
		});
	}

	/**
	 * Kiểm tra file ảnh phía trình duyệt, sau đó đọc bằng FileReader và gán data URL cho thẻ img.
	 * @param {File} file File ảnh cần xem trước.
	 * @param {HTMLImageElement} previewElement Phần tử img nhận ảnh xem trước.
	 * @param {{maxSizeBytes?: number, alt?: string}} [options={}] Giới hạn dung lượng và văn bản thay thế.
	 * @returns {boolean} true nếu file đạt kiểm tra cơ bản và bắt đầu đọc; false nếu bị từ chối.
	 */
	function previewImage(file, previewElement, options = {}) {
		const maxSizeBytes = options.maxSizeBytes ?? 5 * 1024 * 1024;
		const allowedExtensions = /\.(jpe?g|png)$/i;
		const allowedTypes = ["image/jpeg", "image/png"];
		if (!file || !previewElement || previewElement.tagName !== "IMG") {
			showToast("Chọn tệp ảnh và phần tử xem trước hợp lệ.", "error");
			return false;
		}
		previewElement.hidden = true;
		previewElement.classList.add("d-none");
		previewElement.removeAttribute("src");
		if (!allowedExtensions.test(file.name) || (file.type && !allowedTypes.includes(file.type.toLowerCase()))) {
			showToast("Chỉ chấp nhận ảnh JPG, JPEG hoặc PNG.", "error");
			return false;
		}
		if (file.size >= maxSizeBytes) {
			showToast("Ảnh phải có dung lượng nhỏ hơn 5MB.", "error");
			return false;
		}

		const reader = new FileReader();
		reader.addEventListener("load", () => {
			if (typeof reader.result === "string") {
				previewElement.src = reader.result;
				previewElement.alt = options.alt || file.name;
				previewElement.hidden = false;
				previewElement.classList.remove("d-none");
			}
		});
		reader.addEventListener("error", () => showToast("Không thể đọc tệp ảnh đã chọn.", "error"));
		reader.readAsDataURL(file);
		return true;
	}

	/**
	 * Đặt nút vào trạng thái loading, lưu và khôi phục nội dung/trạng thái ban đầu.
	 * @param {HTMLElement} element Nút hoặc phần tử cần hiển thị trạng thái gửi.
	 * @param {boolean} isLoading true để hiện spinner; false để khôi phục.
	 * @param {{text?: string}} [options={}] Nội dung khi đang tải.
	 * @returns {void}
	 */
	function setLoading(element, isLoading, options = {}) {
		if (!element) {
			return;
		}
		if (isLoading) {
			if (!loadingStates.has(element)) {
				loadingStates.set(element, {
					html: element.innerHTML,
					disabled: "disabled" in element ? element.disabled : null,
					ariaBusy: element.getAttribute("aria-busy")
				});
			}
			if ("disabled" in element) {
				element.disabled = true;
			}
			element.setAttribute("aria-busy", "true");
			const spinner = document.createElement("span");
			spinner.className = "spinner-border spinner-border-sm me-2";
			spinner.setAttribute("aria-hidden", "true");
			const label = document.createElement("span");
			label.textContent = options.text || "Đang xử lý...";
			element.replaceChildren(spinner, label);
			return;
		}

		const state = loadingStates.get(element);
		if (!state) {
			return;
		}
		element.innerHTML = state.html;
		if (state.disabled !== null) {
			element.disabled = state.disabled;
		}
		if (state.ariaBusy === null) {
			element.removeAttribute("aria-busy");
		} else {
			element.setAttribute("aria-busy", state.ariaBusy);
		}
		loadingStates.delete(element);
	}

	/**
	 * Hiện spinner trên phần tử; dùng như alias tiện lợi của setLoading(element, true).
	 * @param {HTMLElement} element Phần tử cần hiển thị spinner.
	 * @param {{text?: string}} [options={}] Nội dung loading.
	 * @returns {void}
	 */
	function showSpinner(element, options = {}) {
		setLoading(element, true, options);
	}

	/**
	 * Ẩn spinner và khôi phục phần tử về trạng thái trước đó.
	 * @param {HTMLElement} element Phần tử cần khôi phục.
	 * @returns {void}
	 */
	function hideSpinner(element) {
		setLoading(element, false);
	}

	window.App = window.App || {};
	window.App.fetchWithCsrf = fetchWithCsrf;
	window.App.showToast = showToast;
	window.App.showConfirmModal = showConfirmModal;
	window.App.previewImage = previewImage;
	window.App.setLoading = setLoading;
	window.App.showSpinner = showSpinner;
	window.App.hideSpinner = hideSpinner;
})();
