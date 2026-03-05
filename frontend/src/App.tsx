import React, { useEffect, useRef, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useLocation, Navigate } from 'react-router-dom';
import UploadPage from './pages/UploadPage';
import ProfilePage from './pages/ProfilePage';
import ScanningPage from './pages/ScanningPage';
import ReportPage from './pages/ReportPage';
import DocumentsPage from './pages/DocumentsPage';
import DocumentEditPage from './pages/DocumentEditPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminLayout, { AdminIndexRedirect } from './components/AdminLayout';
import AdminOrganizationsPage from './pages/admin/AdminOrganizationsPage';
import AdminUsersPage from './pages/admin/AdminUsersPage';
import AdminRolesPage from './pages/admin/AdminRolesPage';
import AdminLogsPage from './pages/admin/AdminLogsPage';
import AdminPlansPage from './pages/admin/AdminPlansPage';
import AdminSettingsPage from './pages/admin/AdminSettingsPage';
import PlanCenterPage from './pages/PlanCenterPage';
import OrderHistoryPage from './pages/OrderHistoryPage';
import ReportsPage from './pages/ReportsPage';
import RedliningLogo from './assets/Redlining.png';
import { useLanguage } from './contexts/LanguageContext';
import { useAuth, AuthProvider } from './contexts/AuthContext';

const ADMIN_MENU_CODES = [
  'admin:menu',
  'admin:organizations:menu',
  'admin:users:menu',
  'admin:roles:menu',
  'admin:plans:menu',
  'admin:logs:menu',
  'admin:settings:menu',
];

function canAccessAdmin(hasMenuPermission: (code: string) => boolean, isSuperAdmin: () => boolean) {
  return isSuperAdmin() || ADMIN_MENU_CODES.some((code) => hasMenuPermission(code));
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const { token, loading, hasMenuPermission, isSuperAdmin } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (!canAccessAdmin(hasMenuPermission, isSuperAdmin)) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

function TopNavigation() {
  const location = useLocation();
  const { locale, setLocale, t } = useLanguage();
  const { user, logout, hasMenuPermission, isSuperAdmin } = useAuth();
  const showAdmin = user && canAccessAdmin(hasMenuPermission, isSuperAdmin);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const isActive = (path: string) => location.pathname === path;

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    }
    if (dropdownOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [dropdownOpen]);

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="w-full px-6 h-16 flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link to="/" className="flex items-center gap-2">
            <img src={RedliningLogo} alt="Redlining AI" className="w-8 h-8 rounded-lg" />
            <span className="text-xl font-bold text-gray-900 tracking-tight">Redlining AI</span>
          </Link>

          {user && (
            <nav className="hidden md:flex items-center gap-1">
              <Link
                to="/"
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive('/') ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                }`}
              >
                {t('nav_upload')}
              </Link>
              <Link
                to="/scanning"
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive('/scanning') ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                }`}
              >
                {t('nav_scanning')}
              </Link>
              <Link
                to="/reports"
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive('/reports') ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                }`}
              >
                {t('nav_report')}
              </Link>
              <Link
                to="/documents"
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive('/documents') ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                }`}
              >
                {t('nav_documents')}
              </Link>
              <Link
                to="/plans"
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive('/plans') ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                }`}
              >
                {t('plan_center')}
              </Link>
              {showAdmin && (
                <Link
                  to="/admin"
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    location.pathname.startsWith('/admin') ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
                >
                  {t('admin_system')}
                </Link>
              )}
            </nav>
          )}
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1 rounded-lg border border-gray-200 p-0.5 bg-gray-50">
            <button
              type="button"
              onClick={() => setLocale('zh')}
              className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
                locale === 'zh' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              中文
            </button>
            <button
              type="button"
              onClick={() => setLocale('en')}
              className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
                locale === 'en' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              English
            </button>
          </div>
          {user ? (
            <div className="relative flex items-center gap-3 pl-2" ref={dropdownRef}>
              <div className="text-right hidden sm:block">
                <p className="text-sm font-medium text-gray-900">
                  {user.realName || user.username || user.email || '—'}
                </p>
                <p className="text-xs text-gray-500">{user?.planName ?? t('nav_plan_default')}</p>
              </div>
              <button
                type="button"
                onClick={() => setDropdownOpen((o) => !o)}
                className="flex-shrink-0 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500"
                aria-expanded={dropdownOpen}
                aria-haspopup="true"
              >
                {user.avatarUrl ? (
                  <img
                    src={user.avatarUrl}
                    alt=""
                    className="w-9 h-9 rounded-full object-cover border border-gray-200"
                  />
                ) : (
                  <div className="w-9 h-9 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 font-bold">
                    {(user.realName || user.username || user.email || '?').slice(0, 2).toUpperCase()}
                  </div>
                )}
              </button>
              {dropdownOpen && (
                <div className="absolute right-6 top-14 mt-1 w-40 bg-white rounded-xl border border-gray-200 shadow-lg py-1 z-50">
                  <Link
                    to="/profile"
                    className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                    onClick={() => setDropdownOpen(false)}
                  >
                    {t('nav_profile')}
                  </Link>
                  <button
                    type="button"
                    onClick={() => {
                      setDropdownOpen(false);
                      logout();
                    }}
                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                  >
                    {t('auth_logout')}
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              >
                {t('auth_login')}
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
              >
                {t('auth_register')}
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <div className="min-h-screen bg-gray-50 flex flex-col">
          <TopNavigation />
          <main className="flex-1 w-full mx-auto">
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/" element={<ProtectedRoute><UploadPage /></ProtectedRoute>} />
              <Route path="/scanning" element={<ProtectedRoute><ScanningPage /></ProtectedRoute>} />
              <Route path="/report/:id" element={<ProtectedRoute><ReportPage /></ProtectedRoute>} />
              <Route path="/report" element={<ProtectedRoute><ReportPage /></ProtectedRoute>} />
              <Route path="/reports" element={<ProtectedRoute><ReportsPage /></ProtectedRoute>} />
              <Route path="/documents" element={<ProtectedRoute><DocumentsPage /></ProtectedRoute>} />
              <Route path="/documents/:id/preview" element={<ProtectedRoute><DocumentEditPage /></ProtectedRoute>} />
              <Route path="/documents/:id/edit" element={<ProtectedRoute><DocumentEditPage /></ProtectedRoute>} />
              <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
              <Route path="/plans" element={<ProtectedRoute><PlanCenterPage /></ProtectedRoute>} />
              <Route path="/plans/orders" element={<ProtectedRoute><OrderHistoryPage /></ProtectedRoute>} />
              <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
                <Route index element={<AdminIndexRedirect />} />
                <Route path="organizations" element={<AdminOrganizationsPage />} />
                <Route path="users" element={<AdminUsersPage />} />
                <Route path="roles" element={<AdminRolesPage />} />
                <Route path="logs" element={<AdminLogsPage />} />
                <Route path="plans" element={<AdminPlansPage />} />
                <Route path="settings" element={<AdminSettingsPage />} />
              </Route>
            </Routes>
          </main>
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;
