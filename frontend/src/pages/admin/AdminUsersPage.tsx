import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { useLanguage } from "../../contexts/LanguageContext";
import * as adminApi from "../../api/admin";
import type { UserAdminDto, OrganizationDto, RoleDto } from "../../api/admin";
import Modal from "../../components/Modal";
import PaginationBar, { PAGE_SIZE_OPTIONS } from "../../components/PaginationBar";

function OrgTreeSidebar({
  tree,
  selectedId,
  onSelect,
  expandedIds,
  onToggle,
  t,
}: {
  tree: OrganizationDto[];
  selectedId: number | null;
  onSelect: (id: number | null) => void;
  expandedIds: Set<number>;
  onToggle: (id: number) => void;
  t: (k: string) => string;
}) {
  function render(nodes: OrganizationDto[], depth: number) {
    return nodes.map((o) => {
      const hasChildren = o.children && o.children.length > 0;
      const isExpanded = expandedIds.has(o.id);
      const isSelected = selectedId === o.id;
      return (
        <div key={o.id} className="py-0.5" style={{ paddingLeft: depth * 12 }}>
          <div className="flex items-center gap-1">
            <span className="w-4 flex-shrink-0">
              {hasChildren ? (
                <button
                  type="button"
                  onClick={() => onToggle(o.id)}
                  className="text-gray-500 p-0 text-xs"
                >
                  {isExpanded ? "▼" : "▶"}
                </button>
              ) : (
                <span className="inline-block w-4" />
              )}
            </span>
            <button
              type="button"
              onClick={() => onSelect(o.id)}
              className={`text-left text-sm truncate flex-1 rounded px-1 py-0.5 ${
                isSelected ? "bg-blue-100 text-blue-800 font-medium" : "hover:bg-gray-100 text-gray-800"
              }`}
            >
              {o.name}
            </button>
          </div>
          {hasChildren && isExpanded && (
            <div className="border-l border-gray-200 ml-2">{render(o.children!, depth + 1)}</div>
          )}
        </div>
      );
    });
  }

  return (
    <div className="h-full flex flex-col bg-white rounded-xl border border-gray-200 overflow-hidden">
      <div className="p-2 border-b border-gray-100 font-medium text-gray-700 text-sm">
        {t("admin_org")}
      </div>
      <div className="flex-1 overflow-auto p-2">
        <button
          type="button"
          onClick={() => onSelect(null)}
          className={`w-full text-left text-sm rounded px-2 py-1.5 mb-1 ${
            selectedId === null ? "bg-blue-100 text-blue-800 font-medium" : "hover:bg-gray-100 text-gray-800"
          }`}
        >
          {t("admin_all")}
        </button>
        {tree.length > 0 && render(tree, 0)}
      </div>
    </div>
  );
}

