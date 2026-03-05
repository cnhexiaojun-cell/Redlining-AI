import type { AnalysisResult } from "../types/analysis";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

export interface AnalyzeOptions {
  stance?: string;
  advancedRules?: string;
  token?: string | null;
}

export interface AnalyzeError extends Error {
  status?: number;
}

export async function analyzeContract(
  file: File,
  options?: AnalyzeOptions
): Promise<AnalysisResult> {
  const form = new FormData();
  form.append("file", file);
  if (options?.stance) form.append("stance", options.stance);
  if (options?.advancedRules?.trim())
    form.append("advanced_rules", options.advancedRules.trim());

  const headers: Record<string, string> = {};
  if (options?.token) headers.Authorization = `Bearer ${options.token}`;

  const res = await fetch(`${BASE_URL}/api/analyze`, {
    method: "POST",
    headers,
    body: form,
  });

  const text = await res.text();
  let message = text;
  try {
    const json = JSON.parse(text) as { detail?: string };
    message = json.detail ?? text;
  } catch {
    // use text as message
  }

  if (!res.ok) {
    const err = new Error(message || `Request failed: ${res.status}`) as AnalyzeError;
    err.status = res.status;
    throw err;
  }

  return JSON.parse(text) as AnalysisResult;
}
