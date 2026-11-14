/**
 * Unified Toast Notification Engine for Lauriel Store
 * Used across the entire website (Storefront, Admin, Auth)
 *
 * Usage:
 *   window.showToast('Thao tác thành công!', 'success');
 *   window.showToast('Đã có lỗi xảy ra!', 'error');
 *   window.showToast('Cảnh báo dữ liệu!', 'warning');
 *   window.showToast('Thông tin lưu ý', 'info');
 */
(function () {
  'use strict';

  var TOAST_DURATION = 1500; // 1.5 seconds auto-dismiss

  var ICONS = {
    success: 'bi-check-circle-fill',
    error: 'bi-x-circle-fill',
    danger: 'bi-x-circle-fill',
    warning: 'bi-exclamation-triangle-fill',
    info: 'bi-info-circle-fill'
  };

  function getOrCreateContainer() {
    var container = document.getElementById('appToastContainer');
    if (!container) {
      container = document.querySelector('.app-toast-container, .store-alerts');
    }
    if (!container) {
      container = document.createElement('div');
      container.id = 'appToastContainer';
      container.className = 'app-toast-container';
      document.body.appendChild(container);
    }
    return container;
  }

  function dismissToast(toast) {
    if (!toast || toast.dataset.dismissing === 'true') return;
    toast.dataset.dismissing = 'true';
    toast.classList.add('hide');
    window.setTimeout(function () {
      if (toast && toast.parentNode) {
        toast.parentNode.removeChild(toast);
      }
    }, 280);
  }

  function bindToast(toast, duration) {
    if (!toast || toast.dataset.bound === 'true') return;
    toast.dataset.bound = 'true';

    var closeBtn = toast.querySelector('[data-dismiss-toast], [data-dismiss-alert]');
    if (closeBtn) {
      closeBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        dismissToast(toast);
      });
    }

    var delay = typeof duration === 'number' ? duration : TOAST_DURATION;
    if (delay > 0) {
      window.setTimeout(function () {
        dismissToast(toast);
      }, delay);
    }
  }

  function showToast(message, type, duration) {
    if (!message) return null;
    var normalizedType = type === 'danger' ? 'error' : (type || 'success');
    var container = getOrCreateContainer();
    var iconClass = ICONS[normalizedType] || ICONS.info;

    var toast = document.createElement('div');
    toast.className = 'app-toast app-toast-' + normalizedType;
    toast.setAttribute('role', normalizedType === 'error' ? 'alert' : 'status');

    var iconElem = document.createElement('i');
    iconElem.className = 'bi ' + iconClass;
    toast.appendChild(iconElem);

    var contentElem = document.createElement('div');
    contentElem.className = 'app-toast-content';
    var msgElem = document.createElement('span');
    msgElem.className = 'app-toast-message';
    msgElem.textContent = message;
    contentElem.appendChild(msgElem);
    toast.appendChild(contentElem);

    var closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'app-toast-close';
    closeBtn.setAttribute('data-dismiss-toast', '');
    closeBtn.setAttribute('aria-label', 'Đóng');
    closeBtn.innerHTML = '<i class="bi bi-x-lg"></i>';
    toast.appendChild(closeBtn);

    container.appendChild(toast);

    var effectiveDuration = typeof duration === 'number' ? duration : TOAST_DURATION;
    bindToast(toast, effectiveDuration);
    return toast;
  }

  // Expose globally
  window.showToast = showToast;
  window.showStoreToast = function (message, type, duration) {
    return showToast(message, type, duration);
  };

  // Initialize existing server-rendered toasts on page load
  function initExistingToasts() {
    var existing = document.querySelectorAll('.app-toast, .store-toast');
    existing.forEach(function (toast) {
      bindToast(toast, TOAST_DURATION);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initExistingToasts);
  } else {
    initExistingToasts();
  }
})();

