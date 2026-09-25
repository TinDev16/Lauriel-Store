(() => {
  const input = document.getElementById("imageFiles");
  const selectedName = document.getElementById("selectedImageName");
  const previews = document.getElementById("newImagePreviews");
  const error = document.getElementById("imageError");
  const sizeOptions = document.getElementById("imageSizeOptions");
  if (!input || !selectedName || !previews || !error || !sizeOptions) return;

  const maxBytes = 5 * 1024 * 1024;
  const maxImages = 8;
  const existingCount = Number(input.dataset.existingCount || 0);
  const allowed = new Set(["image/png", "image/jpeg", "image/webp"]);
  const selectedFiles = [];
  const assignedSizes = new Map();
  let urls = [];

  const keyOf = file => `${file.name}:${file.size}:${file.lastModified}`;

  function showError(message) {
    error.textContent = message;
    error.hidden = !message;
  }

  function releaseUrls() {
    urls.forEach(URL.revokeObjectURL);
    urls = [];
  }

  function syncInput() {
    const transfer = new DataTransfer();
    selectedFiles.forEach(file => transfer.items.add(file));
    input.files = transfer.files;
  }

  function render() {
    releaseUrls();
    previews.replaceChildren();
    syncInput();
    selectedName.textContent = selectedFiles.length
      ? `${selectedFiles.length} ảnh đã chọn`
      : "Chưa chọn ảnh mới";

    selectedFiles.forEach((file, index) => {
      const key = keyOf(file);
      const url = URL.createObjectURL(file);
      urls.push(url);

      const column = document.createElement("div");
      column.className = "col-12 col-sm-6 col-lg-4";
      const card = document.createElement("div");
      card.className = "border rounded p-2 h-100 bg-light";

      const image = document.createElement("img");
      image.src = url;
      image.alt = file.name;
      image.className = "img-fluid rounded w-100";
      image.style.cssText = "height:150px;object-fit:cover";

      const name = document.createElement("div");
      name.className = "small text-truncate mt-2";
      name.title = file.name;
      name.textContent = file.name;

      const select = document.createElement("select");
      select.name = "imageSizeIds";
      select.className = "form-select form-select-sm mt-2";
      select.appendChild(sizeOptions.content.cloneNode(true));
      select.value = assignedSizes.get(key) || "";
      select.setAttribute("aria-label", `Size cho ảnh ${file.name}`);
      select.addEventListener("change", () => assignedSizes.set(key, select.value));

      const remove = document.createElement("button");
      remove.type = "button";
      remove.className = "btn btn-outline-danger btn-sm mt-2 w-100";
      remove.textContent = "Bỏ ảnh";
      remove.addEventListener("click", () => {
        selectedFiles.splice(index, 1);
        assignedSizes.delete(key);
        showError("");
        render();
      });

      card.append(image, name, select, remove);
      column.appendChild(card);
      previews.appendChild(column);
    });
  }

  input.addEventListener("change", () => {
    const incoming = Array.from(input.files || []);
    const invalid = incoming.find(file => !allowed.has(file.type) || file.size === 0 || file.size > maxBytes);
    if (invalid) {
      showError("Mỗi ảnh phải là PNG, JPEG hoặc WebP hợp lệ và không vượt quá 5 MB.");
      render();
      return;
    }

    const known = new Set(selectedFiles.map(keyOf));
    incoming.forEach(file => {
      if (!known.has(keyOf(file))) {
        selectedFiles.push(file);
        known.add(keyOf(file));
      }
    });
    if (existingCount + selectedFiles.length > maxImages) {
      selectedFiles.splice(Math.max(0, maxImages - existingCount));
      showError(`Sản phẩm chỉ được có tối đa ${maxImages} ảnh, hiện đã có ${existingCount} ảnh.`);
    } else {
      showError("");
    }
    render();
  });

  window.addEventListener("pagehide", releaseUrls, { once: true });
})();
