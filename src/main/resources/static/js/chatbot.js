"use strict";

(() => {
	const widget = document.querySelector("[data-chatbot-widget]");
	if (!widget) return;

	const toggle = widget.querySelector("[data-chatbot-toggle]");
	const close = widget.querySelector("[data-chatbot-close]");
	const panel = widget.querySelector("[data-chatbot-panel]");
	const messageContainer = widget.querySelector("[data-chatbot-messages]");
	const typing = widget.querySelector("[data-chatbot-typing]");
	const form = widget.querySelector("[data-chatbot-form]");
	const input = form.elements.question;
	const sendButton = widget.querySelector("[data-chatbot-send]");
	const clearButton = widget.querySelector("[data-chatbot-clear]");
	const chatUser = (widget.dataset.chatUser || "").trim();
	if (!chatUser || chatUser.toLowerCase() === "guest_chat") {
		widget.remove();
		return;
	}

	const authPage = ["/auth/login", "/auth/register"].includes(window.location.pathname);
	widget.hidden = authPage;
	const HISTORY_KEY = `tcg_chat_history_${encodeURIComponent(chatUser.toLowerCase())}`;
	const greeting = {
		sender: "bot",
		text: "Xin chào! Tôi là trợ lý ảo của TCG Shop. Bạn cần hỗ trợ điều gì?"
	};
	const messages = [];
	let pending = false;
	let loggingOut = false;

	const renderMessage = ({ sender, text, html, imageUrl }) => {
		const item = document.createElement("div");
		const isUser = sender === "user";
		item.className = `chatbot-message ${isUser ? "chatbot-message--user" : "chatbot-message--bot"}`;
		if (!isUser && typeof html === "string" && html.trim()) {
			item.append(sanitizeBotHtml(html, imageUrl));
		} else {
			const paragraph = document.createElement("p");
			paragraph.textContent = typeof text === "string" ? text : "";
			item.append(paragraph);
		}
		messageContainer.append(item);
		messageContainer.scrollTop = messageContainer.scrollHeight;
	};

	const persistHistory = () => {
		try {
			localStorage.setItem(HISTORY_KEY, JSON.stringify(messages));
		} catch (error) {
			// Storage có thể bị tắt hoặc đầy; chat vẫn hoạt động trong trang hiện tại.
		}
	};

	const appendMessage = (text, isUser, html = null, imageUrl = null) => {
		if (loggingOut) return;
		const message = {
			sender: isUser ? "user" : "bot",
			text: typeof text === "string" ? text : "",
			html: !isUser && typeof html === "string" ? html : null,
			imageUrl: !isUser && typeof imageUrl === "string" ? imageUrl : null
		};
		messages.push(message);
		renderMessage(message);
		persistHistory();
	};

	const sanitizeImageUrl = (value) => {
		if (typeof value !== "string") return null;
		const url = value.trim();
		return url.startsWith("https://") || url.startsWith("http://")
			|| (url.startsWith("/") && !url.startsWith("//")) ? url : null;
	};

	const sanitizeBotHtml = (html, imageUrl) => {
		const source = new DOMParser().parseFromString(html, "text/html").body;
		const responseImageUrl = sanitizeImageUrl(imageUrl);
		const allowedTags = new Set(["A", "DIV", "IMG", "STRONG", "P", "UL", "LI", "SPAN"]);
		const allowedClasses = new Set(["chat-card", "chat-card-link", "chat-card-img", "chat-card-info", "chat-card-name", "chat-shop-list", "chat-best-advice", "chat-price-results"]);
		const copySafe = (node) => {
			if (node.nodeType === Node.TEXT_NODE) return document.createTextNode(node.textContent || "");
			if (node.nodeType !== Node.ELEMENT_NODE || !allowedTags.has(node.tagName)) return document.createDocumentFragment();
			const clean = document.createElement(node.tagName.toLowerCase());
			if (node.tagName === "A") {
				const href = node.getAttribute("href") || "";
				if (!/^\/products\/\d+$/.test(href) && !["/faq", "/contact"].includes(href)) return clean;
				clean.href = href;
				clean.setAttribute("aria-label", (node.getAttribute("aria-label") || "Xem sản phẩm").slice(0, 160));
			}
			if (node.tagName === "IMG") {
				const rawSrc = sanitizeImageUrl(node.getAttribute("src"));
				clean.src = responseImageUrl || rawSrc || "/img/placeholder-card.svg";
				clean.alt = (node.getAttribute("alt") || "Ảnh sản phẩm").slice(0, 160);
				clean.loading = "lazy";
				clean.addEventListener("error", () => {
					if (clean.dataset.fallbackApplied) return;
					clean.dataset.fallbackApplied = "true";
					clean.src = "/img/placeholder-card.svg";
				}, { once: true });
			}
			const className = node.getAttribute("class");
			if (className && allowedClasses.has(className)) clean.className = className;
			node.childNodes.forEach((child) => clean.append(copySafe(child)));
			return clean;
		};
		const fragment = document.createDocumentFragment();
		source.childNodes.forEach((node) => fragment.append(copySafe(node)));
		return fragment;
	};

	const restoreHistory = () => {
		messages.length = 0;
		try {
			const stored = localStorage.getItem(HISTORY_KEY);
			if (stored) {
				const parsed = JSON.parse(stored);
				if (Array.isArray(parsed)) {
					messages.push(...parsed.filter((message) => message
						&& (message.sender === "user" || message.sender === "bot")
						&& typeof message.text === "string")
						.map((message) => ({
							sender: message.sender,
							text: message.text,
							html: message.sender === "bot" && typeof message.html === "string" ? message.html : null,
							imageUrl: message.sender === "bot" && typeof message.imageUrl === "string" ? message.imageUrl : null
						})));
				}
			}
		} catch (error) {
			messages.length = 0;
		}

		if (messages.length === 0) messages.push(greeting);
		messageContainer.replaceChildren();
		messages.forEach(renderMessage);
	};

	const clearHistory = () => {
		try {
			localStorage.removeItem(HISTORY_KEY);
		} catch (error) {
			// Xóa giao diện hiện tại vẫn được nếu storage không khả dụng.
		}
		messages.length = 0;
		messages.push(greeting);
		messageContainer.replaceChildren();
		renderMessage(greeting);
	};

	const setOpen = (open) => {
		panel.hidden = !open;
		toggle.setAttribute("aria-expanded", String(open));
		if (open) input.focus();
	};

	document.addEventListener("submit", (event) => {
		const logoutForm = event.target;
		if (!(logoutForm instanceof HTMLFormElement) || !logoutForm.matches("form.js-logout-form")) return;
		loggingOut = true;
		widget.hidden = true;
		panel.hidden = true;
		toggle.setAttribute("aria-expanded", "false");
		typing.hidden = true;
		input.value = "";
		messages.length = 0;
		messageContainer.replaceChildren();
	}, true);

	if (authPage) return;

	toggle.addEventListener("click", () => setOpen(panel.hidden));
	close.addEventListener("click", () => setOpen(false));
	clearButton.addEventListener("click", clearHistory);
	const initializeChat = () => restoreHistory();
	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", initializeChat, { once: true });
	} else {
		initializeChat();
	}

	form.addEventListener("submit", async (event) => {
		event.preventDefault();
		const question = input.value.trim();
		if (!question || pending) return;

		appendMessage(question, true);
		input.value = "";
		pending = true;
		sendButton.disabled = true;
		typing.hidden = false;
		messageContainer.scrollTop = messageContainer.scrollHeight;

		try {
			const response = await window.App.fetchWithCsrf("/api/chatbot/ask", {
				method: "POST",
				headers: { Accept: "application/json", "Content-Type": "application/json" },
				body: JSON.stringify({ question }),
				showErrorToast: false
			});
			appendMessage(response.reply || "Xin lỗi, tôi chưa có câu trả lời cho câu hỏi này.", false,
				response.html, response.imageUrl);
		} catch (error) {
			appendMessage(error instanceof Error ? error.message : "Chưa thể kết nối với trợ lý. Vui lòng thử lại.", false);
		} finally {
			pending = false;
			if (!loggingOut) {
				typing.hidden = true;
				sendButton.disabled = false;
				input.focus();
			}
		}
	});
})();
