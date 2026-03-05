import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { useLanguage } from "../../contexts/LanguageContext";
import * as adminApi from "../../api/admin";
import type { OrganizationDto } from "../../api/admin";
import Modal from "../../components/Modal";

export default function AdminOrganizationsPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [tree, setTree] = useState<OrganizationDto[]>([]);
  const [flatOrgs, setFlatOrgs] = useState<OrganizationDto[]>([]);
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<"create" | "edit" | null>(null);
  const [parentId, setParentId] = useState<number | null>(null);
  const [editing, setEditing] = useState<OrganizationDto | null>(null);
  const [form, setForm] = useState({ name: "", code: "", sortOrder: 0 });
  const [submitting, setSubmitting] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<OrganizationDto | null>(null);

  const toggleExpand = (id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const load = useCallback(() => {
    if (!token) return;
    Promise.all([
      adminApi.getOrganizationsTree(token),
      adminApi.getOrganizations(token),
    ])
      .then(([tr, f]) => {
        setTree(tr);
        setFlatOrgs(f);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token]);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = (pid: number | null) => {
    setParentId(pid);
    setEditing(null);
    setForm({ name: "", code: "", sortOrder: 0 });
    setModal("create");
  };

  const openEdit = (org: OrganizationDto) => {
    setEditing(org);
    setParentId(org.parentId ?? null);
    setForm({ name: org.name, code: org.code, sortOrder: org.sortOrder ?? 0 });
    setModal("edit");
  };

  const closeModal = () => {
    setModal(null);
    setEditing(null);
    setDeleteConfirm(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    try {
      if (modal === "create") {
        await adminApi.createOrganization(token, {
          name: form.name,
          code: form.code,
          parentId: parentId ?? undefined,
          sortOrder: form.sortOrder,
        });
      } else if (editing) {
        await adminApi.updateOrganization(token, editing.id, {
          name: form.name,
          code: form.code,
          parentId: parentId ?? undefined,
          sortOrder: form.sortOrder,
        });
      }
      closeModal();
      load();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!token || !deleteConfirm) return;
    setSubmitting(true);
    try {
      await adminApi.deleteOrganization(token, deleteConfirm.id);
      closeModal();
      load();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  function renderNode(nodes: OrganizationDto[], depth = 0) {
    if (!nodes.length) return null;
    return (
      <ul className="list-none pl-0">
        {nodes.map((o) => {
          const hasChildren = o.children && o.children.length > 0;
          const isExpanded = expandedIds.has(o.id);
          return (
            <li key={o.id} className="py-0.5">
              <div
                className="flex items-center gap-1 group py-1 rounded hover:bg-gray-50"
                style={{ paddingLeft: depth * 16 }}
              >
                <span className="w-5 flex-shrink-0">
                  {hasChildren ? (
                    <button
                      type="button"
                      onClick={() => toggleExpand(o.id)}
                      className="text-gray-500 hover:text-gray-700 p-0.5"
                      aria-label={isExpanded ? "Collapse" : "Expand"}
                    >
                      {isExpanded ? "▼" : "▶"}
                    </button>
                  ) : (
                    <span className="inline-block w-5" />
                  )}
                </span>
                <span className="font-medium text-gray-900">{o.name}</span>
                <span className="text-gray-500 text-sm">({o.code})</span>
                <span className="opacity-0 group-hover:opacity-100 flex gap-1 ml-2">
                  <button
                    type="button"
                    onClick={() => openCreate(o.id)}
                    className="text-xs text-blue-600 hover:underline"
                  >
                    {t("admin_add_child_org")}
                  </button>
                  <button
                    type="button"
                    onClick={() => openEdit(o)}
                    className="text-xs text-gray-600 hover:underline"
                  >
                    {t("admin_edit")}
                  </button>
                  <button
                    type="button"
                    onClick={() => setDeleteConfirm(o)}
                    className="text-xs text-red-600 hover:underline"
                  >
                    {t("admin_delete")}
                  </button>
                </span>
              </div>
              {hasChildren && isExpanded && (
                <div className="border-l border-gray-200 ml-2">{renderNode(o.children!, depth + 1)}</div>
              )}
            </li>
          );
        })}
      </ul>
    );
  }

  if (loading) return <div className="text-gray-500">{t("admin_loading")}</div>;
  if (error) return <div className="text-red-600">{error}</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold text-gray-900">{t("admin_organizations")}</h1>
        <button
          type="button"
          onClick={() => openCreate(null)}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700"
        >
          {t("admin_add_root_org")}
        </button>
      </div>
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        {tree.length === 0 ? (
          <p className="text-gray-500">{t("admin_no_org")}</p>
        ) : (
          renderNode(tree)
        )}
      </div>

      <Modal
        open={modal === "create" || modal === "edit"}
        title={modal === "create" ? t("admin_org_new_modal") : t("admin_org_edit_modal")}
        onClose={closeModal}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_parent_org")}</label>
            <select
              value={parentId ?? ""}
              onChange={(e) => setParentId(e.target.value ? Number(e.target.value) : null)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            >
              <option value="">{t("admin_no_parent")}</option>
              {flatOrgs
                .filter((o) => modal !== "edit" || o.id !== editing?.id)
                .map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.name} ({o.code})
                  </option>
                ))}
            </select>
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
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_sort_order")}</label>
            <input
              type="number"
              value={form.sortOrder}
              onChange={(e) => setForm((f) => ({ ...f, sortOrder: Number(e.target.value) || 0 }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={closeModal} className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">
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

      <Modal open={!!deleteConfirm} title={t("admin_confirm_delete")} onClose={closeModal}>
        {deleteConfirm && (
          <>
            <p className="text-gray-700">
              {t("admin_delete_org_confirm").replace("{name}", deleteConfirm.name)}
            </p>
            <div className="flex justify-end gap-2 pt-4">
              <button type="button" onClick={closeModal} className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">
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
