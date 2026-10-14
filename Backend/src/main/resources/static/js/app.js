import { api } from "./core/api.js";
import { applyRoleNavigation, currentUser, logout, requireAuth } from "./auth.js";

const page = document.body.dataset.page;
const title = document.querySelector("[data-title]");
const output = document.querySelector("[data-output]");

function value(object, key, fallback = 0) {
  return object && object[key] !== undefined && object[key] !== null ? object[key] : fallback;
}

function renderRows(rows, columns) {
  const table = document.createElement("table");
  const head = document.createElement("tr");
  columns.forEach(([label]) => { const cell = document.createElement("th"); cell.textContent = label; head.append(cell); });
  const thead = document.createElement("thead"); thead.append(head); table.append(thead);
  const tbody = document.createElement("tbody");
  rows.forEach((row) => {
    const tr = document.createElement("tr");
    columns.forEach(([, key]) => { const cell = document.createElement("td"); cell.textContent = String(value(row, key, "")); tr.append(cell); });
    tbody.append(tr);
  });
  table.append(tbody);
  output.replaceChildren(table);
}

async function load() {
  requireAuth();
  try {
    const user = await currentUser();
    applyRoleNavigation(user.role);
    document.querySelector("[data-user]").textContent = `${user.username} · ${user.role}`;
    if (page === "dashboard") {
      title.textContent = "Operations dashboard";
      const data = await api("/dashboard/summary");
      const metrics = [["Active products", "totalActiveProducts"], ["Stock units", "totalStockQuantity"], ["Low stock", "lowStockProductsCount"], ["Critical alerts", "criticalAlertsCount"], ["IN today", "stockInToday"], ["OUT today", "stockOutToday"]];
      metrics.forEach(([label, key]) => { const card = document.createElement("article"); card.className = "card"; card.textContent = label; const metric = document.createElement("div"); metric.className = "metric"; metric.textContent = value(data, key); card.append(metric); document.querySelector("[data-metrics]").append(card); });
    } else if (page === "inventory") {
      title.textContent = "Inventory"; const data = await api("/inventory"); renderRows(data.content || data, [["Product", "productName"], ["SKU", "sku"], ["Quantity", "quantity"], ["Rack", "rackCode"]]);
    } else if (page === "alerts") {
      title.textContent = "Alerts"; const data = await api("/alerts"); const rows = data.content || data;
      renderRows(rows, [["Product", "productName"], ["Severity", "severity"], ["Status", "status"], ["Message", "message"]]);
      const body = output.querySelector("tbody");
      rows.forEach((row, index) => {
        const cell = document.createElement("td");
        if (row.status !== "RESOLVED") {
          const acknowledge = document.createElement("button");
          acknowledge.className = "button secondary"; acknowledge.textContent = "Acknowledge";
          acknowledge.addEventListener("click", async () => { await api(`/alerts/${row.id}/acknowledge`, { method: "PUT", body: "{}" }); load(); });
          cell.append(acknowledge);
        }
        if (row.status !== "RESOLVED") {
          const resolve = document.createElement("button");
          resolve.className = "button danger"; resolve.textContent = "Resolve";
          resolve.addEventListener("click", async () => { await api(`/alerts/${row.id}/resolve`, { method: "PUT", body: "{}" }); load(); });
          cell.append(resolve);
        }
        body.rows[index].append(cell);
      });
    } else if (page === "activity") {
      title.textContent = "Activity"; const data = await api("/audit-logs"); renderRows(data.content || data, [["Action", "action"], ["Entity", "entity"], ["Description", "description"], ["Created", "createdAt"]]);
    } else if (page === "products" || page === "suppliers" || page === "users") {
      const endpoint = page === "products" ? "/products" : page === "suppliers" ? "/suppliers" : "/admin/users";
      const data = await api(endpoint);
      renderRows(data.content || data, [["Name", "name"], ["Username", "username"], ["Email", "email"], ["SKU", "sku"], ["Status", "active"]]);
    } else {
      title.textContent = page.replace("-", " "); output.textContent = "Use this page to manage warehouse operations through the API.";
    }
  } catch (error) { output.textContent = error.message; output.className = "error"; }
}

document.querySelector("[data-logout]")?.addEventListener("click", logout);
load();
