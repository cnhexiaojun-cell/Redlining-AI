import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, FileText, Settings, ArrowRight, Loader2, Maximize2, Minimize2 } from 'lucide-react';
import { useLanguage } from '../contexts/LanguageContext';
import { useAuth } from '../contexts/AuthContext';
import { extractContractText } from '../api/extract';
import { uploadPreviewDocument, AUTH_REQUIRED_ERROR, type OnlyOfficePreviewConfig } from '../api/preview';
import OnlyOfficePreview, { DEFAULT_ONLYOFFICE_SERVER_URL, ONLYOFFICE_SCRIPT_ID } from '../components/OnlyOfficePreview';

const ONLYOFFICE_PREVIEW_EXT = ['.pdf', '.docx', '.doc'];

/** Preload OnlyOffice api.js when upload page mounts so first file (PDF or DOCX) can preview immediately. */
function usePreloadOnlyOfficeScript() {
  React.useEffect(() => {
    const serverUrl = DEFAULT_ONLYOFFICE_SERVER_URL;
    const scriptUrl = `${serverUrl}/web-apps/apps/api/documents/api.js`;
    const el = document.getElementById(ONLYOFFICE_SCRIPT_ID) as HTMLScriptElement | null;
    if (el && (el.src === scriptUrl || el.src.includes("/api/documents/api.js"))) return;
    if (el) return;
    const script = document.createElement("script");
    script.id = ONLYOFFICE_SCRIPT_ID;
    script.type = "text/javascript";
    script.src = scriptUrl;
    document.head.appendChild(script);
  }, []);
}

function useOnlyOfficePreview(file: File | null, token: string | null) {
  const [config, setConfig] = React.useState<OnlyOfficePreviewConfig | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  useEffect(() => {
    if (!file || !token) {
      setConfig(null);
      setLoading(false);
      setError(null);
      return;
    }
    const ext = file.name.includes('.') ? file.name.substring(file.name.lastIndexOf('.')).toLowerCase() : '';
    if (!ONLYOFFICE_PREVIEW_EXT.includes(ext)) {
      setConfig(null);
      setLoading(false);
      setError(null);
      return;
    }
    if (abortRef.current) abortRef.current.abort();
    const ac = new AbortController();
    abortRef.current = ac;
    setLoading(true);
    setError(null);
    setConfig(null);
    uploadPreviewDocument(file, token, { signal: ac.signal })
      .then((c) => {
        if (!ac.signal.aborted) {
          setConfig(c);
          setError(null);
        }
      })
      .catch((err) => {
        if (!ac.signal.aborted && err.name !== 'AbortError') {
          setError(err instanceof Error ? err.message : 'Preview failed');
          setConfig(null);
        }
      })
      .finally(() => {
        if (!ac.signal.aborted) setLoading(false);
        if (abortRef.current === ac) abortRef.current = null;
      });
    return () => {
      ac.abort();
    };
  }, [file, token]);
  return { config, loading, error };
}

function useTextPreview(file: File | null, token: string | null, t: (k: string) => string) {
  const [previewText, setPreviewText] = React.useState<string | null>(null);
  const [previewLoading, setPreviewLoading] = React.useState(false);
  const [previewError, setPreviewError] = React.useState<string | null>(null);
  const extractAbortRef = useRef<AbortController | null>(null);
  useEffect(() => {
    if (!file || !token) {
      setPreviewText(null);
      setPreviewError(null);
      setPreviewLoading(false);
      return;
    }
    const ext = file.name.includes('.') ? file.name.substring(file.name.lastIndexOf('.')).toLowerCase() : '';
    if (ext !== '.txt') {
      setPreviewText(null);
      setPreviewError(null);
      setPreviewLoading(false);
      return;
    }
    if (extractAbortRef.current) extractAbortRef.current.abort();
    const ac = new AbortController();
    extractAbortRef.current = ac;
    setPreviewLoading(true);
    setPreviewError(null);
    setPreviewText(null);
    extractContractText(file, { token, signal: ac.signal })
      .then((data) => {
        if (!ac.signal.aborted) {
          setPreviewText(data.text);
          setPreviewError(null);
        }
      })
      .catch((err) => {
        if (!ac.signal.aborted && err.name !== 'AbortError') {
          setPreviewError(err instanceof Error ? err.message : t('upload_preview_error'));
          setPreviewText(null);
        }
      })
      .finally(() => {
        if (!ac.signal.aborted) setPreviewLoading(false);
        if (extractAbortRef.current === ac) extractAbortRef.current = null;
      });
    return () => {
      ac.abort();
    };
  }, [file, token, t]);
  return { previewText, previewLoading, previewError };
}

