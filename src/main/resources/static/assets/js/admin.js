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
