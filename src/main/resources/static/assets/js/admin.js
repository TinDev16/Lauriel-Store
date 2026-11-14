document.addEventListener('DOMContentLoaded', () => {
  document.addEventListener('submit', event => {
    const confirmForm = event.target.closest('form[data-confirm]');
    if (confirmForm && confirmForm.dataset.confirm) {
      if (!window.confirm(confirmForm.dataset.confirm)) {
        event.preventDefault();
      }
    }
  });

  const numberInputs = document.querySelectorAll('.number-input');
  const formatNumber = (input, initial = false) => {
    const scale = Number(input.dataset.numberScale || 0);
    let raw = input.value.trim().replace(/\s+/g, '');
    if (!raw) return;
    const negative = raw.startsWith('-');
    raw = raw.replace(/[^0-9.,]/g, '');
    let integer = raw;
    let fraction = '';
    if (raw.includes(',')) {
      const parts = raw.split(',');
      integer = parts.shift().replace(/[.]/g, '');
      fraction = parts.join('').replace(/\D/g, '').slice(0, scale);
    } else {
      const parts = raw.split('.');
      const last = parts.at(-1) || '';
      const canonicalDecimal = initial && scale > 0 && parts.length === 2 && last.length > 0 && last.length <= scale;
      if (canonicalDecimal) {
        integer = parts[0];
        fraction = last.slice(0, scale);
      } else {
        integer = parts.join('');
      }
    }
    integer = integer.replace(/\D/g, '').replace(/^0+(?=\d)/, '') || '0';
    fraction = fraction.replace(/0+$/, '');
    const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    input.value = `${negative ? '-' : ''}${grouped}${fraction ? ',' + fraction : ''}`;
  };
  numberInputs.forEach(input => {
    formatNumber(input, true);
    input.addEventListener('input', () => formatNumber(input));
  });
  document.querySelectorAll('form').forEach(form => {
    form.addEventListener('submit', event => {
      if (event.defaultPrevented) return;
      form.querySelectorAll('.number-input').forEach(input => {
        input.value = input.value.replace(/[.]/g, '').replace(',', '.');
      });
    });
  });
  const liveSearchStorageKey = 'admin-live-search';
  const liveSearchInputs = document.querySelectorAll(
    'form[method="get"] input[type="search"][name], form[method="get"] input[name="keyword"]'
  );
  liveSearchInputs.forEach(input => {
    let timer;
    input.addEventListener('input', () => {
      window.clearTimeout(timer);
      timer = window.setTimeout(() => {
        const form = input.form;
        if (!form) return;
        const pageInput = form.querySelector('[name="page"]');
        if (pageInput) pageInput.value = '0';
        sessionStorage.setItem(liveSearchStorageKey, `${location.pathname}:${input.name}`);
        form.requestSubmit();
      }, 350);
    });
  });
  const focusedSearch = sessionStorage.getItem(liveSearchStorageKey);
  const searchToRestore = Array.from(liveSearchInputs).find(
    input => focusedSearch === `${location.pathname}:${input.name}`
  );
  if (searchToRestore) {
    sessionStorage.removeItem(liveSearchStorageKey);
    searchToRestore.focus();
    const caret = searchToRestore.value.length;
    searchToRestore.setSelectionRange?.(caret, caret);
  }
  document.querySelectorAll(
    'form[data-live-filter] select[name]:not([data-cascade-filter]), form[data-live-filter] input[type="date"][name], form[data-live-filter] input[type="datetime-local"][name]'
  ).forEach(control => {
    control.addEventListener('change', () => {
      const form = control.form;
      const pageInput = form?.querySelector('[name="page"]');
      if (pageInput) pageInput.value = '0';
      form?.requestSubmit();
    });
  });

  const scrollStorageKey = `admin-filter-scroll:${location.pathname}`;
  document.querySelectorAll('form[data-preserve-scroll]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!event.defaultPrevented) {
        sessionStorage.setItem(scrollStorageKey, JSON.stringify({ x: window.scrollX, y: window.scrollY }));
      }
    });
  });
  const savedScroll = sessionStorage.getItem(scrollStorageKey);
  if (savedScroll) {
    sessionStorage.removeItem(scrollStorageKey);
    try {
      const position = JSON.parse(savedScroll);
      window.requestAnimationFrame(() => window.scrollTo(position.x || 0, position.y || 0));
    } catch (_) {
      // Ignore obsolete browser data.
    }
  }

  document.querySelectorAll('form[data-product-selection]').forEach(form => {
    const choices = Array.from(form.querySelectorAll('[data-product-choice]'));
    const count = form.querySelector('[data-selected-count]');
    const update = () => {
      let selected = 0;
      choices.forEach(choice => {
        const checkbox = choice.querySelector('input[type="checkbox"]');
        const checked = Boolean(checkbox?.checked);
        choice.classList.toggle('border-primary', checked);
        choice.classList.toggle('bg-light', checked);
        choice.classList.toggle('is-selected', checked);
        choice.setAttribute('aria-selected', String(checked));
        if (checked) selected++;
      });
      if (count) count.textContent = String(selected);
    };
    choices.forEach(choice => choice.querySelector('input[type="checkbox"]')?.addEventListener('change', () => {
      if (form.matches('[data-variant-scope]')) {
        const selectedScope = form.querySelector('input[name="scope"][value="SELECTED"]');
        if (selectedScope) selectedScope.checked = true;
      }
      update();
    }));
    form.querySelector('[data-select-all]')?.addEventListener('click', () => {
      choices.forEach(choice => { choice.querySelector('input[type="checkbox"]').checked = true; });
      if (form.matches('[data-variant-scope]')) {
        const selectedScope = form.querySelector('input[name="scope"][value="SELECTED"]');
        if (selectedScope) selectedScope.checked = true;
      }
      update();
    });
    form.querySelector('[data-clear-all]')?.addEventListener('click', () => {
      choices.forEach(choice => { choice.querySelector('input[type="checkbox"]').checked = false; });
      if (form.matches('[data-variant-scope]')) {
        const allScope = form.querySelector('input[name="scope"][value="ALL"]');
        if (allScope) allScope.checked = true;
      }
      update();
    });
    form.addEventListener('submit', event => {
      const requiredProductChoices = form.querySelectorAll('input[name="productIds"]');
      if (requiredProductChoices.length > 0 && !form.querySelector('input[name="productIds"]:checked')) {
        event.preventDefault();
        window.showToast('Vui lòng chọn ít nhất một sản phẩm.', 'warning');
      }
    });
    update();
  });

  document.querySelectorAll('form[data-variant-scope]').forEach(form => {
    const picker = form.querySelector('[data-variant-picker]');
    if (!picker) return;
    const options = Array.from(form.querySelectorAll('[data-variant-option]'));
    const sizeButtons = Array.from(form.querySelectorAll('[data-size-choice]'));
    const colorButtons = Array.from(form.querySelectorAll('[data-color-choice]'));
    const count = form.querySelector('[data-selected-count]');
    const list = form.querySelector('[data-selected-variant-list]');
    const empty = form.querySelector('[data-selected-variant-empty]');
    const colorHelp = form.querySelector('[data-color-help]');
    const selectedScope = form.querySelector('input[name="scope"][value="SELECTED"]');
    const allScope = form.querySelector('input[name="scope"][value="ALL"]');
    let activeSize = options.find(option => option.checked)?.dataset.sizeId || null;

    const chooseSelectedScope = () => {
      if (selectedScope) selectedScope.checked = true;
    };
    const matchingOption = (sizeId, colorId) => options.find(option =>
      option.dataset.sizeId === sizeId && option.dataset.colorId === colorId);
    const render = () => {
      const selected = options.filter(option => option.checked);
      if (count) count.textContent = String(selected.length);

      sizeButtons.forEach(button => {
        const sizeId = button.dataset.sizeId;
        const selectedForSize = selected.filter(option => option.dataset.sizeId === sizeId).length;
        button.classList.toggle('is-active', activeSize === sizeId);
        const badge = button.querySelector('[data-size-selected-count]');
        if (badge) badge.textContent = selectedForSize > 0 ? String(selectedForSize) : '';
      });

      colorButtons.forEach(button => {
        const option = activeSize ? matchingOption(activeSize, button.dataset.colorId) : null;
        button.disabled = !option;
        button.classList.toggle('is-selected', Boolean(option?.checked));
        button.setAttribute('aria-pressed', String(Boolean(option?.checked)));
      });
      if (colorHelp) {
        colorHelp.textContent = activeSize
          ? 'Nhấn màu một lần để chọn, nhấn lại để bỏ chọn.'
          : 'Hãy chọn kích cỡ để xem các màu có sẵn.';
      }

      if (list) {
        list.replaceChildren();
        selected.forEach(option => {
          const item = document.createElement('span');
          item.className = 'selected-variant-item';
          const label = document.createElement('span');
          label.textContent = 'Size ' + option.dataset.sizeName + ' · ' + option.dataset.colorName;
          const remove = document.createElement('button');
          remove.type = 'button';
          remove.setAttribute('aria-label', 'Bỏ Size ' + option.dataset.sizeName + ', màu ' + option.dataset.colorName);
          remove.textContent = '×';
          remove.addEventListener('click', () => {
            option.checked = false;
            render();
          });
          item.append(label, remove);
          list.append(item);
        });
      }
      if (empty) empty.hidden = selected.length > 0;
    };

    sizeButtons.forEach(button => button.addEventListener('click', () => {
      activeSize = activeSize === button.dataset.sizeId ? null : button.dataset.sizeId;
      render();
    }));
    colorButtons.forEach(button => button.addEventListener('click', () => {
      if (!activeSize) return;
      const option = matchingOption(activeSize, button.dataset.colorId);
      if (!option) return;
      option.checked = !option.checked;
      chooseSelectedScope();
      render();
    }));
    form.querySelector('[data-variant-select-all]')?.addEventListener('click', () => {
      options.forEach(option => { option.checked = true; });
      activeSize = activeSize || sizeButtons[0]?.dataset.sizeId || null;
      chooseSelectedScope();
      render();
    });
    form.querySelector('[data-variant-clear-all]')?.addEventListener('click', () => {
      options.forEach(option => { option.checked = false; });
      activeSize = null;
      if (allScope) allScope.checked = true;
      render();
    });
    allScope?.addEventListener('change', () => {
      if (!allScope.checked) return;
      options.forEach(option => { option.checked = false; });
      activeSize = null;
      render();
    });
    form.addEventListener('submit', event => {
      if (selectedScope?.checked && !options.some(option => option.checked)) {
        event.preventDefault();
        window.showToast('Vui lòng chọn ít nhất một tổ hợp kích cỡ và màu.', 'warning');
      }
    });
    render();
  });
  const normalizeSearch = value => (value || '')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('vi').trim();
  document.querySelectorAll('form[data-local-filter]').forEach(form => {
    const section = form.closest('section');
    const rows = Array.from(section?.querySelectorAll('[data-filter-row]') || []);
    const keyword = form.querySelector('[name="keyword"]');
    const status = form.querySelector('[name="status"]');
    const applyLocalFilter = () => {
      const term = normalizeSearch(keyword?.value);
      rows.forEach(row => {
        const matchesText = !term || normalizeSearch(row.dataset.search).includes(term);
        const matchesStatus = !status?.value || row.dataset.status === status.value;
        row.hidden = !(matchesText && matchesStatus);
      });
    };
    keyword?.addEventListener('input', applyLocalFilter);
    status?.addEventListener('change', applyLocalFilter);
  });

  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('sidebarOverlay');
  const toggle = document.getElementById('sidebar-toggle');
  const close = () => { sidebar?.classList.remove('show'); overlay?.classList.remove('show'); toggle?.setAttribute('aria-expanded', 'false'); };
  toggle?.addEventListener('click', () => {
    if (window.innerWidth <= 1200) {
      sidebar?.classList.toggle('show'); overlay?.classList.toggle('show');
      toggle.setAttribute('aria-expanded', String(sidebar?.classList.contains('show')));
    } else {
      document.body.classList.toggle('sidebar-collapsed');
      toggle.setAttribute('aria-expanded', String(!document.body.classList.contains('sidebar-collapsed')));
    }
  });
  overlay?.addEventListener('click', close);
  document.addEventListener('keydown', event => { if (event.key === 'Escape') close(); });

  // ==========================================================================
  // Unified Sub-Form Quick View System (Dùng chung một code cho toàn bộ sub-form)
  // ==========================================================================
  const quickviewPanel = document.getElementById('appQuickviewPanel') 
    || document.getElementById('orderQuickviewPanel') 
    || document.getElementById('receiptQuickviewPanel') 
    || document.getElementById('promotionQuickviewPanel');

  const quickviewBackdrop = document.getElementById('appQuickviewBackdrop')
    || document.getElementById('orderQuickviewBackdrop')
    || document.getElementById('receiptQuickviewBackdrop')
    || document.getElementById('promotionQuickviewBackdrop');

  const quickviewBody = document.getElementById('appQuickviewBody')
    || document.getElementById('orderQuickviewBody')
    || document.getElementById('receiptQuickviewBody')
    || document.getElementById('promotionQuickviewBody');

  const quickviewTitle = document.getElementById('appQuickviewTitle')
    || document.getElementById('orderQuickviewTitle')
    || document.getElementById('receiptQuickviewTitle')
    || document.getElementById('promotionQuickviewTitle');

  const quickviewSubtitle = document.getElementById('appQuickviewSubtitle')
    || document.getElementById('orderQuickviewSubtitle')
    || document.getElementById('receiptQuickviewSubtitle')
    || document.getElementById('promotionQuickviewSubtitle');

  const quickviewIcon = document.getElementById('appQuickviewIcon')
    || document.querySelector('.app-quickview-icon')
    || document.querySelector('.order-quickview-icon')
    || document.querySelector('.receipt-quickview-icon')
    || document.querySelector('.promotion-quickview-icon');

  const quickviewFullLink = document.getElementById('appQuickviewFullLink')
    || document.getElementById('orderQuickviewFullLink')
    || document.getElementById('receiptQuickviewFullLink')
    || document.getElementById('promotionQuickviewFullLink');

  const quickviewClose = document.getElementById('appQuickviewClose')
    || document.getElementById('orderQuickviewClose')
    || document.getElementById('receiptQuickviewClose')
    || document.getElementById('promotionQuickviewClose');

  let currentQuickviewUrl = null;

  const syncQuickviewTop = (force = false) => {
    if (!quickviewPanel) return;
    const isForce = force === true;
    if (!isForce && !quickviewPanel.classList.contains('is-open')) return;
    if (window.innerWidth >= 992) {
      const listForm = document.querySelector('.order-filter-card')
        || document.querySelector('.receipt-filter-card')
        || document.querySelector('form[data-live-filter]')
        || document.querySelector('form.card')
        || document.querySelector('.table-responsive')?.closest('.card');
      const navbar = document.querySelector('.navbar-custom');
      const navbarBottom = navbar ? Math.max(0, Math.round(navbar.getBoundingClientRect().bottom)) : 70;
      if (listForm) {
        const rect = listForm.getBoundingClientRect();
        const targetTop = Math.max(navbarBottom, Math.round(rect.top));
        quickviewPanel.style.top = `${targetTop}px`;
      } else {
        quickviewPanel.style.top = `${navbarBottom}px`;
      }
    } else {
      quickviewPanel.style.top = '0';
    }
  };

  const closeQuickview = () => {
    if (!quickviewPanel) return;
    quickviewPanel.classList.remove('is-open');
    quickviewBackdrop?.classList.remove('show');
    document.querySelectorAll('.quickview-row-active, .order-row-active, .receipt-row-active, .promotion-row-active')
      .forEach(r => r.classList.remove('quickview-row-active', 'order-row-active', 'receipt-row-active', 'promotion-row-active'));
    currentQuickviewUrl = null;
    setTimeout(() => {
      if (!quickviewPanel.classList.contains('is-open')) {
        quickviewPanel.style.top = '';
      }
    }, 320);
  };

  const bindQuickviewInnerForms = (container, url) => {
    // 1. Bind order status transition quick form
    const form = container.querySelector('.order-status-quick-form');
    if (form) {
      form.addEventListener('submit', async event => {
        event.preventDefault();
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn ? submitBtn.innerHTML : '';
        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang lưu...';
        }
        try {
          const formData = new FormData(form);
          const response = await fetch(form.action, {
            method: 'POST',
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            body: formData
          });
          const data = await response.json().catch(() => null);
          if (response.ok && data?.success) {
            const orderId = form.dataset.orderId;
            const row = document.getElementById(`order-row-${orderId}`);
            if (row) {
              const badge = row.querySelector('.order-status-badge');
              if (badge) {
                const statusName = data.status;
                const statusLabel = form.querySelector(`option[value="${statusName}"]`)?.textContent?.trim() || statusName;
                badge.textContent = statusLabel;
                badge.className = 'order-status-badge badge ' + (
                  statusName === 'DELIVERED' ? 'bg-success' :
                  (statusName === 'CANCELLED' || statusName === 'RETURNED') ? 'bg-danger' :
                  statusName === 'PENDING' ? 'bg-warning text-dark' :
                  statusName === 'CONFIRMED' ? 'bg-info text-dark' : 'bg-primary'
                );
              }
            }
            window.showToast('Cập nhật trạng thái đơn hàng thành công!', 'success');
            loadQuickview({ url, targetRow: row });
          } else {
            window.showToast(data?.message || 'Không thể cập nhật trạng thái đơn hàng.', 'error');
            if (submitBtn) {
              submitBtn.disabled = false;
              submitBtn.innerHTML = originalText;
            }
          }
        } catch (err) {
          window.showToast('Lỗi kết nối khi cập nhật đơn hàng.', 'error');
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
          }
        }
      });
    }

    // 2. Bind close buttons inside the loaded content
    container.querySelectorAll('.btn-quickview-close, .btn-order-quickview-close, .btn-receipt-quickview-close, .btn-promotion-quickview-close')
      .forEach(btn => btn.addEventListener('click', closeQuickview));
  };

  const loadQuickview = async ({ url, title, subtitle, icon, fullLink, targetRow }) => {
    if (!quickviewPanel || !quickviewBody || !url) return;
    currentQuickviewUrl = url;

    document.querySelectorAll('.quickview-row-active, .order-row-active, .receipt-row-active, .promotion-row-active')
      .forEach(r => r.classList.remove('quickview-row-active', 'order-row-active', 'receipt-row-active', 'promotion-row-active'));
    if (targetRow) {
      targetRow.classList.add('quickview-row-active');
    }

    syncQuickviewTop(true);
    quickviewPanel.classList.add('is-open');
    quickviewBackdrop?.classList.add('show');
    requestAnimationFrame(() => syncQuickviewTop(true));

    if (quickviewTitle && title) quickviewTitle.textContent = title;
    if (quickviewSubtitle) quickviewSubtitle.textContent = subtitle || 'Đang tải thông tin chi tiết...';
    if (quickviewIcon && icon) {
      quickviewIcon.innerHTML = `<i class="bi ${icon}"></i>`;
    }
    if (quickviewFullLink) {
      if (fullLink) {
        quickviewFullLink.href = fullLink;
        quickviewFullLink.style.display = '';
      } else {
        quickviewFullLink.style.display = 'none';
      }
    }

    quickviewBody.innerHTML = `
      <div class="text-center py-5 text-muted">
        <div class="spinner-border text-primary mb-3" style="width: 2.2rem; height: 2.2rem;" role="status">
          <span class="visually-hidden">Đang tải...</span>
        </div>
        <p class="mb-0 fw-semibold">Đang tải chi tiết...</p>
      </div>
    `;

    try {
      const response = await fetch(url, {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      });
      if (!response.ok) throw new Error('Không thể tải chi tiết');
      const html = await response.text();
      if (currentQuickviewUrl === url) {
        quickviewBody.innerHTML = html;
        if (quickviewSubtitle && !subtitle) quickviewSubtitle.textContent = 'Thông tin chi tiết';
        bindQuickviewInnerForms(quickviewBody, url);
      }
    } catch (e) {
      if (currentQuickviewUrl === url) {
        quickviewBody.innerHTML = `
          <div class="alert alert-danger my-4" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            Không thể tải thông tin chi tiết. Vui lòng thử lại.
          </div>
        `;
        if (quickviewSubtitle) quickviewSubtitle.textContent = 'Lỗi tải dữ liệu';
      }
    }
  };

  // Expose to window for global programmatic use
  window.AppQuickview = {
    open: loadQuickview,
    close: closeQuickview,
    syncTop: syncQuickviewTop
  };

  if (quickviewPanel) {
    quickviewClose?.addEventListener('click', closeQuickview);
    quickviewBackdrop?.addEventListener('click', closeQuickview);

    document.addEventListener('click', event => {
      // 1. Explicit button click with .btn-quickview or backward-compatible classes
      const previewBtn = event.target.closest('.btn-quickview, .btn-order-preview, .btn-receipt-preview, .btn-promotion-preview');
      if (previewBtn) {
        event.preventDefault();
        const row = previewBtn.closest('tr');
        const url = previewBtn.dataset.quickviewUrl
          || previewBtn.dataset.previewUrl
          || (previewBtn.dataset.orderId ? `/admin/orders/${previewBtn.dataset.orderId}/preview` : null)
          || (previewBtn.dataset.receiptId ? `/admin/purchase-receipts/${previewBtn.dataset.receiptId}/preview` : null);
        const title = previewBtn.dataset.quickviewTitle
          || previewBtn.dataset.title
          || (previewBtn.dataset.orderId ? `Đơn hàng #${previewBtn.dataset.orderId}` : null)
          || (previewBtn.dataset.receiptId ? `Phiếu nhập #PN-${previewBtn.dataset.receiptId}` : null);
        const subtitle = previewBtn.dataset.quickviewSubtitle;
        const icon = previewBtn.dataset.quickviewIcon
          || (previewBtn.dataset.orderId ? 'bi-receipt' : (previewBtn.dataset.receiptId ? 'bi-box-seam' : 'bi-megaphone'));
        const fullLink = previewBtn.dataset.quickviewFullLink
          || previewBtn.dataset.fullLink
          || (previewBtn.dataset.orderId ? `/admin/orders/${previewBtn.dataset.orderId}` : null)
          || (previewBtn.dataset.receiptId ? `/admin/purchase-receipts/${previewBtn.dataset.receiptId}` : null);

        if (url) {
          loadQuickview({ url, title, subtitle, icon, fullLink, targetRow: row });
        }
        return;
      }

      // 2. Table row click with data-quickview-url or backward-compatible row classes
      const row = event.target.closest('tr.quickview-row, tr.order-row, tr.receipt-row, tr.promotion-row');
      if (row && !event.target.closest('a, button, input, select, textarea, label')) {
        const url = row.dataset.quickviewUrl
          || row.dataset.previewUrl
          || (row.dataset.orderId ? `/admin/orders/${row.dataset.orderId}/preview` : null)
          || (row.dataset.receiptId ? `/admin/purchase-receipts/${row.dataset.receiptId}/preview` : null);
        const title = row.dataset.quickviewTitle
          || row.dataset.title
          || (row.dataset.orderId ? `Đơn hàng #${row.dataset.orderId}` : null)
          || (row.dataset.receiptId ? `Phiếu nhập #PN-${row.dataset.receiptId}` : null);
        const subtitle = row.dataset.quickviewSubtitle;
        const icon = row.dataset.quickviewIcon
          || (row.dataset.orderId ? 'bi-receipt' : (row.dataset.receiptId ? 'bi-box-seam' : 'bi-megaphone'));
        const fullLink = row.dataset.quickviewFullLink
          || row.dataset.fullLink
          || (row.dataset.orderId ? `/admin/orders/${row.dataset.orderId}` : null)
          || (row.dataset.receiptId ? `/admin/purchase-receipts/${row.dataset.receiptId}` : null);

        if (url) {
          loadQuickview({ url, title, subtitle, icon, fullLink, targetRow: row });
        }
      }
    });

    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && quickviewPanel.classList.contains('is-open')) {
        closeQuickview();
      }
    });

    window.addEventListener('scroll', syncQuickviewTop, { passive: true });
    window.addEventListener('resize', syncQuickviewTop, { passive: true });
  }

});
