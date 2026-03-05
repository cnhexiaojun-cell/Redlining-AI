import { useEffect, useRef, useState } from "react";
import type { OnlyOfficePreviewConfig } from "../api/preview";

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (id: string, config: Record<string, unknown>) => void;
    };
  }
}

export const ONLYOFFICE_SCRIPT_ID = "onlyoffice-api-script";
const SCRIPT_ID = ONLYOFFICE_SCRIPT_ID;

/** OnlyOffice document server URL, must match backend app.onlyoffice.document-server-url */
export const DEFAULT_ONLYOFFICE_SERVER_URL = "http://127.0.0.1:8080";

type Props = {
  config: OnlyOfficePreviewConfig;
  containerId: string;
  className?: string;
};

/**
 * Renders an OnlyOffice document in view mode. Loads the OnlyOffice script
 * and mounts DocEditor into the container. Use a stable containerId and
 * remount (e.g. via key) when config changes.
 */
const PREVIEW_LOAD_TIMEOUT_MS = 20000;

export default function OnlyOfficePreview({ config, containerId, className }: Props) {
  const [scriptLoaded, setScriptLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const mountedRef = useRef(true);
  const loadedRef = useRef(false);
  const loadTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const serverUrl = config.documentServerUrl ?? "";

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (loadTimeoutRef.current) clearTimeout(loadTimeoutRef.current);
    };
  }, []);

  useEffect(() => {
    if (!serverUrl) return;
    const scriptUrl = `${serverUrl}/web-apps/apps/api/documents/api.js`;
    const el = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;

    const setLoadedWhenReady = () => {
      if (window.DocsAPI && mountedRef.current) setScriptLoaded(true);
    };

    if (el && (el.src === scriptUrl || el.src.includes("/api/documents/api.js"))) {
      if (window.DocsAPI) setScriptLoaded(true);
      else {
        const r = (el as HTMLScriptElement & { readyState?: string }).readyState;
        if (r === "complete" || r === "loaded") setLoadedWhenReady();
        else el.addEventListener("load", setLoadedWhenReady);
      }
      return () => el.removeEventListener("load", setLoadedWhenReady);
    }
    if (el) {
      if (window.DocsAPI) setScriptLoaded(true);
      else {
        const r = (el as HTMLScriptElement & { readyState?: string }).readyState;
        if (r === "complete" || r === "loaded") setLoadedWhenReady();
        else el.addEventListener("load", setLoadedWhenReady);
      }
      return () => el.removeEventListener("load", setLoadedWhenReady);
    }

    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.type = "text/javascript";
    script.src = scriptUrl;
    script.onload = () => {
      if (mountedRef.current) setScriptLoaded(true);
    };
    script.onerror = () => {
      if (mountedRef.current) setError("Failed to load OnlyOffice script");
    };
    document.head.appendChild(script);
    return () => { /* keep script in head for reuse */ };
  }, [serverUrl]);

  useEffect(() => {
    if (!config || !scriptLoaded || !window.DocsAPI) return;
    const container = document.getElementById(containerId);
    if (!container) return;
    loadedRef.current = false;
    if (loadTimeoutRef.current) clearTimeout(loadTimeoutRef.current);
    loadTimeoutRef.current = setTimeout(() => {
      loadTimeoutRef.current = null;
      if (mountedRef.current && !loadedRef.current) {
        setError("预览加载超时（约 20 秒）。请确认 OnlyOffice (http://127.0.0.1:8080) 与后端 (8003) 已启动，且 OnlyOffice 已允许访问私有 IP。");
      }
    }, PREVIEW_LOAD_TIMEOUT_MS);

    const { documentServerUrl: _, ...editorConfig } = config;
    const configWithEvents = {
      ...editorConfig,
      events: {
        onDocumentReady: () => {
          loadedRef.current = true;
          if (loadTimeoutRef.current) {
            clearTimeout(loadTimeoutRef.current);
            loadTimeoutRef.current = null;
          }
        },
        onError: (event: { data?: { errorCode?: number; errorDescription?: string } }) => {
          if (loadTimeoutRef.current) {
            clearTimeout(loadTimeoutRef.current);
            loadTimeoutRef.current = null;
          }
          const code = event?.data?.errorCode;
          const desc = event?.data?.errorDescription ?? "";
          const msg = `OnlyOffice 错误 ${code != null ? code : ""}: ${desc || "文档加载失败"}`.trim();
          if (mountedRef.current) setError(msg);
        },
      },
    };
    let editorInstance: unknown;
    try {
      editorInstance = new window.DocsAPI.DocEditor(containerId, configWithEvents);
    } catch (e) {
      if (loadTimeoutRef.current) clearTimeout(loadTimeoutRef.current);
      setError(e instanceof Error ? e.message : String(e));
      return;
    }

    const ro = new ResizeObserver(() => {
      if (typeof (editorInstance as { resize?: () => void }).resize === "function") {
        (editorInstance as { resize: () => void }).resize();
      } else {
        window.dispatchEvent(new Event("resize"));
      }
    });
    ro.observe(container);
    return () => {
      ro.disconnect();
      if (loadTimeoutRef.current) {
        clearTimeout(loadTimeoutRef.current);
        loadTimeoutRef.current = null;
      }
    };
  }, [config, scriptLoaded, containerId]);

  if (error) {
    return (
      <div className={className} role="alert">
        <p className="text-sm text-red-600">{error}</p>
      </div>
    );
  }

  return (
    <div
      id={containerId}
      className={className}
      style={{
        width: "100%",
        minHeight: "360px",
        height: "100%",
        boxSizing: "border-box",
      }}
    />
  );
}
