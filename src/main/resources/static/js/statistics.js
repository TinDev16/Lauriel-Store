(() => {
  const read = (selector, valueName = "value") => [...document.querySelectorAll(selector)].map(node => ({
    label: node.dataset.label,
    value: Number(node.dataset[valueName] || 0),
    count: Number(node.dataset.count || 0)
  }));
  const revenue = read(".revenue-point");
  const statuses = read(".status-point");
  const customers = read(".customer-point");
  const purchases = read(".purchase-point");
  const products = [...document.querySelectorAll(".product-point")].map(node => ({
    label: node.dataset.label,
    quantity: Number(node.dataset.quantity || 0),
    revenue: Number(node.dataset.revenue || 0)
  }));
  const productsByRevenue = read(".product-revenue-point");
  const money = value => new Intl.NumberFormat("vi-VN").format(value) + " ₫";
  const compact = value => new Intl.NumberFormat("vi-VN", { notation: "compact" }).format(value);
  const statusLabels = {
    PENDING: "Chờ xác nhận", CONFIRMED: "Đã xác nhận", PROCESSING: "Đang xử lý",
    SHIPPING: "Đang giao", DELIVERED: "Đã giao", CANCELLED: "Đã hủy", RETURNED: "Đã trả hàng"
  };
  const empty = (host, text) => {
    if (!host) return;
    host.innerHTML = `<div class="stats-chart-empty"><i class="bi bi-bar-chart"></i><span>${text}</span></div>`;
  };
  const hasPositive = items => items.some(item => item.value > 0);

  if (!window.ApexCharts) {
    const revenueHost = document.getElementById("revenueChart");
    if (revenueHost && hasPositive(revenue)) {
      const max = Math.max(...revenue.map(item => item.value), 1);
      const points = revenue.map((item, index) => {
        const x = 32 + index * 836 / Math.max(1, revenue.length - 1);
        return `${x},${250 - item.value * 205 / max}`;
      }).join(" ");
      revenueHost.innerHTML = `<svg viewBox="0 0 900 280" role="img" aria-label="Doanh thu theo thời gian"><line x1="32" y1="250" x2="868" y2="250" stroke="#dfe7e3"/><polyline points="${points}" fill="none" stroke="#087f63" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
    } else if (revenueHost) empty(revenueHost, "Chưa có đơn đã giao trong kỳ");
    const statusHost = document.getElementById("statusChart");
    if (statusHost && hasPositive(statuses)) {
      const total = statuses.reduce((sum, item) => sum + item.value, 0);
      let current = 0;
      const colors = ["#f59e0b", "#3b82f6", "#8b5cf6", "#14b8a6", "#087f63", "#ef4444", "#64748b"];
      const slices = statuses.map((item, index) => {
        const start = current; current += item.value * 100 / total;
        return `${colors[index % colors.length]} ${start}% ${current}%`;
      });
      statusHost.style.background = `conic-gradient(${slices.join(",")})`;
      statusHost.innerHTML = `<div class="statistics-donut-center">${new Intl.NumberFormat("vi-VN").format(total)} đơn</div>`;
    } else if (statusHost) empty(statusHost, "Chưa có đơn hàng trong kỳ");
    document.querySelectorAll("#customerChart,#purchaseChart,#productQuantityChart,#productRevenueChart")
      .forEach(host => empty(host, "Không thể tải thư viện biểu đồ."));
    return;
  }

  const line = (id, items, name, formatter = compact, emptyText = "Chưa có dữ liệu trong kỳ") => {
    const host = document.getElementById(id);
    if (!host) return;
    if (!hasPositive(items)) return empty(host, emptyText);
    new window.ApexCharts(host, {
      chart: { type: "area", height: 300, toolbar: { show: false }, fontFamily: "inherit" },
      series: [{ name, data: items.map(item => item.value) }],
      colors: ["#087f63"], stroke: { curve: "smooth", width: 3 },
      fill: { type: "gradient", gradient: { opacityFrom: .28, opacityTo: .03 } },
      dataLabels: { enabled: false },
      grid: { borderColor: "#e9efec", strokeDashArray: 4 },
      xaxis: { categories: items.map(item => item.label), tickAmount: 7, labels: { rotate: -25 } },
      yaxis: { labels: { formatter } },
      tooltip: { y: { formatter: name.includes("Doanh thu") || name.includes("Giá trị") ? money : value => new Intl.NumberFormat("vi-VN").format(value) } }
    }).render();
  };

  const revenueHost = document.getElementById("revenueChart");
  if (revenueHost) {
    if (!hasPositive(revenue)) empty(revenueHost, "Chưa có đơn đã giao trong kỳ");
    else new window.ApexCharts(revenueHost, {
      chart: { type: "line", height: 300, toolbar: { show: false }, fontFamily: "inherit" },
      series: [
        { name: "Doanh thu", type: "area", data: revenue.map(item => item.value) },
        { name: "Số đơn đã giao", type: "line", data: revenue.map(item => item.count) }
      ],
      colors: ["#087f63", "#2563eb"], stroke: { curve: "smooth", width: [3, 2] },
      fill: { type: ["gradient", "solid"], opacity: [.24, 1] }, dataLabels: { enabled: false },
      grid: { borderColor: "#e9efec", strokeDashArray: 4 },
      xaxis: { categories: revenue.map(item => item.label), tickAmount: 7, labels: { rotate: -25 } },
      yaxis: [{ labels: { formatter: compact } }, { opposite: true, labels: { formatter: value => Math.round(value) } }],
      tooltip: { shared: true, y: { formatter: (value, context) => context.seriesIndex === 0 ? money(value) : `${Math.round(value)} đơn` } }
    }).render();
  }
  line("customerChart", customers, "Khách hàng mới", compact, "Chưa có khách hàng mới trong kỳ");
  line("purchaseChart", purchases, "Giá trị nhập", compact, "Chưa có phiếu nhập hoàn tất trong kỳ");

  const statusHost = document.getElementById("statusChart");
  if (statusHost) {
    if (!hasPositive(statuses)) empty(statusHost, "Chưa có đơn hàng trong kỳ");
    else {
      statusHost.classList.add("statistics-apex");
      new window.ApexCharts(statusHost, {
        chart: { type: "donut", height: 300, fontFamily: "inherit" },
        series: statuses.map(item => item.value),
        labels: statuses.map(item => statusLabels[item.label] || item.label),
        colors: ["#f59e0b", "#3b82f6", "#8b5cf6", "#14b8a6", "#087f63", "#ef4444", "#64748b"],
        legend: { position: "bottom", fontSize: "12px" },
        plotOptions: { pie: { donut: { size: "68%", labels: { show: true, total: { show: true, label: "Tổng đơn" } } } } },
        dataLabels: { enabled: false }
      }).render();
    }
  }

  const bars = (id, items, valueKey, label, color, format) => {
    const host = document.getElementById(id);
    if (!host) return;
    const values = items.map(item => ({ label: item.label, value: item[valueKey] }));
    if (!hasPositive(values)) return empty(host, "Chưa có dữ liệu bán hàng trong kỳ");
    new window.ApexCharts(host, {
      chart: { type: "bar", height: 310, toolbar: { show: false }, fontFamily: "inherit" },
      series: [{ name: label, data: values.map(item => item.value) }],
      colors: [color], plotOptions: { bar: { borderRadius: 5, horizontal: true } },
      dataLabels: { enabled: false }, xaxis: { categories: values.map(item => item.label), labels: { formatter: format } },
      grid: { borderColor: "#e9efec", strokeDashArray: 4 },
      tooltip: { y: { formatter: format } }
    }).render();
  };
  bars("productQuantityChart", products, "quantity", "Đã bán", "#2563eb", value => new Intl.NumberFormat("vi-VN").format(value));
  bars("productRevenueChart", productsByRevenue, "value", "Doanh thu", "#087f63", money);
})();
