(() => {
  const form = document.querySelector('[data-inventory-variant-filter]');
  if (!form) return;

  const productSelect = form.querySelector('[data-product-select]');
  const sizeSelect = form.querySelector('[data-size-select]');
  const colorSelect = form.querySelector('[data-color-select]');
  const variantIdInput = form.querySelector('[data-variant-id]');
  const result = form.querySelector('[data-variant-result]');
  const variants = Array.from(form.querySelectorAll('[data-variant-source]')).map(item => ({
    variantId: item.dataset.variantId,
    productId: item.dataset.productId,
    sizeId: item.dataset.sizeId,
    sizeName: item.dataset.sizeName,
    colorId: item.dataset.colorId,
    colorName: item.dataset.colorName,
    sku: item.dataset.sku
  }));

  const uniqueValues = (items, idKey, nameKey) => {
    const values = new Map();
    items.forEach(item => values.set(item[idKey], item[nameKey]));
    return Array.from(values, ([id, name]) => ({ id, name }))
      .sort((left, right) => left.name.localeCompare(right.name, 'vi', { numeric: true }));
  };

  const fillSelect = (select, placeholder, values, selectedValue = '') => {
    select.replaceChildren(new Option(placeholder, ''));
    values.forEach(value => select.add(new Option(value.name, value.id)));
    select.disabled = values.length === 0;
    if (selectedValue && values.some(value => value.id === selectedValue)) {
      select.value = selectedValue;
    } else if (values.length === 1) {
      select.value = values[0].id;
    }
  };

  const productVariants = () => variants.filter(item => item.productId === productSelect.value);
  const matchingSizes = () => uniqueValues(productVariants(), 'sizeId', 'sizeName');
  const matchingColors = () => uniqueValues(
    productVariants().filter(item => item.sizeId === sizeSelect.value),
    'colorId',
    'colorName'
  );

  const syncVariant = () => {
    const match = variants.find(item =>
      item.productId === productSelect.value &&
      item.sizeId === sizeSelect.value &&
      item.colorId === colorSelect.value
    );
    variantIdInput.value = match?.variantId || '';
    if (match) {
      result.textContent = `${match.sku} · Kích cỡ ${match.sizeName} · ${match.colorName}`;
    } else if (!productSelect.value) {
      result.textContent = 'Tất cả biến thể';
    } else if (!sizeSelect.value) {
      result.textContent = 'Hãy chọn kích cỡ';
    } else {
      result.textContent = 'Hãy chọn màu sắc tương ứng';
    }
  };

  const renderColors = (selectedColor = '') => {
    if (!productSelect.value || !sizeSelect.value) {
      fillSelect(colorSelect, 'Chọn kích cỡ trước', []);
      syncVariant();
      return;
    }
    fillSelect(colorSelect, 'Chọn màu sắc', matchingColors(), selectedColor);
    syncVariant();
  };

  const renderSizes = (selectedSize = '', selectedColor = '') => {
    if (!productSelect.value) {
      fillSelect(sizeSelect, 'Chọn sản phẩm trước', []);
      fillSelect(colorSelect, 'Chọn kích cỡ trước', []);
      syncVariant();
      return;
    }
    fillSelect(sizeSelect, 'Chọn kích cỡ', matchingSizes(), selectedSize);
    renderColors(selectedColor);
  };

  productSelect.addEventListener('change', () => {
    variantIdInput.value = '';
    renderSizes();
  });
  sizeSelect.addEventListener('change', () => {
    variantIdInput.value = '';
    renderColors();
  });
  colorSelect.addEventListener('change', syncVariant);

  form.addEventListener('submit', event => {
    if (productSelect.value && !variantIdInput.value) {
      event.preventDefault();
      result.textContent = 'Vui lòng chọn đủ kích cỡ và màu sắc.';
      (sizeSelect.value ? colorSelect : sizeSelect).focus();
    }
  });

  const selected = variants.find(item => item.variantId === variantIdInput.value);
  if (selected) {
    productSelect.value = selected.productId;
    renderSizes(selected.sizeId, selected.colorId);
  } else {
    renderSizes();
  }
})();
