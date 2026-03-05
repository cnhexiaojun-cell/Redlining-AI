import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { useLanguage } from "../../contexts/LanguageContext";
import * as adminApi from "../../api/admin";
import type { PlanDto } from "../../api/admin";
import Modal from "../../components/Modal";

export default function AdminPlansPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [plans, setPlans] = useState<PlanDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<"create" | "edit" | "delete" | null>(null);
  const [editingPlan, setEditingPlan] = useState<PlanDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<PlanDto | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    code: "",
    name: "",
    type: "quota" as string,
    quota: 5,
    period: "" as string,
    priceCents: 0,
    defaultPlan: false,
    sortOrder: 0,
    description: "",
    scope: "",
  });

  const loadPlans = useCallback(() => {
    if (!token) return;
    setLoading(true);
    adminApi
      .getPlans(token)
      .then(setPlans)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    loadPlans();
  }, [loadPlans]);

  const openCreate = () => {
    setEditingPlan(null);
    setForm({
      code: "",
      name: "",
      type: "quota",
      quota: 5,
      period: "",
      priceCents: 0,
      defaultPlan: false,
      sortOrder: plans.length * 10,
      description: "",
      scope: "",
    });
    setModal("create");
  };

  const openEdit = (p: PlanDto) => {
    setEditingPlan(p);
    setForm({
      code: p.code,
      name: p.name,
      type: p.type,
      quota: p.quota,
      period: p.period ?? "",
      priceCents: p.priceCents,
      defaultPlan: p.defaultPlan,
      sortOrder: p.sortOrder,
      description: p.description ?? "",
      scope: p.scope ?? "",
    });
    setModal("edit");
  };

  const openDelete = (p: PlanDto) => {
    setDeleteTarget(p);
    setModal("delete");
  };

  const closeModal = () => {
    setModal(null);
    setEditingPlan(null);
    setDeleteTarget(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    try {
      const body = {
        code: form.code.trim(),
        name: form.name.trim(),
        type: form.type,
        quota: form.quota,
        period: form.type === "subscription" ? (form.period || null) : null,
        priceCents: form.priceCents,
        defaultPlan: form.defaultPlan,
        sortOrder: form.sortOrder,
        description: form.description.trim() || null,
        scope: form.scope.trim() || null,
      };
      if (modal === "create") {
        await adminApi.createPlan(token, body);
      } else if (editingPlan) {
        await adminApi.updatePlan(token, editingPlan.id, body);
      }
      closeModal();
      loadPlans();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!token || !deleteTarget) return;
    setSubmitting(true);
    try {
      await adminApi.deletePlan(token, deleteTarget.id);
      closeModal();
      loadPlans();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading && plans.length === 0) return <div className="text-gray-500">{t("admin_loading")}</div>;
  if (error) return <div className="text-red-600">{error}</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold text-gray-900">{t("admin_plans_title")}</h1>
        <button
          type="button"
          onClick={openCreate}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700"
        >
          {t("admin_plans_new")}
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_id")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_org_code")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_org_name")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_plans_type")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_plans_quota")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_plans_period")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_plans_price")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_plans_default")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700 w-28">{t("admin_actions")}</th>
            </tr>
          </thead>
          <tbody>
            {plans.map((p) => (
              <tr key={p.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-3 px-4">{p.id}</td>
                <td className="py-3 px-4">{p.code}</td>
                <td className="py-3 px-4">{p.name}</td>
                <td className="py-3 px-4">{p.type === "quota" ? t("admin_plans_type_quota") : t("admin_plans_type_subscription")}</td>
                <td className="py-3 px-4">{p.quota <= 0 ? t("plan_quota_unlimited") : p.quota}</td>
                <td className="py-3 px-4">{p.period ? (p.period === "year" ? t("admin_plans_period_year") : t("admin_plans_period_month")) : "—"}</td>
                <td className="py-3 px-4">¥{(p.priceCents / 100).toFixed(2)}</td>
                <td className="py-3 px-4">{p.defaultPlan ? t("admin_ok") : "—"}</td>
                <td className="py-3 px-4">
                  <button type="button" onClick={() => openEdit(p)} className="text-blue-600 hover:underline mr-2">
                    {t("admin_edit")}
                  </button>
                  <button type="button" onClick={() => openDelete(p)} className="text-red-600 hover:underline">
                    {t("admin_delete")}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Modal
        open={modal === "create" || modal === "edit"}
        title={modal === "create" ? t("admin_plans_new_modal") : t("admin_plans_edit_modal")}
        onClose={closeModal}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_org_code")} *</label>
            <input
              type="text"
              required
              value={form.code}
              onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_org_name")} *</label>
            <input
              type="text"
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_plans_type")} *</label>
            <select
              value={form.type}
              onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            >
              <option value="quota">{t("admin_plans_type_quota")}</option>
              <option value="subscription">{t("admin_plans_type_subscription")}</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_plans_quota")} *</label>
            <input
              type="number"
              required
              min={-1}
              value={form.quota}
              onChange={(e) => setForm((f) => ({ ...f, quota: parseInt(e.target.value, 10) || 0 }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
            <p className="text-xs text-gray-500 mt-1">{t("admin_plans_quota_hint")}</p>
          </div>
          {form.type === "subscription" && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_plans_period")}</label>
              <select
                value={form.period}
                onChange={(e) => setForm((f) => ({ ...f, period: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                <option value="">—</option>
                <option value="month">{t("admin_plans_period_month")}</option>
                <option value="year">{t("admin_plans_period_year")}</option>
              </select>
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_plans_price")} (分)</label>
            <input
              type="number"
              min={0}
              value={form.priceCents}
              onChange={(e) => setForm((f) => ({ ...f, priceCents: parseInt(e.target.value, 10) || 0 }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="defaultPlan"
              checked={form.defaultPlan}
              onChange={(e) => setForm((f) => ({ ...f, defaultPlan: e.target.checked }))}
              className="rounded border-gray-300"
            />
            <label htmlFor="defaultPlan" className="text-sm text-gray-700">{t("admin_plans_default")}</label>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_sort_order")}</label>
            <input
              type="number"
              value={form.sortOrder}
              onChange={(e) => setForm((f) => ({ ...f, sortOrder: parseInt(e.target.value, 10) || 0 }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_plans_description")}</label>
            <textarea
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              rows={2}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_plans_scope")}</label>
            <input
              type="text"
              value={form.scope}
              onChange={(e) => setForm((f) => ({ ...f, scope: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={closeModal} className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg">
              {t("admin_cancel")}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? t("admin_submitting") : t("admin_save")}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={modal === "delete"} title={t("admin_confirm_delete")} onClose={closeModal}>
        {deleteTarget && (
          <>
            <p className="text-gray-700">{t("admin_plans_delete_confirm").replace("{name}", deleteTarget.name)}</p>
            <div className="flex justify-end gap-2 pt-4">
              <button type="button" onClick={closeModal} className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg">
                {t("admin_cancel")}
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={submitting}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {submitting ? t("admin_submitting") : t("admin_delete")}
              </button>
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}
