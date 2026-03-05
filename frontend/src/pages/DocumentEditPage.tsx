import { useEffect, useRef, useState } from "react";
import { useParams, Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { getEditorConfig, type EditorConfig } from "../api/documents";
import { useLanguage } from "../contexts/LanguageContext";

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (id: string, config: Record<string, unknown>) => void;
    };
  }
}

export default function DocumentEditPage() {
  const { id } = useParams<{ id: string }>();
  const { pathname } = useLocation();
  const { token } = useAuth();
  const { t } = useLanguage();
  const containerRef = useRef<HTMLDivElement>(null);
  const [config, setConfig] = useState<EditorConfig | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [scriptLoaded, setScriptLoaded] = useState(false);

  const viewOnly = pathname.includes("/preview");

  useEffect(() => {
    if (!id || !token) return;
    getEditorConfig(id, token, { mode: viewOnly ? "view" : "edit" })
      .then(setConfig)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, [id, token, viewOnly]);

  const serverUrl = config?.documentServerUrl ?? "";
  useEffect(() => {
    if (!serverUrl) return;
    const scriptId = "onlyoffice-api-script";
    if (document.getElementById(scriptId)) {
      setScriptLoaded(true);
      return;
    }
    const script = document.createElement("script");
    script.id = scriptId;
    script.type = "text/javascript";
    script.src = `${serverUrl}/web-apps/apps/api/documents/api.js`;
    script.onload = () => setScriptLoaded(true);
    script.onerror = () => setError("Failed to load OnlyOffice script");
    document.head.appendChild(script);
    return () => {
      const el = document.getElementById(scriptId);
      if (el) el.remove();
    };
  }, [serverUrl]);

  useEffect(() => {
    if (!config || !scriptLoaded || !containerRef.current || !window.DocsAPI) return;
    const { documentServerUrl: _, ...editorConfig } = config;
    try {
      new window.DocsAPI.DocEditor("onlyoffice-editor-placeholder", editorConfig);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, [config, scriptLoaded]);

  if (error) {
    return (
      <div className="max-w-2xl mx-auto px-6 py-12 text-center">
        <p className="text-red-600 mb-4">{error}</p>
        <Link to="/documents" className="text-blue-600 hover:underline">
          {t("documents_back")}
        </Link>
      </div>
    );
  }

  if (!config) {
    return (
      <div className="max-w-2xl mx-auto px-6 py-12 text-center text-gray-500">
        {t("documents_editor_loading")}
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)]">
      <div className="flex items-center gap-4 px-6 py-2 border-b border-gray-200 bg-white">
        <Link to="/documents" className="text-sm text-blue-600 hover:underline">
          ← {t("documents_back")}
        </Link>
        <span className="text-sm text-gray-600 truncate">{config.document?.title ?? id}</span>
        {viewOnly && (
          <Link
            to={`/documents/${id}/edit`}
            className="ml-auto text-sm font-medium text-blue-600 hover:underline"
          >
            {t("documents_edit")}
          </Link>
        )}
      </div>
      <div ref={containerRef} id="onlyoffice-editor-placeholder" className="flex-1 min-h-0" />
    </div>
  );
}
