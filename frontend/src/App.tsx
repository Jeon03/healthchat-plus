import { Routes, Route, Navigate } from "react-router-dom";
import Layout from "./components/common/Layout";
import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";
import SignupPage from "./pages/SignupPage";
import { useAuth } from "./context/AuthContext";

export default function App() {
    const { user, loading } = useAuth();

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen text-gray-500">
                🔄 사용자 정보를 확인 중입니다...
            </div>
        );
    }

    return (
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

                {/* ✅ 잘못된 경로 리디렉트 */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Layout>
    );
}
