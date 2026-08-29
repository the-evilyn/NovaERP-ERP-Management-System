"use client";

import { useRouter } from "next/navigation";
import type React from "react";
import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { TOKEN_COOKIE_NAME, TOKEN_MAX_AGE_SECONDS } from "@/lib/axios";
import { deleteCookie, getCookie, setCookie } from "@/lib/cookies";
import {
  getMe,
  login as loginRequest,
  register as registerRequest,
} from "@/services/auth.service";
import type { LoginRequest, RegisterRequest, User } from "@/types/models";

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({
  children,
}: {
  children: React.ReactNode;
}): React.ReactElement {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const token = getCookie(TOKEN_COOKIE_NAME);
    if (!token) {
      setIsLoading(false);
      return;
    }

    getMe()
      .then(setUser)
      .catch(() => {
        deleteCookie(TOKEN_COOKIE_NAME);
      })
      .finally(() => setIsLoading(false));
  }, []);

  const login = useCallback(async (data: LoginRequest) => {
    const res = await loginRequest(data);
    setCookie(TOKEN_COOKIE_NAME, res.token, TOKEN_MAX_AGE_SECONDS);
    setUser({
      id: res.userId,
      fullName: res.fullName,
      email: res.email,
      role: res.role,
    });
  }, []);

  const register = useCallback(async (data: RegisterRequest) => {
    const res = await registerRequest(data);
    setCookie(TOKEN_COOKIE_NAME, res.token, TOKEN_MAX_AGE_SECONDS);
    setUser({
      id: res.userId,
      fullName: res.fullName,
      email: res.email,
      role: res.role,
    });
  }, []);

  const logout = useCallback(() => {
    deleteCookie(TOKEN_COOKIE_NAME);
    router.push("/login");
    // Deferred so the popup/menu that triggered logout finishes closing
    // before the sidebar (and the menu itself) unmounts.
    setTimeout(() => setUser(null), 0);
  }, [router]);

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
