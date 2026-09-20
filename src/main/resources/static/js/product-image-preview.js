(() => {
  const input = document.getElementById("imageFile");
  const selectedName = document.getElementById("selectedImageName");
  const currentPreview = document.getElementById("existingImagePreview");
  const newPreview = document.getElementById("newImagePreview");
  const error = document.getElementById("imageError");
  if (!input || !selectedName || !newPreview || !error) return;

  const maxBytes = 20 * 1024 * 1024;
  let objectUrl = null;

  function showCurrentImage() {
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = null;
    newPreview.hidden = true;
    newPreview.removeAttribute("src");
    if (currentPreview) currentPreview.hidden = false;
  }

  input.addEventListener("change", () => {
    error.hidden = true;
    error.textContent = "";
    showCurrentImage();
    const file = input.files && input.files[0];
    if (!file) {
      selectedName.textContent = "Chưa chọn ảnh mới";
      return;
    }
    if (file.size > maxBytes) {
      selectedName.textContent = "Chưa chọn ảnh mới";
      error.textContent = "Ảnh vượt quá 20 MB. Vui lòng chọn ảnh nhỏ hơn.";
      error.hidden = false;
      input.value = "";
      return;
    }
    if (file.size === 0) {
      selectedName.textContent = "Chưa chọn ảnh mới";
      error.textContent = "Ảnh được chọn đang trống. Vui lòng chọn ảnh khác.";
      error.hidden = false;
      input.value = "";
      return;
    }
    selectedName.textContent = file.name;
    objectUrl = URL.createObjectURL(file);
    newPreview.src = objectUrl;
    newPreview.hidden = false;
    if (currentPreview) currentPreview.hidden = true;
  });

  input.form.addEventListener("submit", (event) => {
    const file = input.files && input.files[0];
    if (file && (file.size > maxBytes || file.size === 0)) {
      event.preventDefault();
      input.dispatchEvent(new Event("change"));
    }
  });

  window.addEventListener("pagehide", showCurrentImage, { once: true });
})();