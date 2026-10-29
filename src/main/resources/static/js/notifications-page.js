"use strict";

(() => {
	const root = document.getElementById("allNotifications");
	const list = document.getElementById("allNotificationsList");
	const pagination = document.getElementById("allNotificationsPagination");
	if (!root || !list || !pagination) return;

	const pageSize = Number(root.dataset.pageSize) || 10;
	const requestedPage = Number(new URLSearchParams(window.location.search).get("page"));
	let currentPage = Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;

	const safeLink = (link) => {
		if (typeof link !== "string" || !link.startsWith("/") || link.startsWith("//")) {
			return "/notifications";
		}
		try {
			const url = new URL(link, window.location.origin);
			return url.origin === window.location.origin ? `${url.pathname}${url.search}${url.hash}` : "/notifications";
		} catch {
			return "/notifications";
		}
	};

	const formatDate = (value) => {
		if (!value) return "";
		const date = new Date(value);
		return Number.isNaN(date.getTime()) ? "" : new Intl.DateTimeFormat("vi-VN", {
			dateStyle: "medium",
			timeStyle: "short"
		}).format(date);
	};

	const showMessage = (message, className = "text-muted") => {
		list.replaceChildren();
		const empty = document.createElement("div");
		empty.className = `list-group-item py-4 text-center ${className}`;
		empty.textContent = message;
		list.append(empty);
	};

	const markAsReadAndFollow = async (event, notification, anchor) => {
		if (notification.read) return;
		event.preventDefault();
		try {
			await window.App.fetchWithCsrf(`/api/notifications/${encodeURIComponent(notification.id)}/read`, {
				method: "PATCH"
			});
			window.location.assign(anchor.href);
		} catch {
			// Giữ người dùng ở danh sách nếu không thể xác nhận trạng thái đã đọc.
		}
	};

	const renderNotifications = (pageResponse) => {
		list.replaceChildren();
		if (!pageResponse.content?.length) {
			showMessage("Bạn chưa có thông báo nào.");
		} else {
			pageResponse.content.forEach((notification) => {
				const anchor = document.createElement("a");
				anchor.className = `list-group-item list-group-item-action py-3 all-notifications-item${notification.read ? "" : " is-unread"}`;
				anchor.href = safeLink(notification.link);
				anchor.addEventListener("click", (event) => markAsReadAndFollow(event, notification, anchor));

				const heading = document.createElement("div");
				heading.className = "d-flex justify-content-between align-items-start gap-2";
				const title = document.createElement("strong");
				title.textContent = notification.title || "Thông báo";
				const status = document.createElement("span");
				status.className = `badge ${notification.read ? "text-bg-light" : "text-bg-primary"}`;
				status.textContent = notification.read ? "Đã đọc" : "Chưa đọc";
				heading.append(title, status);

				const content = document.createElement("p");
				content.className = "mb-1 mt-2";
				content.textContent = notification.content || "";
				anchor.append(heading, content);

				const timestamp = formatDate(notification.createdAt);
				if (timestamp) {
					const time = document.createElement("time");
					time.className = "all-notifications-time";
					time.dateTime = notification.createdAt;
					time.textContent = timestamp;
					anchor.append(time);
				}
				list.append(anchor);
			});
		}
		renderPagination(pageResponse);
	};

	const renderPagination = (pageResponse) => {
		pagination.replaceChildren();
		if (pageResponse.totalPages <= 1) return;

		const addEllipsis = () => {
			const item = document.createElement("li");
			item.className = "page-item disabled";
			const label = document.createElement("span");
			label.className = "page-link";
			label.setAttribute("aria-hidden", "true");
			label.textContent = "…";
			item.append(label);
			pagination.append(item);
		};

		const addPageButton = (label, page, disabled, active = false, ariaLabel = label) => {
			const item = document.createElement("li");
			item.className = `page-item${disabled ? " disabled" : ""}${active ? " active" : ""}`;
			const button = document.createElement("button");
			button.className = "page-link";
			button.type = "button";
			button.textContent = label;
			button.setAttribute("aria-label", ariaLabel);
			if (active) button.setAttribute("aria-current", "page");
			button.disabled = disabled;
			button.addEventListener("click", () => loadPage(page));
			item.append(button);
			pagination.append(item);
		};

		addPageButton("Trước", currentPage - 1, pageResponse.first, false, "Trang trước");
		const startPage = Math.max(0, Math.min(currentPage - 2, pageResponse.totalPages - 5));
		const endPage = Math.min(pageResponse.totalPages - 1, startPage + 4);
		if (startPage > 0) {
			addPageButton("1", 0, false, currentPage === 0, "Trang 1");
			if (startPage > 1) addEllipsis();
		}
		for (let page = startPage; page <= endPage; page += 1) {
			addPageButton(String(page + 1), page, false, page === currentPage, `Trang ${page + 1}`);
		}
		if (endPage < pageResponse.totalPages - 1) {
			if (endPage < pageResponse.totalPages - 2) addEllipsis();
			addPageButton(
					String(pageResponse.totalPages),
					pageResponse.totalPages - 1,
					false,
					currentPage === pageResponse.totalPages - 1,
					`Trang ${pageResponse.totalPages}`);
		}
		addPageButton("Sau", currentPage + 1, pageResponse.last, false, "Trang sau");
	};

	const loadPage = async (page) => {
		currentPage = Math.max(0, page);
		pagination.replaceChildren();
		showMessage("Đang tải thông báo...");
		try {
			const response = await window.App.fetchWithCsrf(
				`/api/notifications?page=${currentPage}&size=${pageSize}`,
				{ showErrorToast: false });
			currentPage = response.page;
			const currentUrl = new URL(window.location.href);
			currentUrl.searchParams.set("page", String(currentPage));
			window.history.replaceState(null, "", currentUrl);
			renderNotifications(response);
		} catch (error) {
			showMessage("Không thể tải thông báo. Vui lòng thử lại.", "text-danger");
			window.App.showToast(error instanceof Error ? error.message : "Không thể tải thông báo.", "error");
		}
	};

	loadPage(currentPage);
})();
