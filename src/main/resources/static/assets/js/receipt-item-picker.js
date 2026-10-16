(() => {
  const picker = document.querySelector("[data-receipt-item-picker]");
  if (!picker) return;

  const search = picker.querySelector("[data-product-search]");
  const clearSearch = picker.querySelector("[data-clear-product-search]");
  const productSelect = picker.querySelector("[data-product-select]");
  const sizeSelect = picker.querySelector("[data-size-select]");
  const colorSelect = picker.querySelector("[data-color-select]");
  const variantId = picker.querySelector("[data-variant-id]");
  const unitPrice = picker.querySelector('[name="unitPrice"]');
  const productResult = picker.querySelector("[data-product-result]");
  const variantResult = picker.querySelector("[data-variant-result]");
  const submit = picker.querySelector("[data-add-receipt-item]");
  const productOptions = Array.from(productSelect.options).filter(option => option.value);
  const variants = Array.from(picker.querySelectorAll("[data-variant-source]")).map(source => ({
    id: source.dataset.variantId,
    productId: source.dataset.productId,
    sizeId: source.dataset.sizeId,
    sizeName: source.dataset.sizeName,
    colorId: source.dataset.colorId,
    colorName: source.dataset.colorName,
    sku: source.dataset.sku,
    quantity: source.dataset.quantity
  }));

  const normalize = value => (value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("vi")
    .trim();

  function filterProducts() {
    const term = normalize(search.value);
    let visible = 0;
    productOptions.forEach(option => {
      const matches = !term || normalize(option.textContent).includes(term);
      option.hidden = !matches;
      option.disabled = !matches;
      if (matches) visible++;
    });
    const selected = productSelect.selectedOptions[0];
    if (selected && selected.value && selected.disabled) {
      productSelect.value = "";
      setDefaultUnitPrice();
      renderSizes();
    }
    productResult.textContent = term
      ? visible + " sản phẩm phù hợp"
      : productOptions.length + " sản phẩm có thuộc tính";
  }

  function uniqueById(items, idKey, nameKey) {
    return Array.from(new Map(items.map(item => [item[idKey], {
      id: item[idKey],
      name: item[nameKey]
    }])).values());
  }

  function fillSelect(select, placeholder, items, selectedValue = "") {
    select.replaceChildren(new Option(placeholder, ""));
    items.forEach(item => select.add(new Option(item.name, item.id)));
    select.disabled = items.length === 0;
    select.value = items.some(item => item.id === selectedValue) ? selectedValue : "";
    if (!select.value && items.length === 1) select.value = items[0].id;
  }

  function productVariants() {
    return variants.filter(variant => variant.productId === productSelect.value);
  }

  function setDefaultUnitPrice() {
    const selectedProduct = productSelect.selectedOptions[0];
    unitPrice.value = selectedProduct?.value ? selectedProduct.dataset.basePrice || "" : "";
    unitPrice.dispatchEvent(new Event("input", { bubbles: true }));
  }

  function renderSizes(selectedSize = "", selectedColor = "") {
    const sizes = uniqueById(productVariants(), "sizeId", "sizeName");
    fillSelect(sizeSelect, productSelect.value ? "Chọn kích cỡ" : "Hãy chọn sản phẩm trước", sizes, selectedSize);
    variantId.value = "";
    renderColors(selectedColor);
  }

  function renderColors(selectedColor = "") {
    const matchingSize = productVariants().filter(variant => variant.sizeId === sizeSelect.value);
    const colors = uniqueById(matchingSize, "colorId", "colorName");
    fillSelect(colorSelect, sizeSelect.value ? "Chọn màu sắc" : "Hãy chọn kích cỡ trước", colors, selectedColor);
    variantId.value = "";
    resolveVariant();
  }

  function resolveVariant() {
    const selected = productVariants().find(variant =>
      variant.sizeId === sizeSelect.value && variant.colorId === colorSelect.value);
    variantId.value = selected ? selected.id : "";
    if (selected) {
      variantResult.textContent = "SKU: " + selected.sku + " · Tồn hiện tại: " + selected.quantity;
    } else if (!productSelect.value) {
      variantResult.textContent = "Chọn sản phẩm để xem kích cỡ và màu sắc hiện có.";
    } else if (!sizeSelect.value) {
      variantResult.textContent = "Chọn kích cỡ để xem các màu sắc tương ứng.";
    } else {
      variantResult.textContent = "Chọn màu sắc để xác định đúng SKU.";
    }
    submit.disabled = !variantId.value;
  }

  productSelect.addEventListener("change", () => {
    setDefaultUnitPrice();
    renderSizes();
  });
  sizeSelect.addEventListener("change", () => renderColors());
  colorSelect.addEventListener("change", resolveVariant);
  search.addEventListener("input", filterProducts);
  clearSearch.addEventListener("click", () => {
    search.value = "";
    filterProducts();
    search.focus();
  });

  const selectedVariant = variants.find(variant => variant.id === variantId.value);
  if (selectedVariant) productSelect.value = selectedVariant.productId;
  filterProducts();
  renderSizes(selectedVariant?.sizeId, selectedVariant?.colorId);
})();
