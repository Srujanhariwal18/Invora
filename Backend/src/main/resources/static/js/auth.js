import { api, clearToken, setToken, token } from "./core/api.js";

export function requireAuth() {
  if (!token()) window.location.assign("/login.html");
}

export function applyRoleNavigation(role) {
  document.querySelectorAll("[data-roles]").forEach((element) => {
    const roles = element.dataset.roles.split(",");
    element.hidden = !roles.includes(role);
  });
}

export async function currentUser() {
  return api("/auth/me");
}

export async function login(username, password) {
  const result = await api("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password })
  });
  setToken(result.token);
  window.location.assign("/dashboard.html");
}

export function logout() {
  clearToken();
  window.location.assign("/login.html");
}
