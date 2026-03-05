import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import * as authApi from "../api/auth";
import type { PermissionsResponse } from "../api/auth";

const TOKEN_KEY = "redlining_token";

export interface AuthUser {
  id: number;
  username?: string | null;
  email?: string | null;
  avatarUrl?: string | null;
  realName?: string | null;
  occupation?: string | null;
  planCode?: string | null;
  planName?: string | null;
  planType?: string | null;
  quotaRemaining?: number | null;
  quotaTotal?: number | null;
  periodEndsAt?: string | null;
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  loading: boolean;
  permissions: PermissionsResponse | null;
  refreshUser: () => Promise<void>;
  hasMenuPermission: (code: string) => boolean;
  hasButtonPermission: (code: string) => boolean;
  isSuperAdmin: () => boolean;
  login: (
    username: string,
    password: string,
    captchaId: string,
    captchaCode: string
  ) => Promise<void>;
  register: (
    username: string,
    password: string,
    captchaId: string,
    captchaCode: string,
    email?: string
  ) => Promise<void>;
  logout: () => void;
  setToken: (t: string | null) => void;
}

const AuthContext = React.createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setTokenState] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [permissions, setPermissions] = useState<PermissionsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const hasMenuPermission = useCallback((code: string) => {
    if (!permissions) return false;
    if (permissions.superAdmin) return true;
    return (permissions.menuCodes ?? []).includes(code);
  }, [permissions]);

  const hasButtonPermission = useCallback((code: string) => {
    if (!permissions) return false;
    if (permissions.superAdmin) return true;
    return (permissions.buttonCodes ?? []).includes(code);
  }, [permissions]);

  const isSuperAdmin = useCallback(() => permissions?.superAdmin === true, [permissions]);

  const setToken = useCallback((t: string | null) => {
    if (t) {
      localStorage.setItem(TOKEN_KEY, t);
      setTokenState(t);
    } else {
      localStorage.removeItem(TOKEN_KEY);
      setTokenState(null);
      setUser(null);
    }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    navigate("/login", { replace: true });
  }, [navigate, setToken]);

  const refreshUser = useCallback(async () => {
    if (!token) return;
    const [u, p] = await Promise.all([authApi.getMe(token), authApi.getPermissions(token)]);
    setUser(u);
    setPermissions(p);
  }, [token]);

  const login = useCallback(
    async (
      username: string,
      password: string,
      captchaId: string,
      captchaCode: string
    ) => {
      const res = await authApi.login(
        username,
        password,
        captchaId,
        captchaCode
      );
      setToken(res.access_token);
      setUser(res.user);
      navigate("/", { replace: true });
    },
    [navigate, setToken]
  );

  const register = useCallback(
    async (
      username: string,
      password: string,
      captchaId: string,
      captchaCode: string,
      email?: string
    ) => {
      const res = await authApi.register(
        username,
        password,
        captchaId,
        captchaCode,
        email
      );
      setToken(res.access_token);
      setUser(res.user);
      navigate("/", { replace: true });
    },
    [navigate, setToken]
  );

  useEffect(() => {
    if (!token) {
      setUser(null);
      setPermissions(null);
      setLoading(false);
      return;
    }
    Promise.all([authApi.getMe(token), authApi.getPermissions(token)])
      .then(([u, p]) => {
        setUser(u);
        setPermissions(p);
      })
      .catch(() => setToken(null))
      .finally(() => setLoading(false));
  }, [token, setToken]);

  const value: AuthContextValue = {
    user,
    token,
    loading,
    permissions,
    refreshUser,
    hasMenuPermission,
    hasButtonPermission,
    isSuperAdmin,
    login,
    register,
    logout,
    setToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = React.useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
