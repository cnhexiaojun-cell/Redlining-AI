import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useLanguage } from "../contexts/LanguageContext";
import { listReports, type ReportDto } from "../api/reports";
import PaginationBar from "../components/PaginationBar";

const PAGE_SIZE = 10;

export default function ReportsPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [data, setData] = useState<{ content: ReportDto[]; totalElements: number } | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(PAGE_SIZE);

  const load = useCallback(() => {
    if (!token) return;
    setLoading(true);
    listReports(token, page, pageSize)
      .then((res) => setData({ content: res.content, totalElements: res.totalElements }))
      .catch(() => setData({ content: [], totalElements: 0 }))
      .finally(() => setLoading(false));
  }, [token, page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  const formatDate = (s: string | undefined) =>
    s ? new Date(s).toLocaleString(undefined, { dateStyle: "short", timeStyle: "short" }) : "—";

  return (
    <div className="w-full max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">{t("reports_list_title")}</h1>
      <p className="text-gray-600 mb-6">{t("reports_list_desc")}</p>

      {loading ? (
        <div className="text-gray-500 py-8">{t("admin_loading")}</div>
      ) : !data || data.content.length === 0 ? (
        <div className="text-gray-500 py-8">{t("reports_list_empty")}</div>
      ) : (
        <>
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("reports_file_name")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("reports_created_at")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("reports_health_score")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("scanning_high_risk")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("scanning_med_high")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("scanning_medium")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("scanning_low_risk")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("reports_view")}</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((report) => (
                  <tr key={report.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4 text-gray-900 truncate max-w-[200px]" title={report.fileName}>
                      {report.fileName}
                    </td>
                    <td className="py-3 px-4 text-gray-600">{formatDate(report.createdAt)}</td>
                    <td className="py-3 px-4 text-gray-700">
                      {report.complianceScore != null ? `${report.complianceScore}` : "—"}
                    </td>
                    <td className="py-3 px-4 text-gray-700">{report.riskCountHigh}</td>
                    <td className="py-3 px-4 text-gray-700">{report.riskCountMediumHigh}</td>
                    <td className="py-3 px-4 text-gray-700">{report.riskCountMedium}</td>
                    <td className="py-3 px-4 text-gray-700">{report.riskCountLow}</td>
                    <td className="py-3 px-4">
                      <Link
                        to={`/report/${report.id}`}
                        className="text-blue-600 hover:underline font-medium"
                      >
                        {t("reports_view_report")}
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <PaginationBar
            total={data.totalElements}
            page={page}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(0);
            }}
            t={t}
          />
        </>
      )}
    </div>
  );
}
