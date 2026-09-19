document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!window.confirm(form.dataset.confirm)) event.preventDefault();
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
