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
        confirmed = window.confirm("Bạn có chắc muốn xóa địa chỉ này không?");
      }
      if (confirmed) {
        form.dataset.confirmed = "true";
        form.requestSubmit();
      }
    });
  });
});
