import { Routes, Route, Navigate, useLocation } from "react-router-dom";
import Layout from "./components/common/Layout";
import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";
import SignupPage from "./pages/SignupPage";
import { useAuth } from "./context/AuthContext";
import {useEffect, useRef} from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import api from "./api/axios";

export default function App() {
    const { user, loading, refreshUser } = useAuth();
    const location = useLocation();
    const hasShownToast = useRef(false);

    useEffect(() => {
        if (hasShownToast.current) return;
        const params = new URLSearchParams(location.search);

        /** ✅ 1. 기존 계정과 소셜 계정이 자동 병합된 경우 */
        if (params.get("merged") === "true") {
            toast.info("기존 이메일 계정과 소셜 계정이 자동 연동되었습니다!");
            params.delete("merged");
            window.history.replaceState({}, "", location.pathname);
        }

        /** ✅ 2. 소셜 로그인 성공 */
        else if (params.get("login") === "success") {
            toast.success("소셜 로그인 성공!");
            params.delete("login");
            window.history.replaceState({}, "", location.pathname);
        }

        /** ✅ 3. 병합 필요 (백엔드에서 redirect된 경우) */
        else if (params.get("mergeCandidate") && params.get("provider")) {
            const email = params.get("mergeCandidate")!;
            const provider = params.get("provider")!;

            toast.warn(
                `⚠️ ${provider.toUpperCase()} 계정의 이메일(${email})이 기존 로컬 계정과 동일합니다.`,
                { autoClose: 4000 }
            );
            hasShownToast.current = true;
            setTimeout(() => {
                const confirmMerge = window.confirm(
                    `${provider.toUpperCase()} 계정을 기존 이메일(${email})과 병합하시겠습니까?`
                );
                if (confirmMerge) {
                    handleMerge(email, provider);
                } else {
                    toast.info("병합이 취소되었습니다.");
                    setTimeout(() => (window.location.href = "/"), 1200);
                }
            }, 1000);

            // URL 정리
            params.delete("mergeCandidate");
            params.delete("provider");
            window.history.replaceState({}, "", location.pathname);
        }
    }, [location]);

    /** ✅ 병합 처리 요청 */
    const handleMerge = async (email: string, provider: string) => {
        try {
            const res = await api.post("/auth/merge-account", { email, provider });
            toast.success(res.data || "계정 병합이 완료되었습니다!");
            await refreshUser();
            setTimeout(() => (window.location.href = "/"), 1500);
        } catch (err: any) {
            toast.error(err.response?.data || "병합에 실패했습니다.");
            setTimeout(() => (window.location.href = "/login"), 2000);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen text-gray-500">
                🔄 사용자 정보를 확인 중입니다...
            </div>
        );
    }

    return (
        <>
            <Layout>
                <Routes>
                    {/* ✅ 로그인 필요 페이지 */}
                    <Route path="/" element={<Dashboard />} />

                    {/* ✅ 인증 불필요 페이지 */}
                    <Route
                        path="/login"
                        element={user ? <Navigate to="/" replace /> : <LoginPage />}
                    />
                    <Route
                        path="/signup"
                        element={user ? <Navigate to="/" replace /> : <SignupPage />}
                    />

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </Layout>

            <ToastContainer
                position="top-center"
                autoClose={2500}
                hideProgressBar={false}
                closeOnClick
                pauseOnHover
                draggable
                theme="light"
                toastClassName="dark:!bg-gray-800 dark:!text-white"
            />
        </>
    );
}
