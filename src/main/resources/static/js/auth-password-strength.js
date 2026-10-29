"use strict";

document.querySelectorAll("[data-password-strength]").forEach((input) => {
	const container = input.closest("[data-strength-container]");
	const meter = container.querySelector("[data-strength-meter]");
	const progress = meter.closest('[role="progressbar"]');
	const label = container.querySelector("[data-strength-label]");
	const colors = ["bg-danger", "bg-warning", "bg-info", "bg-success"];
	const labels = ["yếu", "trung bình", "mạnh"];

	const updateStrength = () => {
		const password = input.value;
		const checks = [
			password.length >= 8,
			/[a-z]/.test(password),
			/[A-Z]/.test(password),
			/\d/.test(password),
			/[^A-Za-z0-9]/.test(password)
		];
		const score = checks.filter(Boolean).length;
		const strength = score <= 2 ? 0 : score <= 4 ? 1 : 2;

		meter.classList.remove(...colors);
		meter.classList.add(colors[strength === 0 ? 0 : strength === 1 ? 1 : 3]);
		meter.style.width = `${score * 20}%`;
		progress.setAttribute("aria-valuenow", String(score));
		label.textContent = password ? `Độ mạnh mật khẩu: ${labels[strength]}` : "Độ mạnh mật khẩu: chưa nhập";
	};

	input.addEventListener("input", updateStrength);
	updateStrength();
});
