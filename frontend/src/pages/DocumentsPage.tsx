import React, { useCallback, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { listDocuments, createDocument, type DocumentItem } from "../api/documents";
import { useLanguage } from "../contexts/LanguageContext";

export default function DocumentsPage() {
  const { token } = useAuth();
  const { t } = useLanguage();
  const [docs, setDocs] = useState<DocumentItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const list = await listDocuments(token);
      setDocs(list);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [token]);

  React.useEffect(() => {
    load();
  }, [load]);

  const onFileChange = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (!file || !token) return;
      e.target.value = "";
      setUploading(true);
      setError(null);
      try {
        await createDocument(file, token);
        await load();
      } catch (err) {
        setError(err instanceof Error ? err.message : String(err));
      } finally {
        setUploading(false);
      }
    },
    [token, load]
  );

  return (
    <div className="max-w-4xl mx-auto px-6 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{t("documents_title")}</h1>
        <label className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 cursor-pointer disabled:opacity-50">
          <input
            type="file"
            className="hidden"
            accept=".docx,.xlsx,.pptx,.doc,.xls,.ppt,.odt,.ods,.odp"
            onChange={onFileChange}
            disabled={uploading}
          />
          {uploading ? t("documents_uploading") : t("documents_upload")}
        </label>
      </div>
      <p className="text-gray-600 text-sm mb-6">{t("documents_subtitle")}</p>

      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-gray-500 py-8">{t("documents_loading")}</div>
      ) : docs.length === 0 ? (
        <div className="border border-dashed border-gray-300 rounded-xl p-12 text-center text-gray-500">
          {t("documents_empty")}
        </div>
      ) : (
        <ul className="border border-gray-200 rounded-xl divide-y divide-gray-200 bg-white overflow-hidden">
          {docs.map((doc) => (
            <li key={doc.id} className="flex items-center justify-between px-4 py-3 hover:bg-gray-50">
              <span className="font-medium text-gray-900 truncate flex-1">{doc.name}</span>
              <div className="ml-4 flex items-center gap-2">
                <Link
                  to={`/documents/${doc.id}/preview`}
                  className="px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg"
                >
                  {t("documents_preview")}
                </Link>
                <Link
                  to={`/documents/${doc.id}/edit`}
                  className="px-3 py-1.5 text-sm font-medium text-blue-600 hover:bg-blue-50 rounded-lg"
                >
                  {t("documents_edit")}
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
