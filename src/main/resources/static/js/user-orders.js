"use strict";

document.querySelectorAll("[data-reorder-form]").forEach((form) => {
    let submitting = false;
    form.addEventListener("submit", (event) => {
        if (submitting) { event.preventDefault(); return; }
        submitting = true;
        const button = form.querySelector('button[type="submit"]');
        if (window.App?.setLoading) window.App.setLoading(button, true, { text: "Đang thêm..." });
        else button.disabled = true;
    });
});
