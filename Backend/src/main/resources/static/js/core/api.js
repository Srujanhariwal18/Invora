const API_ROOT = "/api";

export function token() {
  return sessionStorage.getItem("invora_token");
}

export function setToken(value) {
  sessionStorage.setItem("invora_token", value);
}

export function clearToken() {
  sessionStorage.removeItem("invora_token");
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const jwt = token();
  if (jwt) headers.set("Authorization", `Bearer ${jwt}`);
  const response = await fetch(`${API_ROOT}${path}`, { ...options, headers });
  if (response.status === 401) {
    clearToken();
    window.location.assign("/login.html");
    throw new Error("Your session has expired. Please sign in again.");
  }
  if (response.status === 403) throw new Error("You do not have permission to perform this action.");
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
}
