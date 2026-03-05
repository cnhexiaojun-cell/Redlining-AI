import { Link, Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useLanguage } from "../contexts/LanguageContext";

type NavItem =
  | { to: string; label: string; code: string; children?: never }
  | {
      label: string;
      code: string;
      to?: never;
      children: { to: string; label: string; code: string; button?: boolean }[];
    };

const ADMIN_NAV_KEYS: NavItem[] = [
  { to: "/admin/organizations", label: "admin_organizations", code: "admin:organizations:menu" },
  { to: "/admin/users", label: "admin_users", code: "admin:users:menu" },
  { to: "/admin/roles", label: "admin_roles", code: "admin:roles:menu" },
  { to: "/admin/plans", label: "admin_plans", code: "admin:plans:menu" },
  { to: "/admin/logs", label: "admin_logs", code: "admin:logs:menu" },
  { to: "/admin/settings", label: "admin_settings", code: "admin:settings:menu" },
];

function flattenFirstNavPath(
  items: NavItem[],
  canMenu: (code: string) => boolean,
  canButton: (code: string) => boolean
): string | null {
  for (const item of items) {
    if (item.to) {
      if (canMenu(item.code)) return item.to;
    } else if (item.children) {
      const canAny = item.children.some((c) => (c.button ? canButton(c.code) : canMenu(c.code)));
      if (canAny) {
        const first = item.children.find((c) => (c.button ? canButton(c.code) : canMenu(c.code)));
        return first ? first.to : null;
      }
    }
  }
  return null;
}

export function AdminIndexRedirect() {
  const { hasMenuPermission, hasButtonPermission, isSuperAdmin } = useAuth();
  const canMenu = (code: string) => isSuperAdmin() || hasMenuPermission(code);
  const canButton = (code: string) => isSuperAdmin() || hasButtonPermission(code);
  const first = flattenFirstNavPath(ADMIN_NAV_KEYS, canMenu, canButton);
  return first ? <Navigate to={first} replace /> : <Navigate to="/" replace />;
}

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const { hasMenuPermission, hasButtonPermission, isSuperAdmin, logout } = useAuth();

  const canMenu = (code: string) => isSuperAdmin() || hasMenuPermission(code);
  const canButton = (code: string) => isSuperAdmin() || hasButtonPermission(code);

  const isActive = (to: string) => {
    if (to === "/admin/roles") return location.pathname === "/admin/roles" || location.pathname.startsWith("/admin/roles/");
    return location.pathname === to;
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <aside className="w-56 bg-white border-r border-gray-200 flex flex-col shrink-0">
        <nav className="p-2 flex-1 pt-4">
          <div className="text-xs font-medium text-gray-500 uppercase tracking-wider px-3 py-2">{t("admin_system")}</div>
          {ADMIN_NAV_KEYS.map((item) => {
            if (item.to) {
              if (!canMenu(item.code)) return null;
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`block px-3 py-2 rounded-lg text-sm font-medium ${
                    location.pathname === item.to ? "bg-blue-50 text-blue-700" : "text-gray-700 hover:bg-gray-100"
                  }`}
                >
                  {t(item.label)}
                </Link>
              );
            }
            if (!item.children) return null;
            const visibleChildren = item.children.filter((c) => (c.button ? canButton(c.code) : canMenu(c.code)));
            if (visibleChildren.length === 0) return null;
            return (
              <div key={item.code} className="mb-1">
                <div className="px-3 py-1.5 text-xs font-medium text-gray-500 uppercase tracking-wider">
                  {t(item.label)}
                </div>
                {visibleChildren.map((child) => (
                  <Link
                    key={child.to + child.code}
                    to={child.to}
                    className={`block px-3 py-2 rounded-lg text-sm font-medium ml-0 ${
                      isActive(child.to) ? "bg-blue-50 text-blue-700" : "text-gray-700 hover:bg-gray-100"
                    }`}
                  >
                    {t(child.label)}
                  </Link>
                ))}
              </div>
            );
          })}
        </nav>
        <div className="p-2 border-t border-gray-100">
          <button
            type="button"
            onClick={() => navigate("/")}
            className="w-full text-left px-3 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg"
          >
            {t("admin_back_business")}
          </button>
          <button
            type="button"
            onClick={() => logout()}
            className="w-full text-left px-3 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg mt-1"
          >
            {t("auth_logout")}
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto">
        <div className="p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
