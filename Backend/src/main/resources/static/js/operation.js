import { api } from "./core/api.js";
import { requireAuth } from "./auth.js";
requireAuth();
const form = document.querySelector("form");
form?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = document.querySelector("[data-message]");
  const values = Object.fromEntries(new FormData(form).entries());
  values.productId = values.productId || undefined;
  values.quantity = Number(values.quantity);
  try {
    const path = form.dataset.endpoint;
    await api(path, { method: "POST", body: JSON.stringify(values) });
    message.textContent = "Operation completed successfully.";
    form.reset();
  } catch (error) { message.textContent = error.message; message.className = "error"; }
});
