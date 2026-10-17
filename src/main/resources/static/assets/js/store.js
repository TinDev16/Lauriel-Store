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
  window.setTimeout(() => document.querySelectorAll('.store-toast').forEach(alert => alert.classList.add('hide')), 4500);
});
