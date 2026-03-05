import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { useLanguage } from "../../contexts/LanguageContext";
import * as adminApi from "../../api/admin";
import type { RoleDto, PermissionTreeDto } from "../../api/admin";
import Modal from "../../components/Modal";
import PaginationBar, { PAGE_SIZE_OPTIONS } from "../../components/PaginationBar";

function collectIds(node: PermissionTreeDto): number[] {
  const ids = [node.id];
  node.children?.forEach((c) => ids.push(...collectIds(c)));
  return ids;
}

function permissionLabelKey(code: string): string {
  return "perm_" + code.replace(/:/g, "_");
}

function PermissionTree({
  nodes,
  selectedIds,
  onChange,
  getLabel,
  labelSelectAll = "全选",
  labelUnselectAll = "取消全选",
}: {
  nodes: PermissionTreeDto[];
  selectedIds: number[];
  onChange: (ids: number[]) => void;
  getLabel?: (node: PermissionTreeDto) => string;
  labelSelectAll?: string;
  labelUnselectAll?: string;
}) {
  const toggleOne = (id: number, checked: boolean) => {
    if (checked) {
      onChange([...new Set([...selectedIds, id])]);
    } else {
      const find = (id: number, list: PermissionTreeDto[]): PermissionTreeDto | null => {
        for (const p of list) {
          if (p.id === id) return p;
          const c = p.children ? find(id, p.children) : null;
          if (c) return c;
        }
        return null;
      };
      const target = find(id, nodes);
      const toRemove = target ? collectIds(target) : [id];
      onChange(selectedIds.filter((i) => !toRemove.includes(i)));
    }
  };

  const toggleBranch = (branch: PermissionTreeDto, checked: boolean) => {
    const ids = collectIds(branch);
    if (checked) {
      onChange([...new Set([...selectedIds, ...ids])]);
    } else {
      onChange(selectedIds.filter((i) => !ids.includes(i)));
    }
  };

  if (!nodes.length) return null;

  return (
    <ul className="list-none pl-0 space-y-1">
      {nodes.map((p) => {
        const hasChildren = p.children && p.children.length > 0;
        const selected = selectedIds.includes(p.id);
        const branchIds = collectIds(p);
        const allDescSelected = branchIds.every((id) => selectedIds.includes(id));
        return (
          <li key={p.id} className="pl-2">
            <label className="flex items-center gap-2 py-0.5 text-sm cursor-pointer">
              <input
                type="checkbox"
                checked={selected}
                onChange={(e) => toggleOne(p.id, e.target.checked)}
                className="rounded border-gray-300"
              />
              <span className="text-gray-700">{getLabel ? getLabel(p) : p.name}</span>
              <span className="text-gray-400 text-xs">({p.code})</span>
              {hasChildren && (
                <button
                  type="button"
                  onClick={() => toggleBranch(p, !allDescSelected)}
                  className="text-xs text-blue-600 hover:underline ml-1"
                >
                  {allDescSelected ? labelUnselectAll : labelSelectAll}
                </button>
              )}
            </label>
            {hasChildren && (
              <div className="pl-4 border-l border-gray-200 ml-1 mt-1">
                <PermissionTree nodes={p.children!} selectedIds={selectedIds} onChange={onChange} getLabel={getLabel} labelSelectAll={labelSelectAll} labelUnselectAll={labelUnselectAll} />
              </div>
            )}
          </li>
        );
      })}
    </ul>
  );
}

