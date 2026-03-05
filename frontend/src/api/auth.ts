const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

export interface UserInfo {
  id: number;
  username?: string | null;
  email?: string | null;
  avatarUrl?: string | null;
  realName?: string | null;
  occupation?: string | null;
  planCode?: string | null;
  planName?: string | null;
  planType?: string | null;
  quotaRemaining?: number | null;
  quotaTotal?: number | null;
  periodEndsAt?: string | null;
}

export interface LoginResponse {
  access_token: string;
  token_type: string;
  user: UserInfo;
}

export interface CaptchaResponse {
  captchaId: string;
  image: string;
}

async function handleResponse<T>(res: Response): Promise<T> {
  const text = await res.text();
  let message = text;
  try {
    const json = JSON.parse(text) as { detail?: string; message?: string };
    message = json.detail ?? json.message ?? text;
  } catch {
    // use text as message
  }
  if (!res.ok) {
    const err = new Error(message || `Request failed: ${res.status}`) as Error & { status?: number };
    err.status = res.status;
    throw err;
  }
  return (text ? JSON.parse(text) : {}) as T;
}

export async function getCaptcha(): Promise<CaptchaResponse> {
  const res = await fetch(`${BASE_URL}/api/captcha`);
  return handleResponse<CaptchaResponse>(res);
}

export async function login(
  username: string,
  password: string,
  captchaId: string,
  captchaCode: string
): Promise<LoginResponse> {
  const res = await fetch(`${BASE_URL}/api/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      username,
      password,
      captchaId,
      captchaCode,
    }),
  });
  return handleResponse<LoginResponse>(res);
}

export async function register(
  username: string,
  password: string,
  captchaId: string,
  captchaCode: string,
  email?: string
): Promise<LoginResponse> {
  const body: Record<string, string> = {
    username,
    password,
    captchaId,
    captchaCode,
  };
  if (email != null && email.trim() !== "") {
    body.email = email.trim();
  }
  const res = await fetch(`${BASE_URL}/api/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return handleResponse<LoginResponse>(res);
}

export interface PermissionsResponse {
  superAdmin: boolean;
  menuCodes: string[];
  buttonCodes: string[];
  dataScope: string[];
}

export async function getMe(token: string): Promise<UserInfo> {
  const res = await fetch(`${BASE_URL}/api/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<UserInfo>(res);
}

export async function getPermissions(token: string): Promise<PermissionsResponse> {
  const res = await fetch(`${BASE_URL}/api/me/permissions`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<PermissionsResponse>(res);
}

export async function updateProfile(
  token: string,
  body: { realName?: string; occupation?: string; email?: string }
): Promise<UserInfo> {
  const res = await fetch(`${BASE_URL}/api/me`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });
  return handleResponse<UserInfo>(res);
}

export async function uploadAvatar(token: string, file: Blob | File): Promise<UserInfo> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await fetch(`${BASE_URL}/api/me/avatar`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });
  return handleResponse<UserInfo>(res);
}
