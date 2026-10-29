"use strict";

(() => {
	const bell = document.getElementById("notificationBell");
	const badge = document.getElementById("notificationBellBadge");
	const list = document.getElementById("notificationBellList");
	if (!bell || !badge || !list) return;

	const notifications = [];
	let unreadCount = 0;
	let stompClient;
	let reconnectTimer;
	let stopped = false;

	const setUnreadCount = (count) => {
		unreadCount = Math.max(0, Number(count) || 0);
		badge.textContent = unreadCount > 99 ? "99+" : String(unreadCount);
		badge.classList.toggle("d-none", unreadCount === 0);
	};

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
			dateStyle: "short",
			timeStyle: "short"
		}).format(date);
	};

	const renderNotifications = () => {
		list.replaceChildren();
		if (notifications.length === 0) {
			const empty = document.createElement("p");
			empty.className = "notification-bell-empty mb-0";
			empty.textContent = "Bạn chưa có thông báo nào.";
			list.append(empty);
			return;
		}

		notifications.slice(0, 5).forEach((notification) => {
			const link = document.createElement("a");
			link.className = `notification-bell-item${notification.read ? "" : " is-unread"}`;
			link.href = safeLink(notification.link);
			const title = document.createElement("strong");
			title.className = "notification-bell-item-title";
			title.textContent = notification.title || "Thông báo";
			const content = document.createElement("span");
			content.className = "notification-bell-item-content";
			content.textContent = notification.content || "";
			link.append(title, content);
			const time = formatDate(notification.createdAt);
			if (time) {
				const timestamp = document.createElement("time");
				timestamp.className = "notification-bell-item-time";
				timestamp.dateTime = notification.createdAt;
				timestamp.textContent = time;
				link.append(timestamp);
			}
			link.addEventListener("click", async (event) => {
				if (!notification.read && notification.id) {
					event.preventDefault();
					try {
						await window.App.fetchWithCsrf(`/api/notifications/${encodeURIComponent(notification.id)}/read`, {
							method: "PATCH",
							showErrorToast: true
						});
						notification.read = true;
						setUnreadCount(unreadCount - 1);
					} catch {
						// Không chặn điều hướng tới link của thông báo nếu thao tác đánh dấu đã đọc lỗi.
					}
					window.location.assign(link.href);
				}
			});
			list.append(link);
		});
	};

	const addNotification = (notification, showToast) => {
		if (!notification || typeof notification !== "object") return;
		if (notification.id && notifications.some((item) => item.id === notification.id)) return;
		notifications.unshift(notification);
		if (!notification.read) setUnreadCount(unreadCount + 1);
		renderNotifications();
		bell.parentElement.classList.remove("is-ringing");
		void bell.parentElement.offsetWidth;
		bell.parentElement.classList.add("is-ringing");
		if (showToast && window.App?.showToast) {
			window.App.showToast(notification.content || notification.title || "Bạn có thông báo mới", "success");
		}
	};

	const loadInitialNotifications = async () => {
		try {
			const [countResponse, pageResponse] = await Promise.all([
				window.App.fetchWithCsrf("/api/notifications/unread-count", { showErrorToast: false }),
				window.App.fetchWithCsrf("/api/notifications?page=0&size=5", { showErrorToast: false })
			]);
			setUnreadCount(countResponse.unreadCount);
			(pageResponse.content || []).forEach((notification) => notifications.push(notification));
			renderNotifications();
		} catch {
			list.replaceChildren();
			const unavailable = document.createElement("p");
			unavailable.className = "notification-bell-empty mb-0";
			unavailable.textContent = "Chưa thể tải thông báo.";
			list.append(unavailable);
		}
	};

	const connect = () => {
		if (stopped || stompClient) return;
		if (!window.SockJS || !window.Stomp) {
			list.dataset.connectionError = "true";
			return;
		}
		const client = window.Stomp.over(new window.SockJS("/ws"));
		stompClient = client;
		client.debug = null;
		client.connect({}, () => {
			if (stopped || stompClient !== client) return;
			client.subscribe("/user/queue/notifications", (frame) => {
				try {
					addNotification(JSON.parse(frame.body), true);
				} catch {
					window.App?.showToast("Không thể đọc thông báo mới.", "error");
				}
			});
		}, () => {
			if (stompClient === client) stompClient = null;
			if (!stopped && !reconnectTimer) {
				reconnectTimer = window.setTimeout(() => {
					reconnectTimer = null;
					connect();
				}, 5000);
			}
		});
	};

	const start = async () => {
		await loadInitialNotifications();
		connect();
	};

	window.addEventListener("pagehide", () => {
		stopped = true;
		window.clearTimeout(reconnectTimer);
		if (stompClient?.connected) stompClient.disconnect();
		stompClient = null;
	}, { once: true });

	start();
})();
