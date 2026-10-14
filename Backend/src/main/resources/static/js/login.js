import { login } from "./auth.js";
document.querySelector("form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const error = document.querySelector("[data-error]");
  try { await login(event.target.username.value, event.target.password.value); }
  catch (exception) { error.textContent = exception.message; }
});
