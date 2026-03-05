import type { AnalysisResult } from "../types/analysis";
import type { OnlyOfficePreviewConfig } from "./preview";

const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? "" : "http://localhost:8003");

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

export interface ReportDto {
  id: number;
  fileName: string;
  createdAt: string;
  complianceScore: number | null;
  riskCountHigh: number;
  riskCountMediumHigh: number;
  riskCountMedium: number;
  riskCountLow: number;
}

export interface ReportDetailDto extends ReportDto {
  result: AnalysisResult;
  onlyOfficeConfig?: OnlyOfficePreviewConfig;
}

export interface ReportListResponse {
  content: ReportDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export async function listReports(
  token: string,
  page: number = 0,
  size: number = 10
): Promise<ReportListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  const res = await fetch(`${BASE_URL}/api/reports?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<ReportListResponse>(res);
}

export async function getReport(
  token: string,
  reportId: number
): Promise<ReportDetailDto> {
  const res = await fetch(`${BASE_URL}/api/reports/${reportId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return handleResponse<ReportDetailDto>(res);
}

export async function linkReportDocument(
  token: string,
  reportId: number,
  documentKey: string
): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/reports/${reportId}/document`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ documentKey }),
  });
  if (!res.ok) {
    const text = await res.text();
    let message = text;
    try {
      const json = JSON.parse(text) as { message?: string };
      message = json.message ?? text;
    } catch {
      //
    }
    throw new Error(message);
  }
}