export default function AdminRolesPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [permissionTree, setPermissionTree] = useState<PermissionTreeDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<"create" | "edit" | "delete" | null>(null);
  const [editingRole, setEditingRole] = useState<RoleDto | null>(null);
  const [form, setForm] = useState({ name: "", code: "", description: "", permissionIds: [] as number[] });
  const [deleteTarget, setDeleteTarget] = useState<RoleDto | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadRoles = useCallback(() => {
    if (!token) return;
    setLoading(true);
    adminApi
      .getRoles(token, page, pageSize)
      .then((res) => {
        setRoles(res.content);
        setTotal(res.totalElements);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token, page, pageSize]);

  useEffect(() => {
    loadRoles();
  }, [loadRoles]);

  useEffect(() => {
    if (!token) return;
    adminApi.getPermissionsTree(token).then(setPermissionTree).catch(() => {});
  }, [token]);

  const openCreate = () => {
    setEditingRole(null);
    setForm({ name: "", code: "", description: "", permissionIds: [] });
    setModal("create");
  };

  const openEdit = (r: RoleDto) => {
    setEditingRole(r);
    setForm({
      name: r.name,
      code: r.code,
      description: r.description ?? "",
      permissionIds: r.permissionIds ?? [],
    });
    setModal("edit");
  };

  const openDelete = (r: RoleDto) => {
    setDeleteTarget(r);
    setModal("delete");
  };

  const closeModal = () => {
    setModal(null);
    setEditingRole(null);
    setDeleteTarget(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    try {
      if (modal === "create") {
        await adminApi.createRole(token, {
          name: form.name.trim(),
          code: form.code.trim(),
          description: form.description.trim() || undefined,
          permissionIds: form.permissionIds,
        });
      } else if (editingRole) {
        await adminApi.updateRole(token, editingRole.id, {
          name: form.name.trim(),
          code: form.code.trim(),
          description: form.description.trim() || undefined,
          permissionIds: form.permissionIds,
        });
      }
      closeModal();
      loadRoles();
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
      await adminApi.deleteRole(token, deleteTarget.id);
      closeModal();
      loadRoles();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading && roles.length === 0) return <div className="text-gray-500">{t("admin_loading")}</div>;
  if (error) return <div className="text-red-600">{error}</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold text-gray-900">{t("admin_role_list_title")}</h1>
        <button
          type="button"
          onClick={openCreate}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700"
        >
          {t("admin_new_role_btn")}
        </button>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_id")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_org_name")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_org_code")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_description")}</th>
              <th className="text-left py-3 px-4 font-medium text-gray-700 w-28">{t("admin_actions")}</th>
            </tr>
          </thead>
          <tbody>
            {roles.map((r) => (
              <tr key={r.id} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-3 px-4">{r.id}</td>
                <td className="py-3 px-4">{r.name}</td>
                <td className="py-3 px-4">{r.code}</td>
                <td className="py-3 px-4">{r.description ?? "—"}</td>
                <td className="py-3 px-4">
                  <button type="button" onClick={() => openEdit(r)} className="text-blue-600 hover:underline mr-2">
                    {t("admin_edit")}
                  </button>
                  <button type="button" onClick={() => openDelete(r)} className="text-red-600 hover:underline">
                    {t("admin_delete")}
                  </button>
                </td>
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
      </div>

      <Modal
        open={modal === "create" || modal === "edit"}
        title={modal === "create" ? t("admin_new_role_modal") : t("admin_edit_role_modal")}
        onClose={closeModal}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
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
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_description")}</label>
            <input
              type="text"
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_permissions")}</label>
            <div className="max-h-64 overflow-auto border border-gray-200 rounded-lg p-3 bg-gray-50">
              <PermissionTree
                nodes={permissionTree}
                selectedIds={form.permissionIds}
                onChange={(ids) => setForm((f) => ({ ...f, permissionIds: ids }))}
                getLabel={(p) => {
                  const key = permissionLabelKey(p.code);
                  const out = t(key);
                  return out === key ? p.name : out;
                }}
                labelSelectAll={t("admin_select_all")}
                labelUnselectAll={t("admin_unselect_all")}
              />
            </div>
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

      <Modal open={modal === "delete"} title={t("admin_confirm_delete")} onClose={closeModal}>
        {deleteTarget && (
          <>
            <p className="text-gray-700">{t("admin_delete_role_confirm").replace("{name}", deleteTarget.name)}</p>
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
