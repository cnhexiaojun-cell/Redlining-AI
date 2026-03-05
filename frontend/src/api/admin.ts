const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

async function handleResponse<T>(res: Response): Promise<T> {
  const text = await res.text();
  if (!res.ok) {
    let message = text;
    try {
      const json = JSON.parse(text) as { message?: string };
      message = json.message ?? text;
    } catch {
      //
    }
    const err = new Error(message) as Error & { status?: number };
    err.status = res.status;
    throw err;
  }
  return (text ? JSON.parse(text) : {}) as T;
}

export interface OrganizationDto {
  id: number;
  name: string;
  code: string;
  parentId?: number | null;
  sortOrder: number;
  createdAt: string;
  children?: OrganizationDto[];
}

export interface UserAdminDto {
  id: number;
  username?: string;
  email?: string;
  realName?: string;
  enabled: boolean;
  superAdmin: boolean;
  organizationId?: number | null;
  organizationName?: string;
  createdAt: string;
  roleIds?: number[];
}

export interface RoleDto {
  id: number;
  name: string;
  code: string;
  description?: string;
  createdAt: string;
  permissionIds?: number[];
}

export interface PermissionTreeDto {
  id: number;
  code: string;
  name: string;
  type: string;
  parentId?: number | null;
  sortOrder: number;
  children?: PermissionTreeDto[];
}

export interface OperationLogDto {
  id: number;
  userId?: number | null;
  username?: string;
  module?: string;
  action?: string;
  method?: string;
  path?: string;
  ip?: string;
  requestBody?: string;
  createdAt: string;
}

export interface SystemSettingDto {
  id: number;
  key: string;
  value?: string;
  description?: string;
  valueType?: string;
  updatedAt: string;
}

export interface PlanDto {
  id: number;
  code: string;
  name: string;
  type: string;
  quota: number;
  period?: string | null;
  priceCents: number;
  defaultPlan: boolean;
  sortOrder: number;
  description?: string | null;
  scope?: string | null;
}

export async function getOrganizationsTree(token: string, parentId?: number): Promise<OrganizationDto[]> {
  const url = parentId != null ? `${BASE_URL}/api/admin/organizations/tree?parentId=${parentId}` : `${BASE_URL}/api/admin/organizations/tree`;
  const res = await fetch(url, { headers: authHeaders(token) });
  return handleResponse<OrganizationDto[]>(res);
}

export async function getOrganizations(token: string): Promise<OrganizationDto[]> {
  const res = await fetch(`${BASE_URL}/api/admin/organizations`, { headers: authHeaders(token) });
  return handleResponse<OrganizationDto[]>(res);
}

export async function createOrganization(token: string, body: { name: string; code: string; parentId?: number; sortOrder?: number }): Promise<OrganizationDto> {
  const res = await fetch(`${BASE_URL}/api/admin/organizations`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<OrganizationDto>(res);
}

export async function updateOrganization(token: string, id: number, body: { name: string; code: string; parentId?: number; sortOrder?: number }): Promise<OrganizationDto> {
  const res = await fetch(`${BASE_URL}/api/admin/organizations/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<OrganizationDto>(res);
}

export async function deleteOrganization(token: string, id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/admin/organizations/${id}`, { method: "DELETE", headers: authHeaders(token) });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Delete failed");
  }
}

export async function getUsers(token: string, params?: { organizationId?: number; enabled?: boolean; page?: number; size?: number }): Promise<{ content: UserAdminDto[]; totalElements: number }> {
  const sp = new URLSearchParams();
  if (params?.organizationId != null) sp.set("organizationId", String(params.organizationId));
  if (params?.enabled != null) sp.set("enabled", String(params.enabled));
  if (params?.page != null) sp.set("page", String(params.page));
  if (params?.size != null) sp.set("size", String(params.size));
  const res = await fetch(`${BASE_URL}/api/admin/users?${sp}`, { headers: authHeaders(token) });
  return handleResponse<{ content: UserAdminDto[]; totalElements: number }>(res);
}

export async function getUser(token: string, id: number): Promise<UserAdminDto> {
  const res = await fetch(`${BASE_URL}/api/admin/users/${id}`, { headers: authHeaders(token) });
  return handleResponse<UserAdminDto>(res);
}

export async function createUser(token: string, body: { username: string; email?: string; password?: string; realName?: string; organizationId?: number; roleIds?: number[] }): Promise<UserAdminDto> {
  const res = await fetch(`${BASE_URL}/api/admin/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<UserAdminDto>(res);
}

export async function updateUser(token: string, id: number, body: { email?: string; realName?: string; enabled?: boolean; organizationId?: number; roleIds?: number[] }): Promise<UserAdminDto> {
  const res = await fetch(`${BASE_URL}/api/admin/users/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<UserAdminDto>(res);
}

export async function resetUserPassword(token: string, id: number, password: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/admin/users/${id}/reset-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify({ password }),
  });
  if (!res.ok) throw new Error(await res.text());
}

