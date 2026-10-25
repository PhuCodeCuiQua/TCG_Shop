document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("form[data-address-delete]").forEach((form) => {
    form.addEventListener("submit", async (event) => {
      if (form.dataset.confirmed === "true") return;
      event.preventDefault();
      let confirmed;
      if (typeof window.App?.showConfirmModal === "function") {
        confirmed = await window.App.showConfirmModal({
          message: "Bạn có chắc muốn xóa địa chỉ này không?",
          confirmText: "Xóa",
          type: "danger"
        });
      } else {
        window.App?.showToast("Không thể mở hộp thoại xác nhận. Vui lòng tải lại trang.", "error");
        confirmed = false;
      }
      if (confirmed) {
        form.dataset.confirmed = "true";
        window.App?.setLoading(form.querySelector('button[type="submit"]'), true, { text: "Đang xóa..." });
        form.requestSubmit();
      }
    });
  });
});
