import { useLocation, useNavigate, useParams, Link } from 'react-router-dom';
import { useRef, useState, useEffect } from 'react';
import { AlertTriangle, CheckCircle, ArrowLeft, Download, Printer, Share2, Copy, Maximize2, Minimize2 } from 'lucide-react';
import type { AnalysisResult } from '../types/analysis';
import { useLanguage } from '../contexts/LanguageContext';
import { useAuth } from '../contexts/AuthContext';
import { getReport } from '../api/reports';
import OnlyOfficePreview from '../components/OnlyOfficePreview';
import type { OnlyOfficePreviewConfig } from '../api/preview';

type SeverityKey = 'high' | 'medium' | 'low';

function severityToType(severity: string): SeverityKey {
  if (severity === '高') return 'high';
  if (severity === '中') return 'medium';
  return 'low';
}

function countBySeverity(risks: AnalysisResult['risks']) {
  let high = 0;
  let medium = 0;
  let low = 0;
  for (const r of risks) {
    if (r.severity === '高') high++;
    else if (r.severity === '中') medium++;
    else low++;
  }
  return { high, medium, low };
}

interface ReportState {
  analysisResult: AnalysisResult;
  fileName?: string;
  onlyOfficeConfig?: OnlyOfficePreviewConfig;
}

const REPORT_STATE_KEY = 'report_page_state';

