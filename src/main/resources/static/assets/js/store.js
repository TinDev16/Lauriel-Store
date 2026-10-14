document.addEventListener('DOMContentLoaded', () => {
  const menu = document.querySelector('[data-store-menu]');
  const nav = document.querySelector('[data-store-nav]');
  if (menu && nav) menu.addEventListener('click', () => nav.classList.toggle('open'));
  document.querySelectorAll('[data-clear-search]').forEach(button => button.addEventListener('click', () => {
    const input = button.closest('form')?.querySelector('input[name="keyword"]'); if (input) { input.value = ''; input.form.submit(); }
  }));
  const filterToggle = document.querySelector('[data-filter-toggle]');
  const filterPanel = document.querySelector('[data-filter-panel]');
  if (filterToggle && filterPanel) filterToggle.addEventListener('click', () => filterPanel.classList.toggle('open'));
  document.querySelectorAll('[data-dismiss-alert]').forEach(button => button.addEventListener('click', () => button.closest('.store-toast')?.remove()));
  document.querySelectorAll('[data-cart-minus],[data-cart-plus]').forEach(button => button.addEventListener('click', () => {
    const input = button.closest('form').querySelector('input[name="quantity"]');
    const delta = button.hasAttribute('data-cart-plus') ? 1 : -1;
    input.value = Math.max(Number(input.min || 1), Math.min(Number(input.max || 999), Number(input.value || 1) + delta));
  }));
  document.querySelectorAll('form[data-confirm]').forEach(form => form.addEventListener('submit', event => {
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  }));
  document.querySelectorAll('[data-submit-once]').forEach(form => form.addEventListener('submit', () => {
    const button = form.querySelector('[data-submit-button]'); if (button) { button.disabled = true; button.classList.add('loading'); button.querySelector('span').textContent = 'Đang đặt hàng…'; }
  }));
  window.setTimeout(() => document.querySelectorAll('.store-toast').forEach(alert => alert.classList.add('hide')), 4500);
});
