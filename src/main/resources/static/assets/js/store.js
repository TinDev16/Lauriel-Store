document.addEventListener('DOMContentLoaded', () => {
  const menu = document.querySelector('[data-store-menu]');
  const nav = document.querySelector('[data-store-nav]');
  if (menu && nav) menu.addEventListener('click', () => nav.classList.toggle('open'));
  const headerSearch = document.querySelector('[data-store-search]');
  const searchToggle = headerSearch?.querySelector('[data-store-search-toggle]');
  const searchPanel = headerSearch?.querySelector('[data-store-search-panel]');
  const closeHeaderSearch = () => {
    if (!headerSearch || !searchToggle) return;
    headerSearch.classList.remove('open');
    searchToggle.setAttribute('aria-expanded', 'false');
  };
  if (headerSearch && searchToggle && searchPanel) {
    searchToggle.addEventListener('click', () => {
      const open = !headerSearch.classList.contains('open');
      headerSearch.classList.toggle('open', open);
      searchToggle.setAttribute('aria-expanded', String(open));
      if (open) window.setTimeout(() => searchPanel.querySelector('input[name="keyword"]')?.focus(), 0);
    });
    document.addEventListener('click', event => {
      if (!headerSearch.contains(event.target)) closeHeaderSearch();
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') { closeHeaderSearch(); searchToggle.focus(); }
    });
  }
  document.querySelectorAll('[data-clear-search]').forEach(button => button.addEventListener('click', () => {
    const input = button.closest('form')?.querySelector('input[name="keyword"]'); if (input) { input.value = ''; input.form.submit(); }
  }));
  const filterToggle = document.querySelector('[data-filter-toggle]');
  const filterPanel = document.querySelector('[data-filter-panel]');
  if (filterToggle && filterPanel) filterToggle.addEventListener('click', () => filterPanel.classList.toggle('open'));
  document.querySelectorAll('[data-dismiss-alert]').forEach(button => button.addEventListener('click', () => button.closest('.store-toast')?.remove()));
  document.querySelectorAll('[data-cart-auto-update]').forEach(form => {
    const input = form.querySelector('input[name="quantity"]');
    const status = form.querySelector('[data-cart-update-status]');
    let timer;

    const normalizedQuantity = () => {
      const minimum = Number(input.min || 1);
      const maximum = Number(input.max || Number.MAX_SAFE_INTEGER);
      const current = Number(input.value);
      if (!Number.isFinite(current)) return null;
      return Math.max(minimum, Math.min(maximum, Math.trunc(current)));
    };
    const submitUpdate = () => {
      const quantity = normalizedQuantity();
      if (quantity === null) return;
      input.value = quantity;
      if (String(quantity) === input.dataset.originalQuantity || form.dataset.submitting === 'true') return;
      form.requestSubmit();
    };
    const scheduleUpdate = delay => {
      window.clearTimeout(timer);
      timer = window.setTimeout(submitUpdate, delay);
    };

    form.querySelectorAll('[data-cart-minus],[data-cart-plus]').forEach(button => button.addEventListener('click', () => {
      const quantity = normalizedQuantity() ?? Number(input.min || 1);
      const delta = button.hasAttribute('data-cart-plus') ? 1 : -1;
      input.value = quantity + delta;
      input.value = normalizedQuantity();
      scheduleUpdate(180);
    }));
    input.addEventListener('input', () => {
      if (input.value !== '' && input.checkValidity()) scheduleUpdate(550);
    });
    input.addEventListener('change', () => scheduleUpdate(0));
    form.addEventListener('submit', () => {
      window.clearTimeout(timer);
      form.dataset.submitting = 'true';
      form.setAttribute('aria-busy', 'true');
      form.querySelectorAll('button').forEach(button => { button.disabled = true; });
      if (status) status.textContent = 'Đang cập nhật…';
    });
  });
  const addressBook = document.querySelector('[data-checkout-addresses]');
  if (addressBook) {
    const addressInput = addressBook.querySelector('[data-shipping-address]');
    const addressSelect = addressBook.querySelector('[data-address-select]');
    const addressListbox = addressBook.querySelector('[data-address-listbox]');
    let savedAddresses = addressSelect ? [...addressSelect.querySelectorAll('[data-saved-address]')] : [];
    const editor = addressBook.querySelector('[data-address-editor]');
    const editorTitle = addressBook.querySelector('[data-address-editor-title]');
    const confirmedBadge = addressBook.querySelector('[data-address-confirmed]');
    const editButton = addressBook.querySelector('[data-address-edit]');
    const confirmButton = addressBook.querySelector('[data-address-confirm]');
    const cancelButton = addressBook.querySelector('[data-address-cancel]');
    const addressHelp = addressBook.querySelector('[data-address-help]');
    const prompt = addressBook.querySelector('[data-save-address-prompt]');
    const promptTitle = addressBook.querySelector('[data-save-address-title]');
    const promptDescription = addressBook.querySelector('[data-save-address-description]');
    const saveAddress = addressBook.querySelector('[data-save-address]');
    const saveOption = addressBook.querySelector('[data-save-address-option]');
    const defaultAddress = addressBook.querySelector('[data-default-address]');
    const defaultOption = addressBook.querySelector('[data-default-address-option]');
    let hasSaved = addressBook.dataset.hasSaved === 'true';
    const normalizeAddress = value => (value || '').trim().replace(/\s+/g, ' ').toLocaleLowerCase('vi');
    let confirmedValue = (addressInput?.value || '').trim();
    let confirmed = addressBook.dataset.initialConfirmed === 'true' && confirmedValue.length > 0;
    let editing = !confirmed;
    let selectionBeforeEdit = addressSelect?.value || '';
    let lastSelected = selectionBeforeEdit;
    let pending = false;
    let feedbackMessage = '';
    let feedbackError = false;

    const matchedAddress = () => savedAddresses.find(option =>
      normalizeAddress(option.dataset.savedAddress) === normalizeAddress(addressInput?.value));

    const resetPreferences = () => {
      if (saveAddress) saveAddress.checked = false;
      if (defaultAddress) defaultAddress.checked = false;
    };

    const updatePreferences = () => {
      if (!prompt) return;
      const matched = confirmed ? matchedAddress() : null;
      const matchedIsDefault = matched?.dataset.defaultAddress === 'true';
      const newAddress = confirmed && !matched;
      const canMakeDefault = confirmed && !!matched && !matchedIsDefault;
      prompt.hidden = !(newAddress || canMakeDefault);
      if (saveOption) saveOption.hidden = !newAddress;
      if (defaultOption) defaultOption.hidden = !(canMakeDefault || (newAddress && hasSaved && saveAddress?.checked));

      if (newAddress) {
        if (promptTitle) promptTitle.textContent = hasSaved
          ? 'Bạn muốn lưu thêm địa chỉ mới này?'
          : 'Bạn muốn lưu địa chỉ đầu tiên này?';
        if (promptDescription) promptDescription.textContent = hasSaved
          ? 'Địa chỉ sẽ được thêm vào danh sách để bạn chọn nhanh trong những lần mua sau.'
          : 'Lần mua sau bạn có thể chọn nhanh mà không cần nhập lại. Địa chỉ đầu tiên sẽ được đặt làm mặc định.';
      } else if (canMakeDefault) {
        if (promptTitle) promptTitle.textContent = 'Sử dụng địa chỉ này thường xuyên?';
        if (promptDescription) promptDescription.textContent = 'Bạn có thể đặt địa chỉ đang chọn làm địa chỉ mặc định cho lần mua sau.';
        if (saveAddress) saveAddress.checked = !!defaultAddress?.checked;
      } else {
        resetPreferences();
      }
    };

    const renderAddressState = () => {
      editor?.classList.toggle('is-editing', editing);
      editor?.classList.toggle('is-saving', pending);
      editor?.setAttribute('aria-busy', pending ? 'true' : 'false');
      if (addressInput) addressInput.readOnly = !editing;
      if (editorTitle) editorTitle.textContent = editing ? 'Chỉnh sửa địa chỉ nhận hàng' : 'Địa chỉ đang sử dụng';
      if (confirmedBadge) confirmedBadge.hidden = !confirmed || editing;
      if (editButton) editButton.hidden = editing || !confirmed;
      if (confirmButton) confirmButton.hidden = !editing;
      if (confirmButton) confirmButton.disabled = pending;
      if (editButton) editButton.disabled = pending;
      if (cancelButton) {
        cancelButton.hidden = !editing || !confirmedValue;
        cancelButton.disabled = pending;
      }
      if (addressHelp) {
        addressHelp.classList.toggle('is-error', feedbackError);
        addressHelp.textContent = pending ? 'Đang lưu địa chỉ…' : feedbackMessage || (editing
          ? 'Sau khi sửa, hãy nhấn Xác nhận địa chỉ để tiếp tục đặt hàng.'
          : 'Địa chỉ này đã được xác nhận và sẽ không mất khi tải lại trang.');
      }
      updatePreferences();
    };

    const startEditing = (clearAddress = false) => {
      if (clearAddress && addressInput) addressInput.value = '';
      editing = true;
      confirmed = false;
      feedbackMessage = '';
      feedbackError = false;
      resetPreferences();
      renderAddressState();
      addressInput?.focus();
    };

    const refreshAddressOptions = (addresses, currentAddress) => {
      if (!addressSelect || !Array.isArray(addresses)) return;
      addressSelect.querySelectorAll('[data-saved-address]').forEach(option => option.remove());
      const newAddressOption = addressSelect.querySelector('option[value="new"]');
      addresses.forEach(address => {
        const option = document.createElement('option');
        option.value = String(address.addressId);
        option.dataset.savedAddress = address.address;
        option.dataset.defaultAddress = String(address.defaultAddress);
        option.textContent = address.address + (address.defaultAddress ? ' (Mặc định)' : '');
        addressSelect.insertBefore(option, newAddressOption);
      });
      savedAddresses = [...addressSelect.querySelectorAll('[data-saved-address]')];
      hasSaved = savedAddresses.length > 0;
      addressBook.dataset.hasSaved = String(hasSaved);
      if (addressListbox) addressListbox.hidden = !hasSaved;
      const matched = savedAddresses.find(option =>
        normalizeAddress(option.dataset.savedAddress) === normalizeAddress(currentAddress));
      addressSelect.value = matched?.value || 'new';
      lastSelected = addressSelect.value;
      selectionBeforeEdit = addressSelect.value;
    };

    const persistAddress = async (save = false, makeDefault = false) => {
      if (!addressInput || pending) return false;
      addressInput.value = addressInput.value.trim();
      if (!addressInput.reportValidity()) return false;
      pending = true;
      feedbackMessage = '';
      feedbackError = false;
      renderAddressState();
      try {
        const body = new URLSearchParams({
          shippingAddress: addressInput.value,
          saveAddress: String(save),
          setAsDefault: String(makeDefault)
        });
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' };
        if (addressBook.dataset.csrfHeader && addressBook.dataset.csrfToken) {
          headers[addressBook.dataset.csrfHeader] = addressBook.dataset.csrfToken;
        }
        const response = await fetch(addressBook.dataset.confirmUrl, {
          method: 'POST', headers, body, credentials: 'same-origin'
        });
        if (!response.ok) throw new Error('Không thể lưu địa chỉ. Vui lòng thử lại.');
        const result = await response.json();
        addressInput.value = result.address;
        confirmedValue = result.address;
        confirmed = true;
        editing = false;
        refreshAddressOptions(result.addresses, result.address);
        feedbackMessage = result.saved
          ? 'Đã lưu địa chỉ vào sổ địa chỉ.'
          : 'Đã xác nhận địa chỉ.';
        feedbackError = false;
        return true;
      } catch (error) {
        confirmed = false;
        editing = true;
        feedbackMessage = error.message || 'Không thể lưu địa chỉ. Vui lòng thử lại.';
        feedbackError = true;
        return false;
      } finally {
        pending = false;
        renderAddressState();
      }
    };

    addressSelect?.addEventListener('change', async () => {
      const selected = addressSelect.selectedOptions[0];
      if (selected?.value === 'new') {
        selectionBeforeEdit = lastSelected;
        startEditing(true);
        return;
      }
      if (selected?.dataset.savedAddress && addressInput) {
        addressInput.value = selected.dataset.savedAddress;
        confirmedValue = addressInput.value.trim();
        confirmed = true;
        editing = false;
        lastSelected = selected.value;
        selectionBeforeEdit = selected.value;
        resetPreferences();
        renderAddressState();
        await persistAddress(false, false);
      }
    });
    editButton?.addEventListener('click', () => {
      selectionBeforeEdit = addressSelect?.value || selectionBeforeEdit;
      startEditing(false);
    });
    confirmButton?.addEventListener('click', () => persistAddress(false, false));
    cancelButton?.addEventListener('click', () => {
      if (addressInput) addressInput.value = confirmedValue;
      if (addressSelect && selectionBeforeEdit) {
        addressSelect.value = selectionBeforeEdit;
        lastSelected = selectionBeforeEdit;
      }
      confirmed = confirmedValue.length > 0;
      editing = false;
      feedbackMessage = '';
      feedbackError = false;
      resetPreferences();
      renderAddressState();
    });
    addressInput?.addEventListener('input', () => {
      confirmed = false;
      prompt && (prompt.hidden = true);
      if (confirmedBadge) confirmedBadge.hidden = true;
      if (addressHelp) {
        addressHelp.classList.remove('is-error');
        addressHelp.textContent = 'Sau khi sửa, hãy nhấn Xác nhận địa chỉ để tiếp tục đặt hàng.';
      }
    });
    saveAddress?.addEventListener('change', async () => {
      if (!saveAddress.checked && defaultAddress) defaultAddress.checked = false;
      updatePreferences();
      if (saveAddress.checked) await persistAddress(true, !!defaultAddress?.checked);
    });
    defaultAddress?.addEventListener('change', async () => {
      if (defaultAddress.checked && saveAddress) saveAddress.checked = true;
      updatePreferences();
      if (defaultAddress.checked) await persistAddress(true, true);
    });
    document.querySelector('#checkout-form')?.addEventListener('submit', event => {
      if (confirmed && !editing && !pending) return;
      event.preventDefault();
      event.stopImmediatePropagation();
      if (addressHelp) {
        addressHelp.textContent = pending
          ? 'Vui lòng chờ hệ thống lưu địa chỉ.'
          : 'Vui lòng nhấn Xác nhận địa chỉ trước khi đặt hàng.';
        addressHelp.classList.add('is-error');
      }
      addressInput?.focus();
    });
    renderAddressState();
  }
  document.querySelectorAll('form[data-confirm]').forEach(form => form.addEventListener('submit', event => {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  }));
  document.querySelectorAll('[data-submit-once]').forEach(form => form.addEventListener('submit', () => {
    const button = form.querySelector('[data-submit-button]'); if (button) { button.disabled = true; button.classList.add('loading'); button.querySelector('span').textContent = 'Đang đặt hàng…'; }
  }));

  // Toast notification helper: delegates to unified window.showToast
  function showStoreToast(message, type = 'success', duration = 1500) {
    if (typeof window.showToast === 'function') {
      return window.showToast(message, type, duration);
    }
  }

  // Header cart badge update
  function updateStoreCartBadge(count) {
    let badge = document.querySelector('.store-cart-badge');
    if (badge) {
      badge.textContent = count;
      badge.style.display = count > 0 ? '' : 'none';
    } else if (count > 0) {
      const cartLink = document.querySelector('a[href="/shop/cart"]');
      if (cartLink) {
        badge = document.createElement('span');
        badge.className = 'store-cart-badge';
        badge.textContent = count;
        cartLink.appendChild(badge);
      }
    }
  }

  // CSRF tokens
  function getCsrfInfo() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    const token = tokenMeta ? tokenMeta.getAttribute('content') : '';
    const header = headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN';
    return { token, header };
  }

  // Handle Add to Cart from Product Card
  async function handleCardAddToCart(productId, quantity, button) {
    if (!productId) return;
    if (button) button.disabled = true;
    try {
      const { token, header } = getCsrfInfo();
      const formData = new FormData();
      formData.append('productId', productId);
      formData.append('quantity', quantity);
      if (token) formData.append('_csrf', token);

      const headers = {
        'X-Requested-With': 'XMLHttpRequest',
        'Accept': 'application/json'
      };
      if (token && header) headers[header] = token;

      const res = await fetch('/shop/cart/add', {
        method: 'POST',
        headers,
        body: formData
      });
      const data = await res.json().catch(() => null);
      if (res.ok && data?.success) {
        showStoreToast(data.message || 'Thêm vào giỏ hàng thành công!', 'success', 1500);
        if (data.cartCount !== undefined) updateStoreCartBadge(data.cartCount);
      } else {
        showStoreToast(data?.message || 'Không thể thêm vào giỏ hàng.', 'error', 1500);
      }
    } catch (err) {
      showStoreToast('Lỗi kết nối khi thêm vào giỏ hàng.', 'error', 1500);
    } finally {
      if (button) button.disabled = false;
    }
  }

  // Handle Buy Now from Product Card
  function handleCardBuyNow(productId, quantity) {
    if (!productId) return;
    const { token } = getCsrfInfo();
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/shop/cart/buy-now';
    form.style.display = 'none';

    const pInput = document.createElement('input');
    pInput.type = 'hidden';
    pInput.name = 'productId';
    pInput.value = productId;
    form.appendChild(pInput);

    const qInput = document.createElement('input');
    qInput.type = 'hidden';
    qInput.name = 'quantity';
    qInput.value = quantity;
    form.appendChild(qInput);

    if (token) {
      const csrfInput = document.createElement('input');
      csrfInput.type = 'hidden';
      csrfInput.name = '_csrf';
      csrfInput.value = token;
      form.appendChild(csrfInput);
    }

    document.body.appendChild(form);
    form.submit();
  }

  // Delegate click for product card stepper & buttons
  document.addEventListener('click', event => {
    const minusBtn = event.target.closest('[data-card-qty-minus]');
    if (minusBtn) {
      const stepper = minusBtn.closest('.store-card-qty-stepper');
      const input = stepper?.querySelector('[data-card-qty-input]');
      if (input) {
        input.value = Math.max(1, Number(input.value || 1) - 1);
      }
      return;
    }

    const plusBtn = event.target.closest('[data-card-qty-plus]');
    if (plusBtn) {
      const stepper = plusBtn.closest('.store-card-qty-stepper');
      const input = stepper?.querySelector('[data-card-qty-input]');
      if (input) {
        input.value = Math.min(999, Number(input.value || 1) + 1);
      }
      return;
    }

    const addBtn = event.target.closest('[data-card-add-cart]');
    if (addBtn) {
      const card = addBtn.closest('[data-product-card]');
      const productId = addBtn.dataset.productId;
      const input = card?.querySelector('[data-card-qty-input]');
      const quantity = Math.max(1, Number(input?.value || 1));
      handleCardAddToCart(productId, quantity, addBtn);
      return;
    }

    const buyBtn = event.target.closest('[data-card-buy-now]');
    if (buyBtn) {
      const card = buyBtn.closest('[data-product-card]');
      const productId = buyBtn.dataset.productId;
      const input = card?.querySelector('[data-card-qty-input]');
      const quantity = Math.max(1, Number(input?.value || 1));
      handleCardBuyNow(productId, quantity);
      return;
    }
  });

  // Intercept standard Add-to-Cart form submits (e.g. from Product Detail page)
  document.addEventListener('submit', async event => {
    const form = event.target;
    if (form && form.action && form.action.includes('/shop/cart/add')) {
      event.preventDefault();
      const submitBtn = form.querySelector('[data-purchase-button], button[type="submit"]');
      if (submitBtn) submitBtn.disabled = true;
      try {
        const formData = new FormData(form);
        const { token, header } = getCsrfInfo();
        const headers = {
          'X-Requested-With': 'XMLHttpRequest',
          'Accept': 'application/json'
        };
        if (token && header && !formData.has('_csrf')) headers[header] = token;

        const res = await fetch(form.action, {
          method: 'POST',
          headers,
          body: formData
        });
        const data = await res.json().catch(() => null);
        if (res.ok && data?.success) {
          showStoreToast(data.message || 'Thêm vào giỏ hàng thành công!', 'success', 1500);
          if (data.cartCount !== undefined) updateStoreCartBadge(data.cartCount);
        } else {
          showStoreToast(data?.message || 'Không thể thêm vào giỏ hàng.', 'error', 1500);
        }
      } catch (err) {
        showStoreToast('Lỗi kết nối khi thêm vào giỏ hàng.', 'error', 1500);
      } finally {
        if (submitBtn) submitBtn.disabled = false;
      }
    }
  });

  // Track recently viewed products in localStorage & cookie (works even without login)
  const detailMain = document.querySelector('[data-detail-product-id]');
  if (detailMain) {
    const pid = detailMain.dataset.detailProductId;
    if (pid) {
      try {
        let viewed = JSON.parse(localStorage.getItem('lauriel_viewed') || '[]');
        if (!Array.isArray(viewed)) viewed = [];
        viewed = [Number(pid), ...viewed.filter(id => Number(id) !== Number(pid))].slice(0, 12);
        localStorage.setItem('lauriel_viewed', JSON.stringify(viewed));
        document.cookie = `lauriel_viewed=${viewed.join('-')}; path=/; max-age=2592000; SameSite=Lax`;
      } catch (e) {}
    }
  }

  // Clear recently viewed button handler
  document.addEventListener('click', async event => {
    const clearBtn = event.target.closest('[data-clear-recently-viewed]');
    if (!clearBtn) return;
    clearBtn.disabled = true;
    try {
      localStorage.removeItem('lauriel_viewed');
      document.cookie = 'lauriel_viewed=; path=/; max-age=0; SameSite=Lax';

      const { token, header } = getCsrfInfo();
      const headers = { 'X-Requested-With': 'XMLHttpRequest' };
      if (token && header) headers[header] = token;

      await fetch('/shop/products/clear-recently-viewed', {
        method: 'POST',
        headers
      });

      const section = document.getElementById('recentlyViewedSection');
      if (section) {
        section.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        section.style.opacity = '0';
        section.style.transform = 'translateY(10px)';
        window.setTimeout(() => section.remove(), 300);
      }
      showStoreToast('Đã xóa lịch sử xem sản phẩm.', 'info');
    } catch (e) {
      showStoreToast('Không thể xóa lịch sử xem.', 'error');
    } finally {
      clearBtn.disabled = false;
    }
  });

  // Save scroll position and origin URL before navigating to a product detail page
  document.addEventListener('click', event => {
    const productLink = event.target.closest('a[href*="/shop/products/"]');
    if (!productLink) return;
    if (productLink.closest('[data-store-back]') || event.target.closest('button')) return;

    const card = productLink.closest('[data-product-card]');
    const pid = card?.dataset?.cardProductId ||
                productLink.getAttribute('href').split('/shop/products/')[1]?.split('?')[0];

    sessionStorage.setItem('store_return_url', window.location.href);
    sessionStorage.setItem('store_return_scroll_y', String(window.scrollY));
    if (pid) {
      sessionStorage.setItem('store_return_product_id', String(pid));
    }
  });

  // Back button handler on Product Detail page
  document.addEventListener('click', event => {
    const backBtn = event.target.closest('[data-store-back]');
    if (!backBtn) return;
    event.preventDefault();

    const returnUrl = sessionStorage.getItem('store_return_url');
    if (window.history.length > 1 && document.referrer && document.referrer.includes(window.location.host)) {
      window.history.back();
    } else if (returnUrl && !returnUrl.includes(window.location.pathname)) {
      window.location.href = returnUrl;
    } else {
      window.location.href = '/shop';
    }
  });

  // Restore exact scroll position when returning from product detail
  function tryRestoreProductScroll() {
    const returnUrl = sessionStorage.getItem('store_return_url');
    const savedY = sessionStorage.getItem('store_return_scroll_y');
    const targetPid = sessionStorage.getItem('store_return_product_id');

    if (returnUrl && window.location.href.split('#')[0] === returnUrl.split('#')[0] && savedY !== null) {
      const targetScroll = parseInt(savedY, 10);
      if (!isNaN(targetScroll)) {
        window.requestAnimationFrame(() => {
          window.scrollTo({ top: targetScroll, behavior: 'instant' });
        });
        window.setTimeout(() => {
          window.scrollTo({ top: targetScroll, behavior: 'instant' });
        }, 120);
      }

      if (targetPid) {
        window.setTimeout(() => {
          const card = document.getElementById(`product-card-${targetPid}`) ||
                       document.querySelector(`[data-card-product-id="${targetPid}"]`);
          if (card) {
            card.classList.add('store-card-returned');
            window.setTimeout(() => card.classList.remove('store-card-returned'), 1600);
          }
        }, 150);
      }

      sessionStorage.removeItem('store_return_scroll_y');
      sessionStorage.removeItem('store_return_product_id');
    }
  }

  window.addEventListener('pageshow', tryRestoreProductScroll);
  tryRestoreProductScroll();
});