export default function UploadPage() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  const { token, logout } = useAuth();
  const [stance, setStance] = React.useState('neutral');
  const [file, setFile] = React.useState<File | null>(null);
  const [advancedRules, setAdvancedRules] = React.useState('');
  const fileInputRef = React.useRef<HTMLInputElement>(null);
  const previewContainerRef = React.useRef<HTMLDivElement>(null);
  const [isPreviewFullscreen, setIsPreviewFullscreen] = React.useState(false);

  React.useEffect(() => {
    const onFullscreenChange = () => {
      setIsPreviewFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', onFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange);
  }, []);

  const togglePreviewFullscreen = () => {
    if (!previewContainerRef.current) return;
    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else {
      previewContainerRef.current.requestFullscreen();
    }
  };

  usePreloadOnlyOfficeScript();
  const onlyOffice = useOnlyOfficePreview(file, token);
  const textPreview = useTextPreview(file, token, t);

  const ext = file ? (file.name.includes('.') ? file.name.substring(file.name.lastIndexOf('.')).toLowerCase() : '') : '';
  const useOnlyOffice = !!file && !!token && ONLYOFFICE_PREVIEW_EXT.includes(ext);
  const useText = !!file && !!token && ext === '.txt';
  const previewLoading = useOnlyOffice ? onlyOffice.loading : useText ? textPreview.previewLoading : false;
  const previewError = useOnlyOffice ? onlyOffice.error : useText ? textPreview.previewError : null;

  const handleReview = () => {
    if (!file) {
      alert(t('upload_alert_no_file'));
      return;
    }
    navigate('/scanning', { state: { file, stance, advancedRules } });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setFile(e.dataTransfer.files[0]);
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto px-6 py-8">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 h-[calc(100vh-8rem)]">
        {/* Left Column: Upload Area + Preview */}
        <div className="lg:col-span-8 flex flex-col h-full min-h-0">
          <div className="mb-6 flex-shrink-0">
            <h1 className="text-3xl font-bold text-gray-900">{t('upload_title')}</h1>
            <p className="text-gray-500 mt-2">{t('upload_subtitle')}</p>
          </div>

          <div 
            className={`border-2 border-dashed rounded-2xl flex flex-col items-center justify-center transition-all cursor-pointer group relative overflow-hidden bg-white flex-shrink-0 ${
              file ? 'border-blue-500 bg-blue-50/30 h-28' : 'flex-1 border-gray-300 hover:border-blue-400 hover:bg-gray-50'
            }`}
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => e.preventDefault()}
            onDrop={handleDrop}
          >
            <input
              type="file"
              ref={fileInputRef}
              className="hidden"
              onChange={handleFileChange}
              accept=".pdf,.docx,.doc,.txt"
            />
            
            <div className="z-10 text-center space-y-4">
              <div className={`w-20 h-20 rounded-full flex items-center justify-center mx-auto transition-colors ${
                file ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-400 group-hover:bg-blue-50 group-hover:text-blue-500'
              }`}>
                {file ? <FileText className="w-10 h-10" /> : <Upload className="w-10 h-10" />}
              </div>
              <div>
                <h3 className="text-xl font-semibold text-gray-900">
                  {file ? file.name : t('upload_drop_here')}
                </h3>
                <p className="text-gray-500 mt-2">
                  {file ? `${(file.size / 1024 / 1024).toFixed(2)} MB` : t('upload_support')}
                </p>
              </div>
              {!file && (
                <button className="px-6 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-50 transition-colors shadow-sm">
                  {t('upload_browse')}
                </button>
              )}
            </div>
          </div>

          {/* Contract preview: OnlyOffice plugin for PDF/DOCX/DOC, text for TXT */}
          {file && (
            <div
              ref={previewContainerRef}
              className="mt-4 flex-1 flex flex-col min-h-0 bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden"
            >
              <div className="px-4 py-3 border-b border-gray-100 flex-shrink-0 flex items-center justify-between gap-2">
                <h3 className="text-lg font-bold text-gray-900">
                  {t('upload_preview_title')}
                </h3>
                {(onlyOffice.config || textPreview.previewText) && !previewLoading && (
                  <button
                    type="button"
                    onClick={togglePreviewFullscreen}
                    className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition-colors"
                    title={isPreviewFullscreen ? '退出全屏' : '全屏预览'}
                    aria-label={isPreviewFullscreen ? '退出全屏' : '全屏预览'}
                  >
                    {isPreviewFullscreen ? (
                      <Minimize2 className="w-5 h-5" />
                    ) : (
                      <Maximize2 className="w-5 h-5" />
                    )}
                  </button>
                )}
              </div>
              <div className="flex-1 flex flex-col min-h-0 p-4">
                {previewLoading && (
                  <div className="flex items-center justify-center gap-2 py-12 text-gray-500 flex-1">
                    <Loader2 className="w-6 h-6 animate-spin" />
                    <span>{t('upload_preview_loading')}</span>
                  </div>
                )}
                {previewError && !previewLoading && (
                  <div className="text-sm bg-red-50 border border-red-100 rounded-xl px-4 py-3 flex-shrink-0 space-y-2">
                    <p className="text-red-600">
                      {previewError === AUTH_REQUIRED_ERROR ? t('auth_session_expired') : previewError}
                    </p>
                    {previewError === AUTH_REQUIRED_ERROR && (
                      <button
                        type="button"
                        onClick={() => logout()}
                        className="text-blue-600 font-medium hover:underline"
                      >
                        {t('auth_login')}
                      </button>
                    )}
                  </div>
                )}
                {onlyOffice.config && !previewLoading && (
                  <div className="flex-1 min-h-0 rounded-xl border border-gray-100 overflow-hidden flex flex-col">
                    <OnlyOfficePreview
                      key={onlyOffice.config.document.key}
                      config={onlyOffice.config}
                      containerId="contract-preview-onlyoffice"
                      className="w-full h-full min-h-0 flex-1"
                    />
                  </div>
                )}
                {textPreview.previewText && !previewLoading && !onlyOffice.config && (
                  <div
                    className="flex-1 min-h-0 overflow-y-auto rounded-xl border border-gray-100 bg-gray-50/50 p-4 text-sm text-gray-800 whitespace-pre-wrap font-sans"
                    role="region"
                    aria-label={t('upload_preview_title')}
                  >
                    {textPreview.previewText}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Configuration */}
        <div className="lg:col-span-4 flex flex-col h-full bg-white border-l border-gray-100 pl-8 lg:pl-0 lg:border-none lg:bg-transparent">
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm h-full flex flex-col">
            <div className="flex items-center gap-2 mb-6 text-gray-900">
              <Settings className="w-5 h-5" />
              <h2 className="text-lg font-bold">{t('upload_config')}</h2>
            </div>

            <div className="space-y-8 flex-1">
              <div>
                <label className="block text-sm font-bold text-gray-700 mb-3">{t('upload_stance')}</label>
                <div className="space-y-3">
                  {[
                    { id: 'party-a', labelKey: 'upload_stance_party_a', descKey: 'upload_stance_party_a_desc' },
                    { id: 'party-b', labelKey: 'upload_stance_party_b', descKey: 'upload_stance_party_b_desc' },
                    { id: 'neutral', labelKey: 'upload_stance_neutral', descKey: 'upload_stance_neutral_desc' }
                  ].map((s) => (
                    <div 
                      key={s.id}
                      onClick={() => setStance(s.id)}
                      className={`p-4 rounded-xl border cursor-pointer transition-all ${
                        stance === s.id
                          ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500'
                          : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className={`font-semibold ${stance === s.id ? 'text-blue-700' : 'text-gray-900'}`}>
                          {t(s.labelKey)}
                        </span>
                        {stance === s.id && <div className="w-2 h-2 rounded-full bg-blue-600" />}
                      </div>
                      <p className="text-xs text-gray-500">{t(s.descKey)}</p>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-3">
                  {t('upload_advanced_rules')}
                </label>
                <textarea
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all resize-none bg-gray-50"
                  rows={4}
                  placeholder={t('upload_advanced_rules_placeholder')}
                  value={advancedRules}
                  onChange={(e) => setAdvancedRules(e.target.value)}
                />
              </div>
            </div>

            <div className="pt-6 mt-6 border-t border-gray-100">
              <button
                onClick={handleReview}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-4 px-6 rounded-xl shadow-lg hover:shadow-xl flex items-center justify-center gap-2 transition-all"
              >
                <span>{t('upload_start_review')}</span>
                <ArrowRight className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
