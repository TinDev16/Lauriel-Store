document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
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
        window.alert('Vui lòng chọn ít nhất một sản phẩm.');
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
        window.alert('Vui lòng chọn ít nhất một tổ hợp kích cỡ và màu.');
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
});
