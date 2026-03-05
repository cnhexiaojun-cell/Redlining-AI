const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

export async function extractContractText(
  file: File,
  options?: { token?: string | null; signal?: AbortSignal }
): Promise<{ text: string }> {
  const form = new FormData();
  form.append("file", file);

  const headers: Record<string, string> = {};
  if (options?.token) headers.Authorization = `Bearer ${options.token}`;

  const res = await fetch(`${BASE_URL}/api/extract`, {
    method: "POST",
    headers,
    body: form,
    signal: options?.signal,
  });

  const body = await res.text();
  let message = body;
  try {
    const json = JSON.parse(body) as { detail?: string };
    message = json.detail ?? body;
  } catch {
    // use body as message
  }

  if (!res.ok) {
    const err = new Error(message || `Request failed: ${res.status}`);
    (err as Error & { status?: number }).status = res.status;
    throw err;
  }

  return JSON.parse(body) as { text: string };
}
