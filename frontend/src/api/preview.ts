import type { AnalysisResult } from "../types/analysis";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

export interface OnlyOfficePreviewConfig {
  document: {
    url: string;
    key: string;
    fileType: string;
    title: string;
    isForm?: boolean;
    permissions?: { edit?: boolean; comment?: boolean };
  };
  documentType: string;
  editorConfig: {
    mode: string;
    lang?: string;
    user?: { id: string; name: string };
    customization?: { comments?: boolean };
  };
  documentServerUrl: string;
}

/** Response from preview/annotate upload; includes minioKey when backend can link to report. */
export type OnlyOfficePreviewResponse = OnlyOfficePreviewConfig & { minioKey?: string };

/**
 * Get DOCX with AI comments (批注) inserted and OnlyOffice config (edit+comment mode).
 * Use for DOCX so 批注 show inside the document in OnlyOffice.
 */
/** Error message thrown when token is missing or backend returns 401. Show auth_session_expired + login link. */
export const AUTH_REQUIRED_ERROR = "AUTH_REQUIRED";

export async function annotateDocument(
  file: File,
  analysisResult: AnalysisResult,
  token: string | null,
  options?: { signal?: AbortSignal }
): Promise<OnlyOfficePreviewResponse> {
  if (!token) throw new Error(AUTH_REQUIRED_ERROR);
  const form = new FormData();
  form.append("file", file);
  form.append("analysisResult", JSON.stringify(analysisResult));
  const res = await fetch(`${BASE_URL}/api/annotate`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
    signal: options?.signal,
  });
  const text = await res.text();
  if (!res.ok) {
    if (res.status === 401) throw new Error(AUTH_REQUIRED_ERROR);
    let message = text;
    try {
      const json = JSON.parse(text) as { detail?: string };
      message = json.detail ?? text;
    } catch {
      // use text
    }
    throw new Error(message || `Annotate failed: ${res.status}`);
  }
  return JSON.parse(text) as OnlyOfficePreviewResponse;
}

/**
 * Upload contract file for OnlyOffice preview (PDF, DOCX, DOC).
 * Returns config to render the document with OnlyOffice plugin in view mode.
 */
export async function uploadPreviewDocument(
  file: File,
  token: string | null,
  options?: { signal?: AbortSignal }
): Promise<OnlyOfficePreviewResponse> {
  if (!token) throw new Error(AUTH_REQUIRED_ERROR);
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${BASE_URL}/api/preview/upload`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
    signal: options?.signal,
  });
  const text = await res.text();
  if (!res.ok) {
    if (res.status === 401) throw new Error(AUTH_REQUIRED_ERROR);
    let message = text;
    try {
      const json = JSON.parse(text) as { detail?: string };
      message = json.detail ?? text;
    } catch {
      // use text
    }
    throw new Error(message || `Upload failed: ${res.status}`);
  }
  return JSON.parse(text) as OnlyOfficePreviewResponse;
}
