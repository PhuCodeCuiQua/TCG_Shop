"use strict";

(() => {
    const host = document.getElementById("adminLogs");
    if (!host) return;
    const search = document.getElementById("logSearch");
    const rows = document.getElementById("logRows");
    const pagination = document.getElementById("logPagination");
    const summary = document.getElementById("logSummary");
    const statuses = {
        success: { label: "Thành công", className: "admin-log-success" },
        error: { label: "Thất bại", className: "admin-log-error" },
        warning: { label: "Cảnh báo", className: "admin-log-warning" }
    };
    // Dữ liệu mẫu cố định cho giao diện demo, không phải nhật ký hoặc tài khoản thật.
    const entries = [
        ["26/10/2026 14:30", "Admin demo", "Cập nhật thông tin website", "success"],
        ["26/10/2026 14:12", "Manager demo", "Duyệt hồ sơ cửa hàng", "success"],
        ["26/10/2026 13:58", "Vendor demo", "Nhập sai mật khẩu đăng nhập", "error"],
        ["26/10/2026 13:42", "Hệ thống demo", "Phát hiện sản phẩm sắp hết hàng", "warning"],
        ["26/10/2026 13:20", "Shipper demo", "Xác nhận giao hàng thành công", "success"],
        ["26/10/2026 12:55", "Admin demo", "Tạo mã giảm giá hệ thống", "success"],
        ["26/10/2026 12:40", "Người mua demo", "Áp dụng mã giảm giá hết hạn", "warning"],
        ["26/10/2026 12:15", "Vendor demo", "Đăng sản phẩm mới", "success"],
        ["26/10/2026 11:50", "Hệ thống demo", "Gửi email không thành công", "error"],
        ["26/10/2026 11:25", "Manager demo", "Phân công nhân viên giao hàng", "success"],
        ["26/10/2026 10:40", "Admin demo", "Cập nhật hoa hồng mặc định", "success"],
        ["26/10/2026 10:10", "Hệ thống demo", "Cảnh báo tồn kho thấp", "warning"],
        ["25/10/2026 17:15", "Người mua demo", "Đặt hàng COD", "success"],
        ["25/10/2026 16:40", "Shipper demo", "Giao hàng không thành công", "error"],
        ["25/10/2026 16:00", "Manager demo", "Xử lý khiếu nại của khách hàng", "success"]
    ];
    const pageSize = 6;
    const initialUrl = new URL(window.location.href);
    search.value = (initialUrl.searchParams.get("q") || "").slice(0, 150);
    const initialPage = Number(initialUrl.searchParams.get("page") || 0);
    let page = Number.isSafeInteger(initialPage) && initialPage >= 0 ? initialPage : 0;
    const normalize = (value) => value.toLocaleLowerCase("vi").normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d");
    const node = (tag, className, text) => {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (text != null) element.textContent = text;
        return element;
    };
    function render() {
        const keyword = normalize(search.value.trim());
        const filtered = entries.filter((entry) => normalize([...entry.slice(0, 3), statuses[entry[3]].label].join(" ")).includes(keyword));
        const pages = Math.ceil(filtered.length / pageSize);
        page = Math.max(0, Math.min(page, pages - 1));
        const url = new URL(window.location.href);
        if (search.value.trim()) url.searchParams.set("q", search.value.trim());
        else url.searchParams.delete("q");
        if (page) url.searchParams.set("page", String(page));
        else url.searchParams.delete("page");
        window.history.replaceState(null, "", `${url.pathname}${url.search}${url.hash}`);
        rows.replaceChildren();
        for (const entry of filtered.slice(page * pageSize, (page + 1) * pageSize)) {
            const row = node("tr");
            row.append(node("td", "text-nowrap", entry[0]), node("td", "fw-semibold", entry[1]), node("td", "", entry[2]));
            const status = node("td");
            status.append(node("span", `admin-log-status ${statuses[entry[3]].className}`, statuses[entry[3]].label));
            row.append(status);
            rows.append(row);
        }
        if (!filtered.length) {
            const cell = node("td", "text-center py-5", "Không tìm thấy hoạt động phù hợp.");
            cell.colSpan = 4;
            const row = node("tr"); row.append(cell); rows.append(row);
        }
        summary.textContent = filtered.length
            ? `Hiển thị ${page * pageSize + 1}–${Math.min((page + 1) * pageSize, filtered.length)} / ${filtered.length} hoạt động minh họa`
            : "0 hoạt động phù hợp";
        pagination.replaceChildren();
        pagination.parentElement.hidden = pages <= 1;
        if (pages <= 1) return;
        const addPage = (label, target, disabled, active = false) => {
            const item = node("li", `page-item${active ? " active" : ""}`);
            const button = node("button", "page-link", label);
            button.type = "button";
            button.disabled = disabled;
            if (active) button.setAttribute("aria-current", "page");
            button.addEventListener("click", () => { page = target; render(); });
            item.append(button); pagination.append(item);
        };
        addPage("Trước", page - 1, page === 0);
        for (let index = 0; index < pages; index++) addPage(String(index + 1), index, false, index === page);
        addPage("Sau", page + 1, page === pages - 1);
    }
    search.addEventListener("input", () => { page = 0; render(); });
    render();
})();
