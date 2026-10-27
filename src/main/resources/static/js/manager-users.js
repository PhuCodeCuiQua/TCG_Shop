(() => {
    "use strict";
    const editForm = document.getElementById("userEditForm");
    editForm?.addEventListener("submit", () => {
        window.App.setLoading(editForm.querySelector('[type="submit"]'), true, { text: "Đang lưu…" });
    });
    window.addEventListener("pageshow", () => {
        if (editForm) window.App.setLoading(editForm.querySelector('[type="submit"]'), false);
    });

    const modalElement = document.getElementById("userStatusModal");
    if (!modalElement) return;
    const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
    const form = document.getElementById("userStatusForm");
    const reason = document.getElementById("userBlockReason");
    const submit = document.getElementById("userStatusSubmit");
    let target;
    let busy = false;
    document.querySelectorAll(".user-toggle").forEach(button => {
        button.addEventListener("click", () => {
            if (busy) return;
            target = button;
            const locking = button.dataset.status === "ACTIVE";
            reason.value = "";
            reason.required = locking;
            document.getElementById("userReasonGroup").hidden = !locking;
            document.getElementById("userStatusMessage").textContent =
                `${locking ? "Khóa" : "Mở khóa"} tài khoản ${button.dataset.name}?`;
            modal.show();
        });
    });
    modalElement.addEventListener("hide.bs.modal", event => { if (busy) event.preventDefault(); });
    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (busy || !target || !form.reportValidity()) return;
        if (reason.required && !reason.value.trim()) {
            window.App.showToast("Vui lòng nhập lý do khóa tài khoản", "error");
            return;
        }
        busy = true;
        target.disabled = true;
        try {
            const data = await window.App.fetchWithCsrf(target.dataset.url, {
                method: "POST", button: submit, loadingText: "Đang xử lý…",
                headers: { "Content-Type": "application/json", "Accept": "application/json" },
                body: JSON.stringify({ expectedStatus: target.dataset.status, reason: reason.value.trim() })
            });
            const active = data.status === "ACTIVE";
            target.dataset.status = data.status;
            target.classList.toggle("btn-danger", active);
            target.classList.toggle("btn-success", !active);
            target.querySelector(".user-toggle-label").textContent = active ? "Khóa" : "Mở khóa";
            target.querySelector(".user-lock-icon").textContent = active ? "🔒" : "🔓";
            target.setAttribute("aria-label", `${active ? "Khóa" : "Mở khóa"} ${target.dataset.name}`);
            const badge = target.closest("tr").querySelector(".user-status");
            badge.textContent = data.statusDisplayName;
            badge.classList.toggle("text-bg-success", active);
            badge.classList.toggle("text-bg-danger", !active);
            busy = false;
            modal.hide();
            window.App.showToast(active ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản", "success");
        } catch {
            // fetchWithCsrf đã hiển thị toast lỗi; giữ modal để người dùng sửa và thử lại.
        } finally {
            busy = false;
            target.disabled = false;
        }
    });
})();
