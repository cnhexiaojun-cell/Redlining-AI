import React, { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useLanguage } from "../contexts/LanguageContext";
import { useAuth } from "../contexts/AuthContext";
import * as authApi from "../api/auth";

const AVATAR_MAX_BYTES = 5 * 1024 * 1024;
const AVATAR_ACCEPT = "image/jpeg,image/png";

function cropImageToSquare(file: File): Promise<Blob> {
  return new Promise((resolve) => {
    const img = new Image();
    const url = URL.createObjectURL(file);
    img.onload = () => {
      try {
        const size = Math.min(img.width, img.height);
        const canvas = document.createElement("canvas");
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          URL.revokeObjectURL(url);
          resolve(file);
          return;
        }
        const sx = (img.width - size) / 2;
        const sy = (img.height - size) / 2;
        ctx.drawImage(img, sx, sy, size, size, 0, 0, size, size);
        canvas.toBlob(
          (blob) => {
            URL.revokeObjectURL(url);
            resolve(blob || file);
          },
          file.type.startsWith("image/png") ? "image/png" : "image/jpeg",
          0.9
        );
      } catch (e) {
        URL.revokeObjectURL(url);
        resolve(file);
      }
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      resolve(file);
    };
    img.src = url;
  });
}

export default function ProfilePage() {
  const { t } = useLanguage();
  const { user, token, refreshUser } = useAuth();
  const [profile, setProfile] = useState<typeof user>(user);
  const [loading, setLoading] = useState(true);
  const [editOpen, setEditOpen] = useState(false);
  const [realName, setRealName] = useState("");
  const [occupation, setOccupation] = useState("");
  const [email, setEmail] = useState("");
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState("");
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const loadProfile = useCallback(async () => {
    if (!token) return;
    try {
      const data = await authApi.getMe(token);
      setProfile(data);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  useEffect(() => {
    if (user && !profile) setProfile(user);
  }, [user, profile]);

  const openEdit = () => {
    setRealName(profile?.realName ?? "");
    setOccupation(profile?.occupation ?? "");
    setEmail(profile?.email ?? "");
    setAvatarFile(null);
    setAvatarPreview(null);
    setSubmitError("");
    setSubmitSuccess(false);
    setEditOpen(true);
  };

  const onAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    if (!AVATAR_ACCEPT.split(",").some((t) => f.type === t.trim())) {
      setSubmitError(t("profile_avatar_hint"));
      return;
    }
    if (f.size > AVATAR_MAX_BYTES) {
      setSubmitError(t("profile_avatar_hint"));
      return;
    }
    setSubmitError("");
    setAvatarFile(f);
    setAvatarPreview(URL.createObjectURL(f));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setSubmitError("");
    setSubmitting(true);
    try {
      if (avatarFile) {
        const blob = await cropImageToSquare(avatarFile);
        const file = blob instanceof Blob ? new File([blob], avatarFile.name, { type: blob.type }) : avatarFile;
        await authApi.uploadAvatar(token, file);
      }
      await authApi.updateProfile(token, {
        realName: realName.trim() || undefined,
        occupation: occupation.trim() || undefined,
        email: email.trim() || undefined,
      });
      await refreshUser();
      setProfile(await authApi.getMe(token));
      setSubmitSuccess(true);
      setTimeout(() => {
        setEditOpen(false);
        setSubmitSuccess(false);
      }, 800);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "";
      setSubmitError(msg.includes("该邮箱已被使用") || msg.includes("already") ? t("auth_error_email_taken_short") : msg || t("profile_edit"));
    } finally {
      setSubmitting(false);
    }
  };

  const displayName = profile?.realName || profile?.username || profile?.email || "—";

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-[60vh] px-4 py-8 max-w-2xl mx-auto">
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">{t("profile_title")}</h1>
        <div className="flex flex-col sm:flex-row gap-6 items-start">
          <div className="flex-shrink-0">
            {profile?.avatarUrl ? (
              <img
                src={profile.avatarUrl}
                alt=""
                className="w-24 h-24 rounded-full object-cover border border-gray-200"
              />
            ) : (
              <div className="w-24 h-24 bg-blue-100 rounded-full flex items-center justify-center text-blue-700 font-bold text-2xl">
                {displayName.slice(0, 2).toUpperCase()}
              </div>
            )}
          </div>
          <div className="flex-1 space-y-2">
            <p><span className="text-gray-500">{t("profile_real_name")}:</span> {profile?.realName || "—"}</p>
            <p><span className="text-gray-500">{t("profile_occupation")}:</span> {profile?.occupation || "—"}</p>
            <p><span className="text-gray-500">{t("profile_contact")}:</span> {profile?.email || "—"}</p>
            <p><span className="text-gray-500">{t("profile_username")}:</span> {profile?.username || "—"}</p>
            {profile?.planName != null && (
              <p>
                <span className="text-gray-500">{t("plan_quota_label")}:</span>{' '}
                {profile.quotaTotal == null || profile.quotaTotal <= 0
                  ? t("plan_quota_unlimited")
                  : t("plan_quota_remaining")
                      .replace("{remaining}", String(profile.quotaRemaining ?? 0))
                      .replace("{total}", String(profile.quotaTotal))}
                {' · '}
                <Link to="/plans" className="text-blue-600 hover:underline">{t("plan_center")}</Link>
              </p>
            )}
          </div>
        </div>
        <button
          type="button"
          onClick={openEdit}
          className="mt-6 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl transition-colors"
        >
          {t("profile_edit")}
        </button>
      </div>

      {editOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setEditOpen(false)}>
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-lg font-semibold text-gray-900 mb-4">{t("profile_edit")}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t("profile_avatar_change")}</label>
                <div className="flex items-center gap-4">
                  {avatarPreview ? (
                    <img src={avatarPreview} alt="" className="w-16 h-16 rounded-full object-cover border" />
                  ) : profile?.avatarUrl ? (
                    <img src={profile.avatarUrl} alt="" className="w-16 h-16 rounded-full object-cover border" />
                  ) : (
                    <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center text-gray-400 text-sm">?</div>
                  )}
                  <input
                    type="file"
                    accept={AVATAR_ACCEPT}
                    onChange={onAvatarChange}
                    className="text-sm text-gray-600 file:mr-2 file:py-2 file:px-4 file:rounded-lg file:border file:border-gray-300 file:bg-gray-50 file:text-gray-700"
                  />
                </div>
                <p className="text-xs text-gray-500 mt-1">{t("profile_avatar_hint")}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t("profile_real_name")}</label>
                <input
                  type="text"
                  value={realName}
                  onChange={(e) => setRealName(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none"
                  maxLength={64}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t("profile_occupation")}</label>
                <input
                  type="text"
                  value={occupation}
                  onChange={(e) => setOccupation(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none"
                  maxLength={128}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t("profile_contact")}</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none"
                />
              </div>
              {submitError && (
                <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">{submitError}</p>
              )}
              {submitSuccess && (
                <p className="text-sm text-green-600 bg-green-50 rounded-lg px-3 py-2">{t("profile_update_success")}</p>
              )}
              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setEditOpen(false)}
                  className="px-4 py-2 border border-gray-300 rounded-xl text-gray-700 hover:bg-gray-50"
                >
                  {t("profile_cancel")}
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-medium rounded-xl"
                >
                  {submitting ? t("auth_submitting") : t("profile_save")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <p className="mt-4 text-center text-sm text-gray-500">
        <Link to="/" className="text-blue-600 hover:underline">{t("nav_upload")}</Link>
      </p>
    </div>
  );
}
