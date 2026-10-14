import { api } from "./core/api.js";
import { requireAuth } from "./auth.js";
requireAuth();
document.querySelectorAll("[data-action]").forEach((button) => button.addEventListener("click", async () => {
  const message = document.querySelector("[data-message]");
  try { await api(`/alerts/${button.dataset.id}/${button.dataset.action}`, { method: "PUT", body: "{}" }); message.textContent = "Alert updated."; }
  catch (error) { message.textContent = error.message; message.className = "error"; }
}));
