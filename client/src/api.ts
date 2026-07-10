import type { Product, ProductCreatePayload, StockUpdatePayload, StockUpdateResponse, User, UserCreatePayload, UserPasswordResetPayload, SetupTokenResponse, SetupStatusResponse, SetupRegisterPayload } from './types';

const API_BASE = '/api';

function authHeaders(): Record<string, string> {
  const token = sessionStorage.getItem('token');
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

function redirectLogin() {
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
  window.location.href = '/login';
}

async function apiFetch(url: string, options?: RequestInit): Promise<Response> {
  const headers = { ...authHeaders(), ...options?.headers } as Record<string, string>;
  try {
    const res = await fetch(url, { ...options, headers });
    if (res.status === 401) {
      redirectLogin();
      throw { error: 'unauthorized', message: 'Sesi berakhir' };
    }
    return res;
  } catch (err: unknown) {
    if ((err as any)?.error === 'unauthorized') throw err;
    throw { error: 'network_error', message: err instanceof Error ? err.message : 'Permintaan jaringan gagal' };
  }
}

async function handleError(res: Response): Promise<never> {
  try {
    const body = await res.json();
    throw { status: res.status, ...body };
  } catch (err: unknown) {
    if ((err as any)?.status) throw err;
    throw { error: 'http_error', message: `Permintaan gagal (${res.status})` };
  }
}

export async function fetchProducts(): Promise<Product[]> {
  const res = await apiFetch(`${API_BASE}/products`);
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function fetchProduct(barcode: string): Promise<Product | null> {
  const res = await apiFetch(`${API_BASE}/products/${barcode}`);
  if (res.status === 404) return null;
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function searchProducts(query: string): Promise<Product[]> {
  const res = await apiFetch(`${API_BASE}/products/search?q=${encodeURIComponent(query)}`);
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function createProduct(data: ProductCreatePayload): Promise<Product> {
  const res = await apiFetch(`${API_BASE}/products`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function updateStock(barcode: string, payload: StockUpdatePayload): Promise<StockUpdateResponse> {
  const res = await apiFetch(`${API_BASE}/products/${barcode}/stock`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) await handleError(res);
  return res.json();
}

/* ===== Setup endpoints ===== */

export async function getSetupStatus(): Promise<SetupStatusResponse> {
  const res = await fetch(`${API_BASE}/setup/status`);
  return res.json();
}

export async function getSetupToken(): Promise<SetupTokenResponse> {
  const res = await fetch(`${API_BASE}/setup/token`);
  if (!res.ok) {
    const err = await res.json();
    throw { status: res.status, ...err };
  }
  return res.json();
}

export async function setupRegister(payload: SetupRegisterPayload): Promise<any> {
  const res = await fetch(`${API_BASE}/setup/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) {
    const err = await res.json();
    throw { status: res.status, ...err };
  }
  return res.json();
}

/* ===== User management endpoints ===== */

export async function fetchUsers(): Promise<User[]> {
  const res = await apiFetch(`${API_BASE}/users`);
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function createUser(data: UserCreatePayload): Promise<User> {
  const res = await apiFetch(`${API_BASE}/users`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function deactivateUser(id: number): Promise<any> {
  const res = await apiFetch(`${API_BASE}/users/${id}/deactivate`, { method: 'PATCH' });
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function reactivateUser(id: number): Promise<any> {
  const res = await apiFetch(`${API_BASE}/users/${id}/reactivate`, { method: 'PATCH' });
  if (!res.ok) await handleError(res);
  return res.json();
}

export async function resetUserPassword(id: number, payload: UserPasswordResetPayload): Promise<any> {
  const res = await apiFetch(`${API_BASE}/users/${id}/reset-password`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!res.ok) await handleError(res);
  return res.json();
}