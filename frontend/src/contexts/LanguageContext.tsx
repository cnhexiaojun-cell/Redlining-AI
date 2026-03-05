import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { Locale } from '../i18n/translations';
import { translations } from '../i18n/translations';

const STORAGE_KEY = 'redlining-lang';

function getInitialLocale(): Locale {
  if (typeof window === 'undefined') return 'zh';
  const stored = localStorage.getItem(STORAGE_KEY) as Locale | null;
  return stored === 'zh' || stored === 'en' ? stored : 'zh';
}

interface LanguageContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string) => string;
}

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(getInitialLocale);

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    localStorage.setItem(STORAGE_KEY, next);
  }, []);

  const t = useCallback(
    (key: string) => {
      return translations[locale][key] ?? key;
    },
    [locale]
  );

  const value = useMemo(
    () => ({ locale, setLocale, t }),
    [locale, setLocale, t]
  );

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error('useLanguage must be used within LanguageProvider');
  return ctx;
}
