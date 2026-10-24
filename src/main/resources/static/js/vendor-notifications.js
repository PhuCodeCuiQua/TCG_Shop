"use strict";

(() => {
	const bell = document.getElementById("vendorNotificationBell");
	const badge = document.getElementById("vendorNotificationBadge");
	const list = document.getElementById("vendorNotificationList");
	if (!bell || !badge || !list) return;

	const notifications = [];
	let unreadCount = 0;
	let stompClient;

	const setUnreadCount = (count) => {
		unreadCount = Math.max(0, Number(count) || 0);
		badge.textContent = unreadCount > 99 ? "99+" : String(unreadCount);
		badge.classList.toggle("d-none", unreadCount === 0);
	};

	const safeLink = (link) => typeof link === "string" && link.startsWith("/") && !link.startsWith("//")
		? link
		: "/vendor/orders";

	const renderNotifications = () => {
		list.replaceChildren();
		if (notifications.length === 0) {
			const empty = document.createElement("p");
			empty.className = "vendor-notification-empty mb-0";
			empty.textContent = "Bạn chưa có thông báo nào.";
			list.append(empty);
			return;
		}

		notifications.slice(0, 8).forEach((notification) => {
			const link = document.createElement("a");
			link.className = `vendor-notification-item${notification.read ? "" : " is-unread"}`;
			link.href = safeLink(notification.link);
			const title = document.createElement("strong");
			title.className = "d-block";
			title.textContent = notification.title || "Thông báo";
			const content = document.createElement("span");
			content.className = "d-block";
			content.textContent = notification.content || "";
			link.append(title, content);
			link.addEventListener("click", async (event) => {
				if (!notification.read && notification.id && window.App?.fetchWithCsrf) {
					event.preventDefault();
					try {
						await window.App.fetchWithCsrf(`/api/notifications/${encodeURIComponent(notification.id)}/read`, {
							method: "PATCH",
							showErrorToast: false
						});
						notification.read = true;
						setUnreadCount(unreadCount - 1);
					} catch {
						// Điều hướng vẫn được ưu tiên nếu endpoint đánh dấu đã đọc lỗi.
					}
					window.location.assign(link.href);
				}
			});
			list.append(link);
		});
	};

	const addNotification = (notification, showToast) => {
		if (!notification || typeof notification !== "object") return;
		notifications.unshift(notification);
		if (!notification.read) setUnreadCount(unreadCount + 1);
		renderNotifications();
		bell.classList.remove("is-ringing");
		void bell.offsetWidth;
		bell.classList.add("is-ringing");
		if (showToast && window.App?.showToast) {
			window.App.showToast(notification.content || notification.title || "Bạn có thông báo mới", "success");
		}
	};

	const loadInitialNotifications = async () => {
		try {
			const [countResponse, pageResponse] = await Promise.all([
				window.App.fetchWithCsrf("/api/notifications/unread-count", { showErrorToast: false }),
				window.App.fetchWithCsrf("/api/notifications?page=0&size=8", { showErrorToast: false })
			]);
			setUnreadCount(countResponse.unreadCount);
			(pageResponse.content || []).forEach((notification) => notifications.push(notification));
			renderNotifications();
		} catch {
			list.replaceChildren();
			const unavailable = document.createElement("p");
			unavailable.className = "vendor-notification-empty mb-0";
			unavailable.textContent = "Chưa thể tải thông báo.";
			list.append(unavailable);
		}
	};

	const connect = () => {
		if (!window.SockJS || !window.Stomp) {
			window.App?.showToast("Không tải được thư viện kết nối thông báo.", "error");
			return;
		}
		stompClient = window.Stomp.over(new window.SockJS("/ws"));
		stompClient.debug = null;
		stompClient.connect({}, () => {
			stompClient.subscribe("/user/queue/notifications", (frame) => {
				try {
					addNotification(JSON.parse(frame.body), true);
				} catch {
					window.App?.showToast("Không thể đọc thông báo mới.", "error");
				}
			});
		}, () => {
			window.setTimeout(connect, 5000);
		});
	};

	loadInitialNotifications();
	connect();
})();
