"use strict";

(() => {
	const host = document.getElementById("reviews-section");
	if (!host) return;
	const reviewUrl = host.dataset.reviewUrl;
	const loadReviews = (url) => fetch(url, {
		headers: { "X-Requested-With": "XMLHttpRequest" }, credentials: "same-origin"
	})
		.then((response) => {
			if (!response.ok) throw new Error("Không thể tải đánh giá sản phẩm.");
			return response.text();
		})
		.then((html) => {
			host.innerHTML = html;
			const root = host.querySelector("[data-review-section]");
			if (root) {
				updateProductRating(root);
				initialize(root);
			}
		})
		.catch((error) => {
			host.innerHTML = '<div class="alert alert-warning mb-0" role="status"></div>';
			host.querySelector(".alert").textContent = error.message;
		});
	host.addEventListener("click", (event) => {
			const link = event.target instanceof Element
				? event.target.closest("[data-pagination] a[data-pagination-page]") : null;
			if (!link) return;
			event.preventDefault();
			const url = new URL(reviewUrl, window.location.href);
			url.searchParams.set("page", link.dataset.paginationPage);
			loadReviews(url.toString());
		});
	loadReviews(reviewUrl);

	function updateProductRating(root) {
		const target = document.getElementById("product-rating-inline");
		if (!target) return;
		const average = Number(root.dataset.averageRating || 0);
		const count = Number(root.dataset.reviewCount || 0);
		target.replaceChildren();
		const score = document.createElement("strong");
		score.className = "product-rating-inline__score";
		score.textContent = average.toFixed(1);
		const stars = document.createElement("span");
		stars.className = "product-rating-inline__stars";
		stars.setAttribute("aria-label", `${average.toFixed(1)} trên 5 sao`);
		for (let star = 1; star <= 5; star += 1) {
			const icon = document.createElement("i");
			icon.className = `bi ${average >= star ? "bi-star-fill" : "bi-star"}`;
			stars.append(icon);
		}
		const total = document.createElement("span");
		total.className = "product-rating-inline__count";
		total.textContent = `(${count} đánh giá)`;
		target.append(score, stars, total);
	}

	function initialize(root) {

	const productId = root.dataset.productId;
	const form = root.querySelector("[data-review-form]");
	const imageInput = root.querySelector("[data-review-images]");
	const videoInput = root.querySelector("[data-review-video]");
	const preview = root.querySelector("[data-review-preview]");
	const comment = root.querySelector("[data-review-comment]");
	const counter = root.querySelector("[data-comment-counter]");
	const count = root.querySelector("[data-comment-count]");
	const commentError = root.querySelector("[data-comment-error]");
	let selectedImages = [];
	let selectedVideo = null;
	let videoPreviewUrl = null;

	const showToast = (message, type = "info") => window.App?.showToast(message, type);
	const syncFiles = (input, files) => {
		const transfer = new DataTransfer();
		files.forEach((file) => transfer.items.add(file));
		input.files = transfer.files;
	};
	const renderPreviews = () => {
		preview.replaceChildren();
		selectedImages.forEach((file, index) => {
			const item = document.createElement("div");
			item.className = "review-preview-item";
			const image = document.createElement("img");
			image.className = "review-preview-item__media";
			window.App.previewImage(file, image, { alt: file.name });
			const remove = document.createElement("button");
			remove.type = "button";
			remove.className = "review-preview-item__remove";
			remove.setAttribute("aria-label", "Xóa ảnh " + file.name);
			remove.innerHTML = '<i class="bi bi-x-lg" aria-hidden="true"></i>';
			remove.addEventListener("click", () => {
				selectedImages.splice(index, 1);
				syncFiles(imageInput, selectedImages);
				renderPreviews();
			});
			item.append(image, remove);
			preview.append(item);
		});
		if (selectedVideo) {
			if (videoPreviewUrl) URL.revokeObjectURL(videoPreviewUrl);
			const item = document.createElement("div");
			item.className = "review-preview-item review-preview-item--video";
			const video = document.createElement("video");
			video.className = "review-preview-item__media";
			videoPreviewUrl = URL.createObjectURL(selectedVideo);
			video.src = videoPreviewUrl;
			video.muted = true;
			video.playsInline = true;
			const remove = document.createElement("button");
			remove.type = "button";
			remove.className = "review-preview-item__remove";
			remove.setAttribute("aria-label", "Xóa video");
			remove.innerHTML = '<i class="bi bi-x-lg" aria-hidden="true"></i>';
			remove.addEventListener("click", () => {
				if (videoPreviewUrl) URL.revokeObjectURL(videoPreviewUrl);
				videoPreviewUrl = null;
				selectedVideo = null;
				syncFiles(videoInput, []);
				renderPreviews();
			});
			item.append(video, remove);
			preview.append(item);
		}
	};

	const updateCounter = (input, target, number, errorTarget) => {
		const length = input.value.trim().length;
		number.textContent = String(length);
		target.classList.toggle("is-valid", length >= 50);
		target.classList.toggle("is-invalid", length > 0 && length < 50);
		if (errorTarget && length >= 50) {
			errorTarget.textContent = "";
			errorTarget.classList.add("d-none");
		}
	};
	if (comment && counter && count) {
		comment.addEventListener("input", () => updateCounter(comment, counter, count, commentError));
		updateCounter(comment, counter, count, commentError);
	}

	root.querySelector("[data-choose-images]")?.addEventListener("click", () => imageInput.click());
	root.querySelector("[data-choose-video]")?.addEventListener("click", () => videoInput.click());
	imageInput?.addEventListener("change", () => {
		const chosen = Array.from(imageInput.files || []);
		const incoming = chosen.filter((file) => {
			const validType = /\.(jpe?g|png)$/i.test(file.name)
				&& (!file.type || ["image/jpeg", "image/png"].includes(file.type.toLowerCase()));
			const validSize = file.size < 5 * 1024 * 1024;
			if (!validType || !validSize) showToast("Chỉ nhận ảnh JPG/PNG hợp lệ, dung lượng dưới 5 MB.", "warning");
			return validType && validSize;
		});
		const merged = [...selectedImages, ...incoming];
		if (merged.length > 5) {
			showToast("Mỗi đánh giá được đính kèm tối đa 5 ảnh.", "warning");
			merged.length = 5;
		}
		selectedImages = merged;
		syncFiles(imageInput, selectedImages);
		renderPreviews();
	});
	videoInput?.addEventListener("change", () => {
		const candidate = videoInput.files?.[0] || null;
		if (candidate && (!/\.(mp4|webm)$/i.test(candidate.name)
				|| (candidate.type && !["video/mp4", "video/webm"].includes(candidate.type.toLowerCase()))
				|| candidate.size > 50 * 1024 * 1024)) {
			showToast("Chỉ nhận video MP4/WebM, dung lượng tối đa 50 MB.", "warning");
			selectedVideo = null;
			syncFiles(videoInput, []);
		} else {
			selectedVideo = candidate;
		}
		renderPreviews();
	});

	const selectRating = (container, value, hiddenInput) => {
		if (hiddenInput) hiddenInput.value = value;
		container.querySelectorAll("[data-rating], [data-edit-rating]").forEach((button) => {
			const rating = Number(button.dataset.rating || button.dataset.editRating);
			const active = rating <= value;
			button.classList.toggle("is-active", active);
			button.querySelector("i")?.classList.toggle("bi-star-fill", active);
			button.querySelector("i")?.classList.toggle("bi-star", !active);
			button.setAttribute("aria-checked", String(rating === Number(value)));
		});
	};
	const ratingInput = root.querySelector("[data-review-rating]");
	const ratingPicker = root.querySelector(".review-star-picker:not(.review-star-picker--edit)");
	ratingPicker?.querySelectorAll("[data-rating]").forEach((button) => button.addEventListener("click", () => {
		selectRating(ratingPicker, Number(button.dataset.rating), ratingInput);
		root.querySelector("[data-rating-error]")?.classList.add("d-none");
	}));

	const appendFormData = (rating, text, images, video) => {
		const data = new FormData();
		data.append("rating", String(rating));
		data.append("comment", text || "");
		images.forEach((file) => data.append("images", file));
		if (video) data.append("video", video);
		return data;
	};
	const submitForm = async (url, method, data, button) => window.App.fetchWithCsrf(url, {
		method,
		body: data,
		button,
		loadingText: "Đang tải tệp lên...",
		showErrorToast: false
	});
	const showApiError = (error, target) => {
		const fieldError = error.payload?.fieldErrors?.find((item) => item.field === "comment");
		if (fieldError && target) {
			target.textContent = fieldError.message;
			target.classList.remove("d-none");
		}
		showToast(fieldError?.message || error.message, "error");
	};

	form?.addEventListener("submit", async (event) => {
		event.preventDefault();
		const rating = Number(ratingInput.value);
		if (!rating) {
			root.querySelector("[data-rating-error]")?.classList.remove("d-none");
			return;
		}
		const text = comment.value.trim();
		if (text && text.length < 50) {
			commentError.textContent = "Nhận xét cần có ít nhất 50 ký tự hoặc để trống.";
			commentError.classList.remove("d-none");
			return;
		}
		try {
			await submitForm(`/api/reviews/${productId}`, "POST", appendFormData(rating, text, selectedImages, selectedVideo), form.querySelector("[data-review-submit]"));
			showToast("Đã gửi đánh giá của bạn.", "success");
			loadReviews(reviewUrl);
		} catch (error) {
			showApiError(error, commentError);
		}
	});

	const mediaModalElement = root.querySelector(".review-media-modal");
	const mediaModal = mediaModalElement && window.bootstrap?.Modal.getOrCreateInstance(mediaModalElement);
	const lightboxImage = root.querySelector("[data-lightbox-image]");
	const lightboxVideo = root.querySelector("[data-lightbox-video]");
	root.querySelectorAll("[data-review-image], [data-review-video-open]").forEach((button) => button.addEventListener("click", () => {
		const isVideo = button.matches("[data-review-video-open]");
		lightboxImage.classList.toggle("d-none", isVideo);
		lightboxVideo.classList.toggle("d-none", !isVideo);
		if (isVideo) lightboxVideo.src = button.dataset.mediaUrl;
		else lightboxImage.src = button.dataset.mediaUrl;
		mediaModal?.show();
	}));
	mediaModalElement?.addEventListener("hidden.bs.modal", () => {
		lightboxImage.removeAttribute("src");
		lightboxVideo.pause();
		lightboxVideo.removeAttribute("src");
		lightboxVideo.load();
	});

	const editModalElement = root.querySelector(".review-edit-modal");
	const editModal = editModalElement && window.bootstrap?.Modal.getOrCreateInstance(editModalElement);
	const editForm = root.querySelector("[data-review-edit-form]");
	let editingCard;
	let editingRating = 0;
	root.querySelectorAll("[data-edit-review]").forEach((button) => button.addEventListener("click", () => {
		editingCard = button.closest("[data-review-id]");
		editingRating = Number(editingCard.dataset.reviewRating);
		root.querySelector("[data-edit-comment]").value = editingCard.querySelector(".review-card__comment:not(.review-card__comment--empty)")?.textContent || "";
		selectRating(root.querySelector(".review-star-picker--edit"), editingRating);
		updateCounter(root.querySelector("[data-edit-comment]"), root.querySelector("[data-edit-comment-counter]"), root.querySelector("[data-edit-comment-count]"), root.querySelector("[data-edit-comment-error]"));
		editModal?.show();
	}));
	root.querySelectorAll("[data-edit-rating]").forEach((button) => button.addEventListener("click", () => {
		editingRating = Number(button.dataset.editRating);
		selectRating(root.querySelector(".review-star-picker--edit"), editingRating);
	}));
	root.querySelector("[data-edit-comment]")?.addEventListener("input", (event) => {
		updateCounter(event.target, root.querySelector("[data-edit-comment-counter]"),
			root.querySelector("[data-edit-comment-count]"), root.querySelector("[data-edit-comment-error]"));
	});
	editForm?.addEventListener("submit", async (event) => {
		event.preventDefault();
		if (!editingRating) return showToast("Vui lòng chọn số sao.", "warning");
		const text = root.querySelector("[data-edit-comment]").value.trim();
		if (text && text.length < 50) {
			const error = root.querySelector("[data-edit-comment-error]");
			error.textContent = "Nhận xét cần có ít nhất 50 ký tự hoặc để trống.";
			error.classList.remove("d-none");
			return;
		}
		try {
			await submitForm(`/api/reviews/${editingCard.dataset.reviewId}`, "PUT", appendFormData(editingRating, text, [], null), editForm.querySelector("[data-edit-submit]"));
			showToast("Đã cập nhật đánh giá.", "success");
			editModalElement.addEventListener("hidden.bs.modal", () => loadReviews(reviewUrl), { once: true });
			editModal?.hide();
		} catch (error) { showApiError(error, root.querySelector("[data-edit-comment-error]")); }
	});

	root.querySelectorAll("[data-delete-review]").forEach((button) => button.addEventListener("click", async () => {
		const card = button.closest("[data-review-id]");
		try {
			const confirmed = await window.App.showConfirmModal({
				title: "Xóa đánh giá",
				message: "Bạn có chắc muốn xóa đánh giá và các tệp đính kèm?",
				confirmText: "Xóa đánh giá",
				cancelText: "Giữ lại",
				type: "danger"
			});
			if (!confirmed) return;
			await window.App.fetchWithCsrf(`/api/reviews/${card.dataset.reviewId}`, {
				method: "DELETE", button, loadingText: "Đang xóa...", showErrorToast: false
			});
			showToast("Đã xóa đánh giá.", "success");
			loadReviews(reviewUrl);
		} catch (error) { if (error.message) showToast(error.message, "error"); }
	}));
	}
})();
