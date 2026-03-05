export interface RiskItem {
  type: string;
  description: string;
  severity: "高" | "中" | "低";
  clause: string;
  suggestion: string;
}

export interface MissingClause {
  clause: string;
  importance: string;
  recommendation: string;
}

export interface AnalysisResult {
  summary: string;
  compliance_score: number;
  risks: RiskItem[];
  missing_clauses: MissingClause[];
  key_points: string[];
  /** Shown when backend returns a placeholder (e.g. AI not configured) */
  notice?: string;
  /** Id of the saved report record; set by backend after persisting. */
  reportId?: number;
}
