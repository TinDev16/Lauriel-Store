(() => {
  const revenueHost = document.getElementById("revenueChart");
  const statusHost = document.getElementById("statusChart");
  if (!revenueHost || !statusHost) return;

  const revenue = [...document.querySelectorAll(".revenue-point")].map(node => ({
    label: node.dataset.label,
    value: Number(node.dataset.value || 0),
    orders: Number(node.dataset.count || 0)
  }));
  const statuses = [...document.querySelectorAll(".status-point")].map(node => ({
    label: node.dataset.label,
    value: Number(node.dataset.value || 0)
  }));

  if (window.ApexCharts) {
    statusHost.classList.add("statistics-apex");
    const money = value => new Intl.NumberFormat("vi-VN").format(value) + " ₫";
    const statusLabels = {
      PENDING: "Chờ xác nhận", CONFIRMED: "Đã xác nhận", PROCESSING: "Đang xử lý",
      SHIPPING: "Đang giao", DELIVERED: "Đã giao", CANCELLED: "Đã hủy", RETURNED: "Đã trả hàng"
    };
    new window.ApexCharts(revenueHost, {
      chart: { type: "line", height: 280, toolbar: { show: false } },
      series: [{ name: "Doanh thu", data: revenue.map(point => point.value) }],
      colors: ["#087f63"], stroke: { curve: "straight", width: 3 },
      xaxis: { categories: revenue.map(point => point.label), tickAmount: 8 },
      yaxis: { labels: { formatter: value => new Intl.NumberFormat("vi-VN", { notation: "compact" }).format(value) } },
      tooltip: { y: { formatter: money } },
      noData: { text: "Chưa có đơn đã giao" }
    }).render();
    new window.ApexCharts(statusHost, {
      chart: { type: "donut", height: 280 },
      series: statuses.map(status => status.value),
      labels: statuses.map(status => statusLabels[status.label] || status.label),
      colors: ["#f0a44b", "#4e9edd", "#8b74dc", "#4fc3b1", "#087f63", "#df6b6b", "#64748b"],
      legend: { show: false },
      noData: { text: "Chưa có đơn hàng" }
    }).render();
    return;
  }
  const svgNs = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(svgNs, "svg");
  svg.setAttribute("viewBox", "0 0 900 280");
  svg.setAttribute("role", "presentation");
  const draw = (tag, attributes, label) => {
    const node = document.createElementNS(svgNs, tag);
    for (const [key, value] of Object.entries(attributes)) node.setAttribute(key, String(value));
    if (label !== undefined) node.textContent = label;
    svg.appendChild(node);
    return node;
  };
  const max = Math.max(1, ...revenue.map(point => point.value));
  const x = index => 55 + index * 810 / Math.max(1, revenue.length - 1);
  const y = value => 230 - value * 190 / max;
  for (let tick = 0; tick <= 4; tick++) {
    const level = max * tick / 4;
    const py = y(level);
    draw("line", { x1: 55, y1: py, x2: 870, y2: py, stroke: "#e4e9ec" });
    draw("text", { x: 48, y: py + 4, "text-anchor": "end", fill: "#617178", "font-size": 11 },
      new Intl.NumberFormat("vi-VN", { notation: "compact" }).format(level));
  }
  if (revenue.length) {
    const points = revenue.map((point, index) => x(index) + "," + y(point.value)).join(" ");
    draw("polyline", { points, fill: "none", stroke: "#087f63", "stroke-width": 3,
      "stroke-linejoin": "round", "stroke-linecap": "round" });
    const step = Math.max(1, Math.ceil(revenue.length / 8));
    revenue.forEach((point, index) => {
      if (index % step === 0 || index === revenue.length - 1) {
        draw("text", { x: x(index), y: 253, "text-anchor": "middle", fill: "#617178",
          "font-size": 11 }, point.label);
      }
      if (revenue.length <= 35) {
        const dot = draw("circle", { cx: x(index), cy: y(point.value), r: 4, fill: "#087f63" });
        const title = document.createElementNS(svgNs, "title");
        title.textContent = point.label + ": " +
          new Intl.NumberFormat("vi-VN").format(point.value) + " ₫ · " + point.orders + " đơn";
        dot.appendChild(title);
      }
    });
  }
  revenueHost.appendChild(svg);

  const total = statuses.reduce((sum, status) => sum + status.value, 0);
  const colors = ["#f0a44b", "#4e9edd", "#8b74dc", "#4fc3b1", "#087f63", "#df6b6b", "#64748b"];
  if (total === 0) {
    statusHost.style.background = "#e4e9ec";
  } else {
    let at = 0;
    const slices = statuses.map((status, index) => {
      const start = at;
      at += status.value * 100 / total;
      return colors[index % colors.length] + " " + start + "% " + at + "%";
    });
    statusHost.style.background = "conic-gradient(" + slices.join(",") + ")";
  }
  const center = document.createElement("div");
  center.className = "statistics-donut-center";
  center.textContent = new Intl.NumberFormat("vi-VN").format(total) + " đơn";
  statusHost.appendChild(center);
})();