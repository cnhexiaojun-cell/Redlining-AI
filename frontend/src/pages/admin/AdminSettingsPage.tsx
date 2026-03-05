import { useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { useLanguage } from "../../contexts/LanguageContext";
import * as adminApi from "../../api/admin";
import type { SystemSettingDto } from "../../api/admin";

export default function AdminSettingsPage() {
  const { t } = useLanguage();
  const { token } = useAuth();
  const [settings, setSettings] = useState<SystemSettingDto[]>([]);
  const [values, setValues] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    adminApi
      .getSettings(token)
      .then((list) => {
        setSettings(list);
        const init: Record<string, string> = {};
        list.forEach((s) => {
          init[s.key] = s.value ?? "";
        });
        setValues(init);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const updates: Record<string, string> = {};
      settings.forEach((s) => {
        updates[s.key] = values[s.key] ?? "";
      });
      await adminApi.updateSettings(token, updates);
      setMessage(t("admin_settings_saved"));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  const updateValue = (key: string, value: string) => {
    setValues((v) => ({ ...v, [key]: value }));
  };

  if (loading) return <div className="text-gray-500">{t("admin_loading")}</div>;
  if (error && settings.length === 0) return <div className="text-red-600">{error}</div>;

  return (
    <div>
      <h1 className="text-xl font-bold text-gray-900 mb-4">{t("admin_settings_title")}</h1>

      {error && (
        <div className="mb-4 p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
      )}
      {message && (
        <div className="mb-4 p-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>
      )}

      {settings.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <p className="text-gray-500">{t("admin_settings_no_editable")}</p>
        </div>
      ) : (
        <form onSubmit={handleSave} className="bg-white rounded-xl border border-gray-200 p-6">
          <div className="space-y-4">
            {settings.map((s) => (
              <div key={s.id} className="flex flex-col gap-1">
                <label className="text-sm font-medium text-gray-700">
                  {s.key}
                  {s.description && (
                    <span className="ml-2 font-normal text-gray-500">— {s.description}</span>
                  )}
                </label>
                <input
                  type="text"
                  value={values[s.key] ?? ""}
                  onChange={(e) => updateValue(s.key, e.target.value)}
                  className="border border-gray-300 rounded-lg px-3 py-2 text-sm w-full max-w-md"
                />
              </div>
            ))}
          </div>
          <div className="mt-6">
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {saving ? t("admin_submitting") : t("admin_save")}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
