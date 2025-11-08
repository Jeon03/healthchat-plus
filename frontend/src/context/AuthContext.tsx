import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { toast } from "react-toastify";
import api from "../api/axios";


interface User {
    email: string;
    nickname: string;
}

interface AuthContextType {
    user: User | null;
    loading: boolean;
    refreshUser: () => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    const refreshUser = async () => {
        try {
            const res = await api.get("/user/me", { validateStatus: () => true });

            // ✅ 스프링이 HTML (로그인 폼 리디렉션) 반환하는 경우 방지
            const contentType = res.headers["content-type"];
            if (contentType && contentType.includes("text/html")) {
                console.warn("HTML 응답 감지 → 비로그인 처리");
                throw new Error("HTML response");
            }

            // ✅ 401/403 → 비로그인 처리
            if (res.status >= 400) {
                console.warn(`🔒 인증 실패 (${res.status})`);
                throw new Error("Unauthorized");
            }

            setUser(res.data);
        } catch {
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    /** ✅ 로그아웃 */
    const logout = async () => {
        try {
            await api.post("/auth/logout");
        } catch {
            // 서버 응답이 없어도 로컬 상태 정리
        } finally {
            setUser(null);
            localStorage.clear();
            sessionStorage.clear();
            console.log("로그아웃 완료");
            window.dispatchEvent(new Event("auth-logout"));
            toast.info("로그아웃 되었습니다.");
            window.location.href = `${window.location.origin}/login`;
        }
    };

    /** ✅ 앱 시작 시 1회 실행 */
    useEffect(() => {
        refreshUser();

        // ✅ 401 감지 시 전역 로그아웃
        const listener = () => {
            console.log("⚠️ 401 또는 수동 로그아웃 감지 — 상태 초기화");
            setUser(null);
        };

        window.addEventListener("auth-logout", listener);
        return () => window.removeEventListener("auth-logout", listener);
    }, []);

    return (
        <AuthContext.Provider value={{ user, loading, refreshUser, logout }}>
            {loading ? (
                <div className="flex justify-center items-center h-screen text-gray-500">
                    🔄 사용자 정보를 확인 중입니다...
                </div>
            ) : (
                children
            )}
        </AuthContext.Provider>
    );
}

/** ✅ Context 쉽게 불러오는 훅 */
export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth는 AuthProvider 내부에서만 사용해야 합니다.");
    return ctx;
}
