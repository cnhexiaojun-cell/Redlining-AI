import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useLanguage } from "../contexts/LanguageContext";
import { listOrders, type OrderDto } from "../api/plans";
import PaginationBar from "../components/PaginationBar";

const PAGE_SIZE = 10;

export default function OrderHistoryPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [data, setData] = useState<{ content: OrderDto[]; totalElements: number } | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(PAGE_SIZE);

  const load = useCallback(() => {
    if (!token) return;
    setLoading(true);
    listOrders(token, page, pageSize)
      .then((res) => setData({ content: res.content, totalElements: res.totalElements }))
      .catch(() => setData({ content: [], totalElements: 0 }))
      .finally(() => setLoading(false));
  }, [token, page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  const formatDate = (s: string | undefined) =>
    s ? new Date(s).toLocaleString(undefined, { dateStyle: "short", timeStyle: "short" }) : "—";

  const statusLabel = (status: string) =>
    status === "paid" ? t("order_status_paid") : t("order_status_pending");

  return (
    <div className="w-full max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">{t("order_history_title")}</h1>
      <p className="text-gray-600 mb-6">{t("order_history_desc")}</p>

      <div className="mb-4">
        <Link
          to="/plans"
          className="text-sm text-blue-600 hover:underline"
        >
          ← {t("order_back_plans")}
        </Link>
      </div>

      {loading ? (
        <div className="text-gray-500 py-8">{t("admin_loading")}</div>
      ) : !data || data.content.length === 0 ? (
        <div className="text-gray-500 py-8">{t("admin_none")}</div>
      ) : (
        <>
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("order_id")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("order_plan")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("order_amount")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("order_status")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("order_created_at")}</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">{t("order_paid_at")}</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((order) => (
                  <tr key={order.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4 text-gray-900">{order.id}</td>
                    <td className="py-3 px-4 text-gray-700">
                      {order.planName ?? `#${order.planId}`}
                      {order.renewal && (
                        <span className="ml-1 text-xs text-gray-500">({t("order_renewal")})</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-gray-700">¥{(order.amountCents / 100).toFixed(2)}</td>
                    <td className="py-3 px-4">
                      <span
                        className={
                          order.status === "paid"
                            ? "text-green-600 font-medium"
                            : "text-amber-600"
                        }
                      >
                        {statusLabel(order.status)}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-gray-600">{formatDate(order.createdAt)}</td>
                    <td className="py-3 px-4 text-gray-600">{formatDate(order.paidAt)}</td>
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
