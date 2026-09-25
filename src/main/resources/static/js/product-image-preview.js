(() => {
  const input = document.getElementById("imageFiles");
  const selectedName = document.getElementById("selectedImageName");
  const previews = document.getElementById("newImagePreviews");
  const error = document.getElementById("imageError");
  if (!input || !selectedName || !previews || !error) return;

  const maxBytes = 5 * 1024 * 1024;
  const maxImages = 8;
  const allowed = new Set(["image/png", "image/jpeg", "image/webp"]);
  let urls = [];

  function clearPreviews() {
    urls.forEach(URL.revokeObjectURL);
    urls = [];
    previews.replaceChildren();
  }

  input.addEventListener("change", () => {
    clearPreviews();
    error.hidden = true;
    error.textContent = "";
    const files = Array.from(input.files || []);
    const invalid = files.find(file => !allowed.has(file.type) || file.size === 0 || file.size > maxBytes);
    if (files.length > maxImages || invalid) {
      error.textContent = files.length > maxImages
        ? "Chỉ được chọn tối đa 8 hình ảnh."
        : "Mỗi ảnh phải là PNG, JPEG hoặc WebP hợp lệ và không vượt quá 5 MB.";
      error.hidden = false;
      input.value = "";
      selectedName.textContent = "Chưa chọn ảnh mới";
      return;
    }
    selectedName.textContent = files.length ? files.length + " ảnh đã chọn" : "Chưa chọn ảnh mới";
    files.forEach(file => {
      const url = URL.createObjectURL(file);
      urls.push(url);
      const image = document.createElement("img");
      image.src = url;
      image.alt = file.name;
      image.className = "img-thumbnail";
      image.style.cssText = "width:120px;height:120px;object-fit:cover";
      previews.appendChild(image);
    });
  });

  window.addEventListener("pagehide", clearPreviews, { once: true });
})();
