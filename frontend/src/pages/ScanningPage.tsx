import React, { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { AlertTriangle, CheckCircle, Info, FileText, Activity, Search, ArrowRight } from 'lucide-react';
import { analyzeContract, type AnalyzeError } from '../api/analyze';
import { annotateDocument, uploadPreviewDocument, type OnlyOfficePreviewConfig } from '../api/preview';
import { linkReportDocument } from '../api/reports';
import type { AnalysisResult } from '../types/analysis';
import { useLanguage } from '../contexts/LanguageContext';
import { useAuth } from '../contexts/AuthContext';

interface RiskCardProps {
  title: string;
  count: number;
  color: string;
  icon: React.ElementType;
  bgClass: string;
  textClass: string;
}

const RiskCard = ({ title, count, color, icon: Icon, bgClass, textClass }: RiskCardProps) => (
  <div 
    className="bg-white rounded-xl border border-gray-100 p-6 flex flex-col items-start justify-between shadow-sm hover:shadow-md transition-shadow h-full"
  >
    <div className="flex items-center justify-between w-full mb-4">
      <div className={`p-3 rounded-lg ${bgClass} bg-opacity-50`}>
        <Icon className={`w-6 h-6 ${textClass}`} />
      </div>
      <motion.span 
        key={count}
        initial={{ scale: 1.5, color: color }}
        animate={{ scale: 1, color: '#1F2937' }}
        className="text-3xl font-bold text-gray-900"
      >
        {count}
      </motion.span>
    </div>
    <span className="font-medium text-gray-500">{title}</span>
  </div>
);

interface ScanningState {
  file: File;
  stance: string;
  advancedRules: string;
}

function countRisksBySeverity(risks: AnalysisResult['risks']) {
  let high = 0;
  let mediumHigh = 0;
  let medium = 0;
  let low = 0;
  for (const r of risks) {
    if (r.severity === '高') high++;
    else if (r.severity === '中') medium++;
    else low++;
  }
  return { high, mediumHigh: mediumHigh, medium, low };
}

export default function ScanningPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
  const { token, logout } = useAuth();
  const state = location.state as ScanningState | null;
  const [progress, setProgress] = useState(0);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [quotaExhausted, setQuotaExhausted] = useState(false);
  const [analysisResult, setAnalysisResult] = useState<AnalysisResult | null>(null);
  const [risks, setRisks] = useState({
    high: 0,
    mediumHigh: 0,
    medium: 0,
    low: 0,
  });
  const [logs, setLogs] = useState<string[]>([]);
  const [reportLoading, setReportLoading] = useState(false);
  const progressIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Redirect if no file (e.g. direct visit to /scanning)
  useEffect(() => {
    if (!state?.file) {
      navigate('/', { replace: true });
      return;
    }
  }, [state?.file, navigate]);

  // Progress animation while analysis runs
  useEffect(() => {
    if (!state?.file) return;
    progressIntervalRef.current = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 100) return 100;
        return prev + 0.5;
      });
    }, 50);
    return () => {
      if (progressIntervalRef.current) clearInterval(progressIntervalRef.current);
    };
  }, [state?.file]);

  // Call API and merge result into UI
  useEffect(() => {
    if (!state?.file) return;
    let cancelled = false;
    setError(null);
    analyzeContract(state.file, {
      stance: state.stance,
      advancedRules: state.advancedRules,
      token,
    })
      .then((result) => {
        if (cancelled) return;
        if (progressIntervalRef.current) {
          clearInterval(progressIntervalRef.current);
          progressIntervalRef.current = null;
        }
        setProgress(100);
        setRisks(countRisksBySeverity(result.risks));
        setLogs(
          result.risks.slice(0, 5).map(
            (r) => `[${r.severity}] ${r.type}: ${r.clause}`
          )
        );
        setAnalysisResult(result);
        setIsComplete(true);
      })
      .catch((err) => {
        if (cancelled) return;
        if ((err as AnalyzeError).status === 401) {
          logout();
          navigate('/login', { replace: true, state: { from: '/scanning' } });
          return;
        }
        const msg = err instanceof Error ? err.message : '';
        const isQuotaExhausted = (err as AnalyzeError).status === 403 && (msg === 'quota_exhausted' || msg.includes('quota_exhausted'));
        setQuotaExhausted(isQuotaExhausted);
        setError(isQuotaExhausted ? t('quota_exhausted_message') : (msg || t('scanning_error_title')));
        if (progressIntervalRef.current) {
          clearInterval(progressIntervalRef.current);
          progressIntervalRef.current = null;
        }
        setProgress(100);
        setIsComplete(true);
      });
    return () => {
      cancelled = true;
    };
  }, [state?.file, state?.stance, state?.advancedRules, token]);

  if (!state?.file) {
    return null;
  }

  return (
    <div className="w-full max-w-7xl mx-auto px-6 py-8">
      <div className="flex flex-col gap-8">
        {/* Header Section */}
        <div className="flex items-center justify-between bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
              <Activity className="w-6 h-6 text-blue-600 animate-pulse" />
              {t('scanning_title')}
            </h1>
            <p className="text-gray-500 mt-1">{t('scanning_subtitle')}</p>
          </div>
          <div className="text-right">
            <span className="text-4xl font-bold text-blue-600 tabular-nums">{Math.round(progress)}%</span>
            <span className="text-sm text-gray-400 block font-medium">{t('scanning_completed')}</span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left: Visual Scanner */}
          <div className="lg:col-span-1 bg-white rounded-2xl border border-gray-200 p-8 flex flex-col items-center justify-center min-h-[400px]">
            <div className="relative w-64 h-64">
              <div className="absolute inset-0 rounded-full border-8 border-gray-50"></div>
              <motion.div 
                className="absolute inset-0 rounded-full border-8 border-blue-500 border-t-transparent border-l-transparent"
                animate={{ rotate: 360 }}
                transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
              />
              <div className="absolute inset-4 rounded-full bg-blue-50/50 flex flex-col items-center justify-center overflow-hidden backdrop-blur-sm">
                <Search className="w-16 h-16 text-blue-400 mb-2 opacity-50" />
                <span className="text-sm font-bold text-blue-600 tracking-widest uppercase">{t('scanning_processing')}</span>
              </div>
            </div>
            <div className="w-full mt-8 space-y-2">
              <div className="flex justify-between text-xs font-bold text-gray-400 uppercase tracking-wider">
                <span>{t('scanning_depth')}</span>
                <span>{t('scanning_deep')}</span>
              </div>
              <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                <motion.div 
                  className="h-full bg-blue-500"
                  initial={{ width: 0 }}
                  animate={{ width: `${progress}%` }}
                />
              </div>
            </div>
          </div>

          {/* Right: Stats & Logs */}
          <div className="lg:col-span-2 flex flex-col gap-6">
            {/* Stats Row */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <RiskCard 
                title={t('scanning_high_risk')} 
                count={risks.high} 
                color="#EF4444" 
                icon={AlertTriangle}
                bgClass="bg-red-50"
                textClass="text-red-500"
              />
              <RiskCard 
                title={t('scanning_med_high')} 
                count={risks.mediumHigh} 
                color="#F97316" 
                icon={AlertTriangle}
                bgClass="bg-orange-50"
                textClass="text-orange-500"
              />
              <RiskCard 
                title={t('scanning_medium')} 
                count={risks.medium} 
                color="#EAB308" 
                icon={Info}
                bgClass="bg-yellow-50"
                textClass="text-yellow-500"
              />
              <RiskCard 
                title={t('scanning_low_risk')} 
                count={risks.low} 
                color="#22C55E" 
                icon={CheckCircle}
                bgClass="bg-green-50"
                textClass="text-green-500"
              />
            </div>

            {/* Live Log */}
            <div className="flex-1 bg-gray-900 rounded-2xl p-6 font-mono text-sm overflow-hidden relative">
              <div className="absolute top-4 right-4 flex gap-2">
                <div className="w-3 h-3 rounded-full bg-red-500"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                <div className="w-3 h-3 rounded-full bg-green-500"></div>
              </div>
              <h3 className="text-gray-400 font-bold mb-4 uppercase tracking-wider text-xs">{t('scanning_log_title')}</h3>
              <div className="space-y-3">
                {logs.map((log, i) => (
                  <motion.div 
                    key={i}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="flex items-center gap-3 text-green-400"
                  >
                    <span className="text-gray-600">[{new Date().toLocaleTimeString()}]</span>
                    <span>{'>'} {log}</span>
                  </motion.div>
                ))}
                <motion.div 
                  animate={{ opacity: [0, 1, 0] }}
                  transition={{ duration: 1, repeat: Infinity }}
                  className="w-2 h-4 bg-green-500"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Error message */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="rounded-xl bg-red-50 border border-red-200 p-4 text-red-800"
          >
            <p className="font-medium">{quotaExhausted ? t('quota_exhausted_message') : t('scanning_error_title')}</p>
            <p className="text-sm mt-1">{error}</p>
            <div className="mt-3 flex gap-3">
              {quotaExhausted && (
                <Link to="/plans" className="text-sm font-medium text-blue-600 hover:underline">
                  {t('quota_exhausted_go')}
                </Link>
              )}
              <button
                onClick={() => navigate('/')}
                className="text-sm font-medium text-red-600 hover:underline"
              >
                {t('scanning_back_upload')}
              </button>
            </div>
          </motion.div>
        )}

        {/* Action Button */}
        {isComplete && analysisResult && !error && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex flex-col items-end gap-3"
          >
            {analysisResult.notice && (
              <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-4 py-2 w-full max-w-2xl">
                {analysisResult.notice}
              </p>
            )}
            <button
              disabled={reportLoading}
              onClick={async () => {
                if (!state?.file || !analysisResult) return;
                const fileName = state.file.name;
                if (!token) {
                  const reportState = { analysisResult, fileName };
                  try {
                    sessionStorage.setItem('report_page_state', JSON.stringify(reportState));
                  } catch {
                    /* ignore */
                  }
                  if (analysisResult.reportId != null) {
                    navigate(`/report/${analysisResult.reportId}`, { state: reportState });
                  } else {
                    navigate('/report', { state: reportState });
                  }
                  return;
                }
                setReportLoading(true);
                let onlyOfficeConfig: OnlyOfficePreviewConfig | undefined;
                let minioKey: string | undefined;
                const isDocx = /\.(docx?|doc)$/i.test(state.file.name);
                try {
                  if (isDocx) {
                    const res = await annotateDocument(state.file, analysisResult, token);
                    onlyOfficeConfig = res;
                    minioKey = res.minioKey;
                  } else {
                    const res = await uploadPreviewDocument(state.file, token);
                    onlyOfficeConfig = res;
                    minioKey = res.minioKey;
                  }
                } catch {
                  if (isDocx) {
                    try {
                      const res = await uploadPreviewDocument(state.file, token);
                      onlyOfficeConfig = res;
                      minioKey = res.minioKey;
                    } catch {
                      /* no preview */
                    }
                  }
                } finally {
                  setReportLoading(false);
                }
                if (analysisResult.reportId != null && minioKey) {
                  try {
                    await linkReportDocument(token, analysisResult.reportId, minioKey);
                  } catch {
                    /* link failed; still navigate with local config */
                  }
                }
                if (analysisResult.reportId != null) {
                  navigate(`/report/${analysisResult.reportId}`, {
                    state: { analysisResult, fileName, onlyOfficeConfig },
                  });
                } else {
                  const reportState = { analysisResult, fileName, onlyOfficeConfig };
                  try {
                    sessionStorage.setItem('report_page_state', JSON.stringify(reportState));
                  } catch {
                    /* ignore */
                  }
                  navigate('/report', { state: reportState });
                }
              }}
              className="bg-blue-600 hover:bg-blue-700 disabled:opacity-70 text-white font-bold py-4 px-8 rounded-xl shadow-lg hover:shadow-xl flex items-center gap-3 transform hover:-translate-y-0.5 transition-all"
            >
              <FileText className="w-5 h-5" />
              {reportLoading ? t('report_loading_doc') : t('scanning_view_report')}
              <ArrowRight className="w-5 h-5" />
            </button>
          </motion.div>
        )}
      </div>
    </div>
  );
}
