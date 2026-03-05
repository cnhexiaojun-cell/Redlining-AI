import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { useLanguage } from "../../contexts/LanguageContext";
import * as adminApi from "../../api/admin";
import type { OperationLogDto } from "../../api/admin";
import PaginationBar, { PAGE_SIZE_OPTIONS } from "../../components/PaginationBar";

function toISO(d: string): string | undefined {
  if (!d || !d.trim()) return undefined;
  const date = new Date(d);
  return isNaN(date.getTime()) ? undefined : date.toISOString();
}

export default function AdminLogsPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [logs, setLogs] = useState<OperationLogDto[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [form, setForm] = useState({ userId: "", module: "", start: "", end: "" });
  const [applied, setApplied] = useState<{ userId?: number; module?: string; start?: string; end?: string }>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!token) return;
    setLoading(true);
    adminApi
      .getLogs(token, {
        page,
        size: pageSize,
        userId: applied.userId,
        module: applied.module,
        start: applied.start,
        end: applied.end,
      })
      .then((res) => {
        setLogs(res.content);
        setTotal(res.totalElements);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token, page, pageSize, applied.userId, applied.module, applied.start, applied.end]);

  useEffect(() => {
    load();
  }, [load]);

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setApplied({
      userId: form.userId ? Number(form.userId) : undefined,
      module: form.module.trim() || undefined,
      start: toISO(form.start),
      end: toISO(form.end),
    });
    setPage(0);
  };

  if (error) return <div className="text-red-600">{error}</div>;

  return (
    <div>
      <h1 className="text-xl font-bold text-gray-900 mb-4">{t("admin_logs_title")}</h1>

      <form onSubmit={onSearch} className="bg-white rounded-xl border border-gray-200 p-4 mb-4 flex flex-wrap gap-4 items-end">
        <div>
          <label className="block text-xs text-gray-500 mb-1">{t("admin_user_id")}</label>
          <input
            type="number"
            min={1}
            value={form.userId}
            onChange={(e) => setForm((f) => ({ ...f, userId: e.target.value }))}
            placeholder="可选"
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm w-28"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">{t("admin_module")}</label>
          <input
            type="text"
            value={form.module}
            onChange={(e) => setForm((f) => ({ ...f, module: e.target.value }))}
            placeholder="e.g. user, settings"
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm w-40"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">{t("admin_start_time")}</label>
          <input
            type="datetime-local"
            value={form.start}
            onChange={(e) => setForm((f) => ({ ...f, start: e.target.value }))}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">{t("admin_end_time")}</label>
          <input
            type="datetime-local"
            value={form.end}
            onChange={(e) => setForm((f) => ({ ...f, end: e.target.value }))}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <button type="submit" className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700">
          {t("admin_query")}
        </button>
      </form>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading && logs.length === 0 ? (
          <div className="p-8 text-center text-gray-500">{t("admin_loading")}</div>
        ) : (
          <>
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_time")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_user")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_module")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_action")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_path")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_ip")}</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4 text-gray-600">
                      {log.createdAt ? new Date(log.createdAt).toLocaleString() : "—"}
                    </td>
                    <td className="py-3 px-4">{log.username ?? "—"}</td>
                    <td className="py-3 px-4">{log.module ?? "—"}</td>
                    <td className="py-3 px-4">{log.action ?? "—"}</td>
                    <td className="py-3 px-4 text-gray-600 max-w-xs truncate">{log.path ?? "—"}</td>
                    <td className="py-3 px-4">{log.ip ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <PaginationBar
              total={total}
              page={page}
              pageSize={pageSize}
              onPageChange={setPage}
              onPageSizeChange={(size) => {
                setPageSize(size);
                setPage(0);
              }}
              t={t}
              pageSizeOptions={PAGE_SIZE_OPTIONS}
            />
          </>
        )}
      </div>
    </div>
  );
}
