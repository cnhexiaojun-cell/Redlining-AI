import React, { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useLanguage } from "../contexts/LanguageContext";
import { useAuth } from "../contexts/AuthContext";
import * as authApi from "../api/auth";

export default function LoginPage() {
  const { t } = useLanguage();
  const { login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [captchaId, setCaptchaId] = useState("");
  const [captchaImage, setCaptchaImage] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadCaptcha = useCallback(async () => {
    try {
      const data = await authApi.getCaptcha();
      setCaptchaId(data.captchaId);
      setCaptchaImage(data.image);
      setCaptchaCode("");
    } catch {
      setError(t("auth_error_captcha"));
    }
  }, [t]);

  useEffect(() => {
    loadCaptcha();
  }, [loadCaptcha]);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await login(username, password, captchaId, captchaCode);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "";
      if (msg.includes("Invalid or expired captcha") || msg.includes("captcha")) {
        setError(t("auth_error_captcha"));
        loadCaptcha();
      } else if (msg.includes("Account is disabled") || msg.includes("disabled")) {
        setError(t("auth_error_account_disabled"));
        loadCaptcha();
      } else if (
        msg.includes("Invalid username or password") ||
        msg.includes("Invalid")
      ) {
        setError(t("auth_error_invalid_credentials"));
        loadCaptcha();
      } else {
        setError(msg || t("auth_login"));
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-[60vh] flex items-center justify-center px-4 pt-20 pb-8">
      <div className="w-full max-w-md bg-white rounded-2xl border border-gray-200 shadow-sm p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">{t("auth_login_title")}</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {t("auth_username")}
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              required
              autoComplete="username"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("auth_password")}</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              required
              autoComplete="current-password"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">{t("auth_captcha")}</label>
            <div className="flex gap-2 items-center">
              {captchaImage && (
                <img
                  src={captchaImage}
                  alt="captcha"
                  className="h-10 border border-gray-200 rounded-lg flex-shrink-0"
                />
              )}
              <button
                type="button"
                onClick={loadCaptcha}
                className="text-sm text-blue-600 hover:underline whitespace-nowrap"
              >
                {t("auth_refresh_captcha")}
              </button>
            </div>
            <input
              type="text"
              value={captchaCode}
              onChange={(e) => setCaptchaCode(e.target.value)}
              placeholder={t("auth_captcha_placeholder")}
              className="w-full mt-2 px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              required
              autoComplete="off"
            />
          </div>
          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
              {error}
            </p>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold py-3 rounded-xl transition-colors"
          >
            {submitting ? t("auth_submitting") : t("auth_login")}
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-gray-500">
          {t("auth_no_account")}{" "}
          <Link to="/register" className="text-blue-600 font-medium hover:underline">
            {t("auth_register")}
          </Link>
        </p>
      </div>
    </div>
  );
}
