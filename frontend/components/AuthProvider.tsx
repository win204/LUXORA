"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  clearStoredSession,
  getCurrentUser,
  logoutSession,
  refreshAndStoreSession,
  storeSession,
  type AuthSession
} from "@/lib/authClient";
import type { AuthResponse, CurrentUser } from "@/lib/types";

type AuthContextValue = {
  user: CurrentUser | null;
  accessToken: string | null;
  loading: boolean;
  sessionExpired: boolean;
  setAuthenticated: (response: AuthResponse) => void;
  updateUser: (user: CurrentUser) => void;
  clearLocalSession: () => void;
  refreshAuth: () => Promise<AuthSession | null>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);

  const clearLocalSession = useCallback(() => {
    clearStoredSession();
    setSession(null);
    setSessionExpired(false);
  }, []);

  const refreshAuth = useCallback(async () => {
    try {
      const refreshed = await refreshAndStoreSession();
      setSession(refreshed);
      setSessionExpired(false);
      return refreshed;
    } catch {
      clearStoredSession();
      setSession(null);
      return null;
    }
  }, []);

  useEffect(() => {
    let active = true;

    async function restoreSession() {
      try {
        const restored = await refreshAndStoreSession();
        if (!active) {
          return;
        }
        try {
          const user = await getCurrentUser(restored.accessToken);
          if (active) {
            setSession({ ...restored, user });
            setSessionExpired(false);
          }
        } catch {
          if (active) {
            clearStoredSession();
            setSession(null);
            setSessionExpired(true);
          }
        }
      } catch {
        if (active) {
          clearStoredSession();
          setSession(null);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void restoreSession();
    return () => {
      active = false;
    };
  }, []);

  const setAuthenticated = useCallback((response: AuthResponse) => {
    setSession(storeSession(response));
    setSessionExpired(false);
  }, []);

  const updateUser = useCallback((user: CurrentUser) => {
    setSession((current) => current ? { ...current, user } : current);
    window.dispatchEvent(new Event("luxora-auth-updated"));
  }, []);

  const logout = useCallback(async () => {
    await logoutSession(session?.accessToken ?? null);
    setSession(null);
    setSessionExpired(false);
  }, [session?.accessToken]);

  const value = useMemo<AuthContextValue>(() => ({
    user: session?.user ?? null,
    accessToken: session?.accessToken ?? null,
    loading,
    sessionExpired,
    setAuthenticated,
    updateUser,
    clearLocalSession,
    refreshAuth,
    logout
  }), [clearLocalSession, loading, logout, refreshAuth, session, sessionExpired, setAuthenticated, updateUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