export async function getRoles(token: string, page = 0, size = 20): Promise<{ content: RoleDto[]; totalElements: number }> {
  const res = await fetch(`${BASE_URL}/api/admin/roles?page=${page}&size=${size}`, { headers: authHeaders(token) });
  return handleResponse<{ content: RoleDto[]; totalElements: number }>(res);
}

export async function getRole(token: string, id: number): Promise<RoleDto> {
  const res = await fetch(`${BASE_URL}/api/admin/roles/${id}`, { headers: authHeaders(token) });
  return handleResponse<RoleDto>(res);
}

export async function createRole(token: string, body: { name: string; code: string; description?: string; permissionIds?: number[] }): Promise<RoleDto> {
  const res = await fetch(`${BASE_URL}/api/admin/roles`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<RoleDto>(res);
}

export async function updateRole(token: string, id: number, body: { name: string; code: string; description?: string; permissionIds?: number[] }): Promise<RoleDto> {
  const res = await fetch(`${BASE_URL}/api/admin/roles/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<RoleDto>(res);
}

export async function deleteRole(token: string, id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/admin/roles/${id}`, { method: "DELETE", headers: authHeaders(token) });
  if (!res.ok) throw new Error(await res.text());
}

export async function getPermissionsTree(token: string): Promise<PermissionTreeDto[]> {
  const res = await fetch(`${BASE_URL}/api/admin/permissions/tree`, { headers: authHeaders(token) });
  return handleResponse<PermissionTreeDto[]>(res);
}

export async function getLogs(
  token: string,
  params?: { userId?: number; module?: string; start?: string; end?: string; page?: number; size?: number }
): Promise<{ content: OperationLogDto[]; totalElements: number }> {
  const sp = new URLSearchParams();
  if (params?.userId != null) sp.set("userId", String(params.userId));
  if (params?.module != null) sp.set("module", params.module);
  if (params?.start != null) sp.set("start", params.start);
  if (params?.end != null) sp.set("end", params.end);
  if (params?.page != null) sp.set("page", String(params.page));
  if (params?.size != null) sp.set("size", String(params.size));
  const res = await fetch(`${BASE_URL}/api/admin/logs?${sp}`, { headers: authHeaders(token) });
  return handleResponse<{ content: OperationLogDto[]; totalElements: number }>(res);
}

export async function getSettings(token: string): Promise<SystemSettingDto[]> {
  const res = await fetch(`${BASE_URL}/api/admin/settings`, { headers: authHeaders(token) });
  return handleResponse<SystemSettingDto[]>(res);
}

export async function updateSettings(token: string, updates: Record<string, string>): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/admin/settings`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(updates),
  });
  if (!res.ok) throw new Error(await res.text());
}

export async function getPlans(token: string): Promise<PlanDto[]> {
  const res = await fetch(`${BASE_URL}/api/admin/plans`, { headers: authHeaders(token) });
  return handleResponse<PlanDto[]>(res);
}

export async function getPlan(token: string, id: number): Promise<PlanDto> {
  const res = await fetch(`${BASE_URL}/api/admin/plans/${id}`, { headers: authHeaders(token) });
  return handleResponse<PlanDto>(res);
}

export async function createPlan(
  token: string,
  body: {
    code: string;
    name: string;
    type: string;
    quota: number;
    period?: string | null;
    priceCents: number;
    defaultPlan: boolean;
    sortOrder: number;
    description?: string | null;
    scope?: string | null;
  }
): Promise<PlanDto> {
  const res = await fetch(`${BASE_URL}/api/admin/plans`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<PlanDto>(res);
}

export async function updatePlan(
  token: string,
  id: number,
  body: {
    code: string;
    name: string;
    type: string;
    quota: number;
    period?: string | null;
    priceCents: number;
    defaultPlan: boolean;
    sortOrder: number;
    description?: string | null;
    scope?: string | null;
  }
): Promise<PlanDto> {
  const res = await fetch(`${BASE_URL}/api/admin/plans/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders(token) },
    body: JSON.stringify(body),
  });
  return handleResponse<PlanDto>(res);
}

export async function deletePlan(token: string, id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/admin/plans/${id}`, { method: "DELETE", headers: authHeaders(token) });
  if (!res.ok) throw new Error(await res.text());
}
