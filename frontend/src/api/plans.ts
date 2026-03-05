const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

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
    throw new Error(message);
  }
  return (text ? JSON.parse(text) : []) as T;
}

export async function getPlans(token: string): Promise<PlanDto[]> {
  const res = await fetch(`${BASE_URL}/api/plans`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<PlanDto[]>(res);
}

export interface OrderDto {
  id: number;
  planId: number;
  amountCents: number;
  paymentMethod?: string;
  status: string;
  paidAt?: string;
  createdAt: string;
  renewal: boolean;
  /** WeChat Pay Native code_url for QR; only when paymentMethod is wechat and backend created native order */
  codeUrl?: string | null;
  /** Plan name for display in order list */
  planName?: string | null;
}

export interface OrderListResponse {
  content: OrderDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export async function createOrder(
  token: string,
  body: { planId: number; paymentMethod?: string; renewal?: boolean }
): Promise<OrderDto> {
  const res = await fetch(`${BASE_URL}/api/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      planId: body.planId,
      paymentMethod: body.paymentMethod || "wechat",
      renewal: body.renewal ?? false,
    }),
  });
  return handleResponse<OrderDto>(res);
}

export async function listOrders(
  token: string,
  page: number = 0,
  size: number = 10
): Promise<OrderListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  const res = await fetch(`${BASE_URL}/api/orders?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<OrderListResponse>(res);
}

export async function getOrder(token: string, orderId: number): Promise<OrderDto> {
  const res = await fetch(`${BASE_URL}/api/orders/${orderId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<OrderDto>(res);
}

export async function completeOrder(token: string, orderId: number): Promise<OrderDto> {
  const res = await fetch(`${BASE_URL}/api/orders/${orderId}/complete`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<OrderDto>(res);
}
