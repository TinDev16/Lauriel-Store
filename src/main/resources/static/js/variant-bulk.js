(() => {
  const form = document.querySelector("form[data-bulk-variants]");
  if (!form) return;

  const inputs = name => Array.from(form.querySelectorAll(`input[name="${name}"]`));
  const allChoices = () => [...inputs("sizeIds"), ...inputs("colorIds")];
  const checkedCount = name => inputs(name).filter(input => input.checked).length;
  const sizeCount = form.querySelector("[data-size-count]");
  const colorCount = form.querySelector("[data-color-count]");
  const combinationCount = form.querySelector("[data-combination-count]");
  const submit = form.querySelector("[data-submit-variants]");

  function syncHighlight(input) {
    Array.from(input.labels || []).forEach(label => {
      label.classList.toggle("btn-dark", input.checked);
      label.classList.toggle("btn-outline-dark", !input.checked);
      label.classList.toggle("active", input.checked);
      label.classList.toggle("is-selected", input.checked);
      label.setAttribute("aria-pressed", String(input.checked));
    });
  }

  function update() {
    allChoices().forEach(syncHighlight);
    const sizes = checkedCount("sizeIds");
    const colors = checkedCount("colorIds");
    sizeCount.textContent = sizes;
    colorCount.textContent = colors;
    combinationCount.textContent = sizes * colors;
    submit.disabled = sizes === 0 || colors === 0;
  }

  form.querySelectorAll("[data-select-all]").forEach(button => {
    button.addEventListener("click", () => {
      inputs(button.dataset.selectAll).forEach(input => { input.checked = true; });
      update();
    });
  });

  form.querySelectorAll("[data-clear-all]").forEach(button => {
    button.addEventListener("click", () => {
      inputs(button.dataset.clearAll).forEach(input => { input.checked = false; });
      update();
    });
  });

  allChoices().forEach(input => input.addEventListener("change", update));
  update();
})();