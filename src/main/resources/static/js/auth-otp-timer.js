"use strict";

const countdown = document.querySelector("[data-otp-countdown]");
const verifyForm = document.querySelector("[data-otp-form]");
const expiredMessage = document.querySelector("[data-otp-expired]");
const resendForm = document.querySelector("[data-otp-resend-form]");
const resendButton = document.querySelector("[data-otp-resend-button]");
const resendStorageKey = "tcgOtpResendCooldownUntil";

const formatTime = (seconds) => {
	const minutes = Math.floor(seconds / 60).toString().padStart(2, "0");
	const remainder = (seconds % 60).toString().padStart(2, "0");
	return `${minutes}:${remainder}`;
};

if (countdown && verifyForm && expiredMessage) {
	let remaining = Math.max(0, Number(countdown.dataset.durationMinutes || 5) * 60);
	countdown.textContent = formatTime(remaining);

	const timer = window.setInterval(() => {
		remaining -= 1;
		countdown.textContent = formatTime(Math.max(remaining, 0));

		if (remaining <= 0) {
			window.clearInterval(timer);
			verifyForm.querySelectorAll("input:not([type='hidden']), button[type='submit']")
				.forEach((control) => { control.disabled = true; });
			expiredMessage.classList.remove("d-none");
		}
	}, 1000);
}

if (resendForm && resendButton) {
	let cooldownUntil = Number(sessionStorage.getItem(resendStorageKey) || 0);

	const updateResendButton = () => {
		const seconds = Math.ceil((cooldownUntil - Date.now()) / 1000);
		if (seconds > 0) {
			resendButton.disabled = true;
			resendButton.textContent = `Gửi lại sau ${seconds}s`;
			return;
		}

		resendButton.disabled = false;
		resendButton.textContent = "Gửi lại mã";
		sessionStorage.removeItem(resendStorageKey);
	};

	updateResendButton();
	const timer = window.setInterval(() => {
		if (cooldownUntil <= Date.now()) {
			window.clearInterval(timer);
		}
		updateResendButton();
	}, 1000);

	resendForm.addEventListener("submit", (event) => {
		if (cooldownUntil > Date.now()) {
			event.preventDefault();
			return;
		}

		cooldownUntil = Date.now() + 60_000;
		sessionStorage.setItem(resendStorageKey, String(cooldownUntil));
		updateResendButton();
	});
}