function loadReportStateFromStorage(): ReportState | null {
  try {
    const raw = sessionStorage.getItem(REPORT_STATE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as ReportState;
    if (!parsed?.analysisResult) return null;
    return parsed;
  } catch {
    return null;
  }
}

export default function ReportPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const params = useParams<{ id?: string }>();
  const { token } = useAuth();
  const { locale, t } = useLanguage();
  const stateFromLocation = location.state as ReportState | null;
  const stateFromStorage = loadReportStateFromStorage();
  const onlyOfficeWrapperRef = useRef<HTMLDivElement>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [fetchedState, setFetchedState] = useState<ReportState | null>(null);
  const [reportLoadError, setReportLoadError] = useState<string | null>(null);

  useEffect(() => {
    const id = params.id;
    if (!id) {
      setFetchedState(null);
      setReportLoadError(null);
      return;
    }
    if (!token) {
      setFetchedState(null);
      setReportLoadError(t('report_not_found'));
      return;
    }
    const reportId = Number(id);
    if (Number.isNaN(reportId)) {
      setReportLoadError(t('report_not_found'));
      return;
    }
    setReportLoadError(null);
    const locState = location.state as ReportState | null;
    getReport(token, reportId)
      .then((res) => {
        setFetchedState({
          analysisResult: res.result,
          fileName: res.fileName,
          onlyOfficeConfig: res.onlyOfficeConfig ?? locState?.onlyOfficeConfig,
        });
      })
      .catch(() => setReportLoadError(t('report_not_found')));
  }, [params.id, token, t, location.state]);

  useEffect(() => {
    const onFullscreenChange = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', onFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange);
  }, []);

  const locationState = location.state as ReportState | null;
  const state = params.id ? fetchedState : (stateFromLocation ?? stateFromStorage);
  const analysisResult = state?.analysisResult;
  const fileName = state?.fileName ?? t('report_contract');
  const onlyOfficeConfig =
    state?.onlyOfficeConfig ?? (params.id ? locationState?.onlyOfficeConfig : undefined);
  const loadingById = params.id && !fetchedState && !reportLoadError;

  if (loadingById) {
    return (
      <div className="w-full max-w-7xl mx-auto px-6 py-8">
        <div className="text-gray-500 py-8 text-center">{t('admin_loading')}</div>
      </div>
    );
  }

  if (params.id && reportLoadError) {
    return (
      <div className="w-full max-w-7xl mx-auto px-6 py-8">
        <div className="bg-white rounded-2xl border border-gray-200 p-8 text-center">
          <p className="text-gray-600 mb-4">{reportLoadError}</p>
          <Link to="/reports" className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 inline-block">
            {t('reports_list_title')}
          </Link>
        </div>
      </div>
    );
  }

  if (!analysisResult) {
    return (
      <div className="w-full max-w-7xl mx-auto px-6 py-8">
        <div className="bg-white rounded-2xl border border-gray-200 p-8 text-center">
          <p className="text-gray-600 mb-4">{t('report_no_data')}</p>
          <button
            onClick={() => {
              try {
                sessionStorage.removeItem(REPORT_STATE_KEY);
              } catch {
                /* ignore */
              }
              navigate('/');
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700"
          >
            {t('report_back_upload')}
          </button>
        </div>
      </div>
    );
  }

  const risks = Array.isArray(analysisResult.risks) ? analysisResult.risks : [];
  const counts = countBySeverity(risks);
  const suggestions = risks.map((r, i) => ({
    id: i + 1,
    type: severityToType(r.severity) as SeverityKey,
    title: r.type,
    description: [r.description, r.suggestion].filter(Boolean).join(' '),
    location: r.clause,
    suggestion: r.suggestion ?? '',
  }));

  const formatCommentItem = (item: typeof suggestions[0]) =>
    `[条款] ${item.location}\n[建议] ${item.description}`;
  const allCommentsText = suggestions.map(formatCommentItem).join('\n\n');
  const copySingle = (item: typeof suggestions[0]) => {
    navigator.clipboard.writeText(formatCommentItem(item));
  };
  const copyAll = () => navigator.clipboard.writeText(allCommentsText);
  const exportAnnotations = () => {
    const blob = new Blob([allCommentsText], { type: 'text/plain;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `批注清单-${fileName}.txt`;
    a.click();
    URL.revokeObjectURL(a.href);
  };

  const generatedAt = new Date().toLocaleDateString(locale === 'zh' ? 'zh-CN' : 'en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  return (
    <div className="w-full max-w-7xl mx-auto px-6 py-8 animate-in fade-in duration-500 flex flex-col h-[calc(100vh-6rem)] overflow-y-auto">
      <div className="flex items-center justify-between mb-6 shrink-0">
        <div className="flex items-center gap-4">
          <button
            onClick={() => {
              try {
                sessionStorage.removeItem(REPORT_STATE_KEY);
              } catch {
                /* ignore */
              }
              navigate('/reports');
            }}
            className="p-2 hover:bg-gray-100 rounded-full text-gray-500 transition-colors"
          >
            <ArrowLeft className="w-6 h-6" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{t('report_title')}</h1>
            <p className="text-sm text-gray-500">{t('report_generated')} {generatedAt} • {fileName}</p>
          </div>
        </div>
        <div className="flex gap-2">
          <button className="px-4 py-2 bg-white border border-gray-200 rounded-lg text-gray-700 font-medium hover:bg-gray-50 flex items-center gap-2">
            <Share2 className="w-4 h-4" />
            {t('report_share')}
          </button>
          <button className="px-4 py-2 bg-white border border-gray-200 rounded-lg text-gray-700 font-medium hover:bg-gray-50 flex items-center gap-2">
            <Printer className="w-4 h-4" />
            {t('report_print')}
          </button>
          <button className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 flex items-center gap-2">
            <Download className="w-4 h-4" />
            {t('report_download_pdf')}
          </button>
        </div>
      </div>

      {analysisResult.notice && (
        <div className="mb-6 p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-sm flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
          <span>{analysisResult.notice}</span>
        </div>
      )}

      <div className="mb-8 space-y-0">
        <div className="report-score-bar flex flex-wrap items-center gap-4 px-4 py-3 rounded-t-xl border border-b-0 border-gray-200 bg-gradient-to-r from-blue-50 to-white text-gray-800">
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold text-blue-600">{analysisResult.compliance_score}</span>
            <span className="text-lg text-gray-500">/100</span>
            <span className="text-sm font-medium text-gray-600 ml-1">{t('report_health_score')}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="flex items-center gap-1.5">
              <span className="font-medium text-red-700">{t('report_high_risks')}</span>
              <span className="font-bold text-red-600">{counts.high}</span>
            </span>
            <span className="flex items-center gap-1.5">
              <span className="font-medium text-orange-700">{t('report_medium_risks')}</span>
              <span className="font-bold text-orange-600">{counts.medium}</span>
            </span>
            <span className="flex items-center gap-1.5">
              <span className="font-medium text-green-700">{t('report_low_risks')}</span>
              <span className="font-bold text-green-600">{counts.low}</span>
            </span>
          </div>
        </div>
        {onlyOfficeConfig ? (
          <div
            ref={onlyOfficeWrapperRef}
            className="report-onlyoffice-wrapper border border-t-0 border-gray-200 bg-white shadow-sm flex flex-col min-h-[360px] h-[45vh] max-h-[45vh] overflow-hidden [&:fullscreen]:!max-h-none [&:fullscreen]:!h-full [&:fullscreen]:!rounded-none [&:fullscreen]:!m-0 [&:fullscreen]:!min-h-0 [&:fullscreen]:flex [&:fullscreen]:flex-col [&:fullscreen]:bg-white [&:fullscreen]:rounded-none"
          >
            <div className="report-onlyoffice-title relative z-10 overflow-visible border-b border-gray-100 px-4 py-2.5 bg-gray-50 text-sm text-gray-600 shrink-0 flex items-center gap-3 min-h-[3rem] leading-normal [&:fullscreen]:min-h-[3rem] [&:fullscreen]:flex-shrink-0">
              <span className="min-w-0 flex-1 break-words leading-normal align-middle">
                {fileName} — {t('report_annotation_in_doc')}
              </span>
              <button
                type="button"
                onClick={() => {
                  if (isFullscreen) document.exitFullscreen?.();
                  else onlyOfficeWrapperRef.current?.requestFullscreen?.();
                }}
                className="flex-shrink-0 p-2 rounded-lg text-gray-600 hover:bg-gray-200 hover:text-gray-900 transition-colors flex items-center justify-center"
                title={isFullscreen ? t('report_exit_fullscreen') : t('report_fullscreen')}
              >
                {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
              </button>
            </div>
            <div
              className="report-onlyoffice-body flex-1 min-h-0 w-full relative flex flex-col overflow-hidden z-0"
              style={isFullscreen ? { minHeight: 0 } : undefined}
            >
              <OnlyOfficePreview
                key={onlyOfficeConfig.document.key}
                config={onlyOfficeConfig}
                containerId="report-onlyoffice-doc"
                className="absolute inset-0 w-full h-full min-h-[280px]"
              />
            </div>
            <p className="text-xs text-gray-500 px-4 py-2 bg-gray-50/80 border-t border-gray-100">
              {t('report_onlyoffice_comment_hint')}
            </p>
          </div>
        ) : (
          <div className="rounded-b-xl border border-t-0 border-gray-200 bg-gray-50 px-4 py-6">
            <p className="text-sm text-gray-500">{t('report_no_doc_hint')}</p>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 shrink-0 relative z-20 bg-transparent">
        <div className="lg:col-span-12">
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
            <h3 className="text-lg font-bold text-gray-900 mb-3 px-6 pt-6">{t('report_exec_summary')}</h3>
            <div className="px-6 pb-4 border-b border-gray-100">
              <p className="text-sm text-gray-600 leading-relaxed">
                {analysisResult.summary || t('report_no_summary')}
              </p>
              {analysisResult.key_points?.length > 0 && (
                <ul className="mt-2 text-sm text-gray-600 list-disc list-inside space-y-0.5">
                  {analysisResult.key_points.map((point, i) => (
                    <li key={i}>{point}</li>
                  ))}
                </ul>
              )}
            </div>
            {/* 批注条款：始终显示在详细建议内 */}
            <div className="border-t border-gray-100">
              <div className="p-6 flex flex-wrap items-center justify-between gap-3 border-b border-gray-100">
                <h2 className="text-lg font-bold text-gray-900">{t('report_annotation_suggestions')}</h2>
                <div className="flex gap-2">
                  <button type="button" onClick={copyAll} className="text-sm font-medium text-gray-600 hover:text-gray-900 flex items-center gap-1.5 px-3 py-1.5 rounded-lg hover:bg-gray-100">
                    <Copy className="w-4 h-4" /> {t('report_copy_all_comments')}
                  </button>
                  <button type="button" onClick={exportAnnotations} className="text-sm font-medium text-gray-600 hover:text-gray-900 flex items-center gap-1.5 px-3 py-1.5 rounded-lg hover:bg-gray-100">
                    {t('report_export_annotations')}
                  </button>
                </div>
              </div>
              <div className="divide-y divide-gray-100">
                {suggestions.map((item) => (
                  <div key={item.id} className="p-6 hover:bg-gray-50 transition-colors group">
                    <div className="flex items-start gap-4">
                      <div className="mt-1">
                        {item.type === 'high' && <AlertTriangle className="w-5 h-5 text-red-500" />}
                        {item.type === 'medium' && <AlertTriangle className="w-5 h-5 text-orange-500" />}
                        {item.type === 'low' && <CheckCircle className="w-5 h-5 text-green-500" />}
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center justify-between mb-1">
                          <h4 className="text-base font-bold text-gray-900">{item.title}</h4>
                          <span className="text-xs font-mono text-gray-400 bg-gray-100 px-2 py-1 rounded">{item.location}</span>
                        </div>
                        <p className="text-gray-600 text-sm leading-relaxed mb-3">{item.description}</p>
                        <button type="button" onClick={() => copySingle(item)} className="text-xs font-medium text-blue-600 hover:underline">
                          {t('report_copy_comment')}
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            {analysisResult.missing_clauses?.length > 0 && (
              <div className="border-t border-gray-100 p-6">
                <h3 className="font-bold text-gray-900 mb-3">{t('report_suggested_clauses')}</h3>
                <ul className="space-y-2">
                  {analysisResult.missing_clauses.map((mc, i) => (
                    <li key={i} className="text-sm text-gray-600">
                      <span className="font-medium text-gray-900">{mc.clause}</span>
                      {mc.importance && (
                        <span className="text-gray-500 ml-2">({mc.importance})</span>
                      )}
                      — {mc.recommendation}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