export default function AdminUsersPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [users, setUsers] = useState<UserAdminDto[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [orgTree, setOrgTree] = useState<OrganizationDto[]>([]);
  const [orgExpanded, setOrgExpanded] = useState<Set<number>>(new Set());
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null);
  const [filters, setFilters] = useState<{ organizationId?: number; enabled?: boolean }>({});
  const [organizations, setOrganizations] = useState<OrganizationDto[]>([]);
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<"create" | "edit" | "resetPwd" | null>(null);
  const [editingUser, setEditingUser] = useState<UserAdminDto | null>(null);
  const [createForm, setCreateForm] = useState({
    username: "",
    email: "",
    password: "",
    realName: "",
    organizationId: "" as number | "",
    roleIds: [] as number[],
  });
  const [editForm, setEditForm] = useState({
    email: "",
    realName: "",
    enabled: true,
    organizationId: "" as number | "",
    roleIds: [] as number[],
  });
  const [resetPasswordUserId, setResetPasswordUserId] = useState<number | null>(null);
  const [resetPasswordValue, setResetPasswordValue] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const effectiveOrgId = selectedOrgId ?? filters.organizationId;
  const loadUsers = useCallback(() => {
    if (!token) return;
    setLoading(true);
    adminApi
      .getUsers(token, {
        page,
        size: pageSize,
        organizationId: effectiveOrgId ?? undefined,
        enabled: filters.enabled,
      })
      .then((res) => {
        setUsers(res.content);
        setTotal(res.totalElements);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token, page, pageSize, effectiveOrgId, filters.enabled]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    if (!token) return;
    Promise.all([
      adminApi.getOrganizationsTree(token),
      adminApi.getOrganizations(token),
      adminApi.getRoles(token, 0, 500).then((r) => r.content),
    ])
      .then(([tree, orgs, roleList]) => {
        setOrgTree(tree);
        setOrganizations(orgs);
        setRoles(roleList);
      })
      .catch(() => {});
  }, [token]);

  const onOrgSelect = useCallback((id: number | null) => {
    setSelectedOrgId(id);
    setFilters((f) => ({ ...f, organizationId: id ?? undefined }));
    setPage(0);
  }, []);

  const openCreate = () => {
    setCreateForm({
      username: "",
      email: "",
      password: "",
      realName: "",
      organizationId: selectedOrgId ?? "",
      roleIds: [],
    });
    setModal("create");
  };

  const openEdit = (u: UserAdminDto) => {
    setEditingUser(u);
    setEditForm({
      email: u.email ?? "",
      realName: u.realName ?? "",
      enabled: u.enabled,
      organizationId: u.organizationId ?? "",
      roleIds: u.roleIds ?? [],
    });
    setModal("edit");
  };

  const openResetPwd = (u: UserAdminDto) => {
    setResetPasswordUserId(u.id);
    setResetPasswordValue("");
    setModal("resetPwd");
  };

  const closeModal = () => {
    setModal(null);
    setEditingUser(null);
    setResetPasswordUserId(null);
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    try {
      await adminApi.createUser(token, {
        username: createForm.username.trim(),
        email: createForm.email.trim() || undefined,
        password: createForm.password || undefined,
        realName: createForm.realName.trim() || undefined,
        organizationId: createForm.organizationId || undefined,
        roleIds: createForm.roleIds.length ? createForm.roleIds : undefined,
      });
      closeModal();
      loadUsers();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token || !editingUser) return;
    setSubmitting(true);
    try {
      await adminApi.updateUser(token, editingUser.id, {
        email: editForm.email.trim() || undefined,
        realName: editForm.realName.trim() || undefined,
        enabled: editForm.enabled,
        organizationId: editForm.organizationId || undefined,
        roleIds: editForm.roleIds,
      });
      closeModal();
      loadUsers();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleResetPwd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token || resetPasswordUserId == null || !resetPasswordValue.trim()) return;
    setSubmitting(true);
    try {
      await adminApi.resetUserPassword(token, resetPasswordUserId, resetPasswordValue.trim());
      closeModal();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleRole = (form: "create" | "edit", roleId: number) => {
    if (form === "create") {
      setCreateForm((f) => ({
        ...f,
        roleIds: f.roleIds.includes(roleId) ? f.roleIds.filter((id) => id !== roleId) : [...f.roleIds, roleId],
      }));
    } else {
      setEditForm((f) => ({
        ...f,
        roleIds: f.roleIds.includes(roleId) ? f.roleIds.filter((id) => id !== roleId) : [...f.roleIds, roleId],
      }));
    }
  };

  if (error) return <div className="text-red-600">{error}</div>;

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-xl font-bold text-gray-900">{t("admin_users")}</h1>
        <button
          type="button"
          onClick={openCreate}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700"
        >
          {t("admin_new_user")}
        </button>
      </div>

      <div className="flex gap-4 flex-1 min-h-0">
        <aside className="w-56 flex-shrink-0 flex flex-col min-h-[400px]">
          <OrgTreeSidebar
            tree={orgTree}
            selectedId={selectedOrgId}
            onSelect={onOrgSelect}
            expandedIds={orgExpanded}
            onToggle={(id) => setOrgExpanded((prev) => { const n = new Set(prev); if (n.has(id)) n.delete(id); else n.add(id); return n; })}
            t={t}
          />
        </aside>
        <div className="flex-1 min-w-0 flex flex-col">
          <div className="bg-white rounded-xl border border-gray-200 p-4 mb-4 flex flex-wrap gap-4 items-end">
            <div>
              <label className="block text-xs text-gray-500 mb-1">{t("admin_status")}</label>
              <select
                value={filters.enabled === undefined ? "" : String(filters.enabled)}
                onChange={(e) =>
                  setFilters((f) => ({
                    ...f,
                    enabled: e.target.value === "" ? undefined : e.target.value === "true",
                  }))
                }
                className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                <option value="">{t("admin_all")}</option>
                <option value="true">{t("admin_enabled")}</option>
                <option value="false">{t("admin_disabled")}</option>
              </select>
            </div>
          </div>

          {loading && users.length === 0 ? (
            <div className="text-gray-500 py-8">{t("admin_loading")}</div>
          ) : (
            <>
              <div className="bg-white rounded-xl border border-gray-200 overflow-hidden flex-1">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_id")}</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_username")}</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_real_name")}</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_email")}</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_status")}</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-700">{t("admin_org")}</th>
                      <th className="text-left py-3 px-4 font-medium text-gray-700 w-32">{t("admin_actions")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50">
                        <td className="py-3 px-4">{u.id}</td>
                        <td className="py-3 px-4">{u.username ?? "—"}</td>
                        <td className="py-3 px-4">{u.realName ?? "—"}</td>
                        <td className="py-3 px-4">{u.email ?? "—"}</td>
                        <td className="py-3 px-4">
                          {u.superAdmin ? t("admin_super_admin") : u.enabled ? t("admin_enabled") : t("admin_disabled")}
                        </td>
                        <td className="py-3 px-4">{u.organizationName ?? "—"}</td>
                        <td className="py-3 px-4">
                          {!u.superAdmin && (
                            <>
                              <button type="button" onClick={() => openEdit(u)} className="text-blue-600 hover:underline mr-2">
                                {t("admin_edit")}
                              </button>
                              <button type="button" onClick={() => openResetPwd(u)} className="text-gray-600 hover:underline">
                                {t("admin_reset_password")}
                              </button>
                            </>
                          )}
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
            </>
          )}
        </div>
      </div>

      <Modal open={modal === "create"} title={t("admin_new_user_title")} onClose={closeModal}>
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_username")} *</label>
            <input
              type="text"
              required
              value={createForm.username}
              onChange={(e) => setCreateForm((f) => ({ ...f, username: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_email")}</label>
            <input
              type="email"
              value={createForm.email}
              onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("auth_password")}</label>
            <input
              type="password"
              value={createForm.password}
              onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              placeholder={t("admin_password_optional")}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_real_name")}</label>
            <input
              type="text"
              value={createForm.realName}
              onChange={(e) => setCreateForm((f) => ({ ...f, realName: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_org")}</label>
            <select
              value={createForm.organizationId}
              onChange={(e) => setCreateForm((f) => ({ ...f, organizationId: e.target.value ? Number(e.target.value) : "" }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            >
              <option value="">{t("admin_none")}</option>
              {organizations.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_roles_label")}</label>
            <div className="flex flex-wrap gap-2">
              {roles.map((r) => (
                <label key={r.id} className="inline-flex items-center gap-1 text-sm">
                  <input
                    type="checkbox"
                    checked={createForm.roleIds.includes(r.id)}
                    onChange={() => toggleRole("create", r.id)}
                  />
                  {r.name}
                </label>
              ))}
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
              {submitting ? t("admin_submitting") : t("admin_create")}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={modal === "edit"} title={t("admin_edit_user_title")} onClose={closeModal}>
        {editingUser && (
          <form onSubmit={handleEdit} className="space-y-4">
            <p className="text-sm text-gray-500">{t("admin_username_readonly")}: {editingUser.username}</p>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_email")}</label>
              <input
                type="email"
                value={editForm.email}
                onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_real_name")}</label>
              <input
                type="text"
                value={editForm.realName}
                onChange={(e) => setEditForm((f) => ({ ...f, realName: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="inline-flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={editForm.enabled}
                  onChange={(e) => setEditForm((f) => ({ ...f, enabled: e.target.checked }))}
                />
                <span className="text-sm font-medium text-gray-700">{t("admin_enabled")}</span>
              </label>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_org")}</label>
              <select
                value={editForm.organizationId}
                onChange={(e) => setEditForm((f) => ({ ...f, organizationId: e.target.value ? Number(e.target.value) : "" }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                <option value="">{t("admin_none")}</option>
                {organizations.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_roles_label")}</label>
              <div className="flex flex-wrap gap-2">
                {roles.map((r) => (
                  <label key={r.id} className="inline-flex items-center gap-1 text-sm">
                    <input
                      type="checkbox"
                      checked={editForm.roleIds.includes(r.id)}
                      onChange={() => toggleRole("edit", r.id)}
                    />
                    {r.name}
                  </label>
                ))}
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
        )}
      </Modal>

      <Modal open={modal === "resetPwd"} title={t("admin_reset_pwd_title")} onClose={closeModal}>
        <form onSubmit={handleResetPwd} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("admin_new_password")} *</label>
            <input
              type="password"
              required
              value={resetPasswordValue}
              onChange={(e) => setResetPasswordValue(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={closeModal} className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">
              {t("admin_cancel")}
            </button>
            <button
              type="submit"
              disabled={submitting || !resetPasswordValue.trim()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? t("admin_submitting") : t("admin_ok")}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
