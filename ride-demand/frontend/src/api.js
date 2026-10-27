// all calls to the Spring Boot backend live here

async function post(url, body, extraHeaders = {}) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...extraHeaders },
    body: JSON.stringify(body),
    credentials: "include",
  });

  if (!res.ok) {
    throw new Error(await getErrorMessage(res));
  }
  return res.status === 204 ? null : res.json();
}

async function getErrorMessage(res) {
  try {
    const data = await res.json();
    return data.message || data.detail || "Something went wrong. Please try again.";
  } catch {
    return "Something went wrong. Please try again.";
  }
}

export async function getCurrentDriver() {
  const res = await fetch("/api/auth/me", { credentials: "include" });
  if (res.status === 401) return null;
  if (!res.ok) throw new Error(await getErrorMessage(res));
  return res.json();
}

export function registerDriver({ username, password }) {
  return post("/api/auth/register", { username, password });
}

export function loginDriver({ username, password }) {
  return post("/api/auth/login", { username, password });
}

export function logoutDriver() {
  return post("/api/auth/logout", {});
}

export function predictDemand({ latitude, longitude, vehicle, time }) {
  return post("/api/demand/predict", { latitude, longitude, vehicle, time });
}

export function reportUnmetDemand({ area, vehicleType }) {
  return post("/api/reports/unmet-demand", { area, vehicleType });
}

export function reportRideFound({ vehicleType, latitude, longitude }) {
  return post("/api/observations/ride-found", {
    vehicleType,
    latitude,
    longitude,
    consent: true,
  });
}
