const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

export interface DocumentItem {
  id: string;
  userId: number;
  name: string;
  minioKey: string;
  createdAt: string;
}

export interface EditorConfig {
  document: { url: string; key: string; fileType: string; title: string };
  documentType: string;
  editorConfig: { callbackUrl: string; mode?: string };
  documentServerUrl?: string;
}

export async function listDocuments(token: string | null): Promise<DocumentItem[]> {
  if (!token) throw new Error("Not authenticated");
  const res = await fetch(`${BASE_URL}/api/documents`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to list documents: ${res.status}`);
  }
  return res.json();
}

export async function createDocument(
  file: File,
  token: string | null
): Promise<DocumentItem> {
  if (!token) throw new Error("Not authenticated");
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${BASE_URL}/api/documents`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to upload document: ${res.status}`);
  }
  return res.json();
}

export async function getEditorConfig(
  documentId: string,
  token: string | null,
  options?: { mode?: "edit" | "view" }
): Promise<EditorConfig> {
  if (!token) throw new Error("Not authenticated");
  const mode = options?.mode ?? "edit";
  const url = `${BASE_URL}/api/documents/${encodeURIComponent(documentId)}/editor-config?mode=${encodeURIComponent(mode)}`;
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to get editor config: ${res.status}`);
  }
  return res.json();
}
