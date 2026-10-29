"use strict";

(() => {
    const form = document.getElementById("adminSettingsForm");
    if (!form) return;
    const input = document.getElementById("websiteLogo");
    const preview = document.getElementById("logoPreview");
    const placeholder = document.getElementById("logoPlaceholder");
    const logoError = document.getElementById("logoError");
    const initialLogo = preview.getAttribute("src");
    let previewUrl;
    const clearPreview = () => {
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = null;
        preview.hidden = !initialLogo;
        if (initialLogo) preview.src = initialLogo;
        else preview.removeAttribute("src");
        placeholder.hidden = !!initialLogo;
    };
    input.addEventListener("change", () => {
        clearPreview();
        logoError.textContent = "";
        input.setCustomValidity("");
        const file = input.files?.[0];
        if (!file) return;
        if (!/\.(jpe?g|png)$/i.test(file.name) || !["image/jpeg", "image/png"].includes(file.type)
                || file.size >= 5 * 1024 * 1024) {
            logoError.textContent = "Chọn ảnh JPG/PNG hợp lệ, dung lượng dưới 5 MB.";
            input.setCustomValidity(logoError.textContent);
            return;
        }
        previewUrl = URL.createObjectURL(file);
        preview.onerror = () => {
            clearPreview();
            logoError.textContent = "Không thể đọc ảnh đã chọn.";
            input.setCustomValidity(logoError.textContent);
        };
        preview.src = previewUrl;
        preview.hidden = false;
        placeholder.hidden = true;
    });
    form.addEventListener("submit", (event) => {
        const name = document.getElementById("websiteName");
        name.setCustomValidity(name.value.trim() ? "" : "Vui lòng nhập tên website.");
        form.classList.add("was-validated");
        if (!form.checkValidity()) {
            event.preventDefault();
            form.querySelector(":invalid")?.focus();
            return;
        }
        window.App?.setLoading(form.querySelector('button[type="submit"]'), true, { text: "Đang lưu..." });
    });
    form.addEventListener("reset", () => {
        clearPreview();
        logoError.textContent = "";
        form.classList.remove("was-validated");
        for (const field of form.querySelectorAll("input")) field.setCustomValidity("");
    });
    window.addEventListener("pagehide", clearPreview, { once: true });
})();
