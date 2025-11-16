import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import { Moon, Sun } from "lucide-react";
import logo from "../../assets/logo.png";

export default function Header() {
    const { user, logout } = useAuth();
    const { theme, toggleTheme } = useTheme();
    const location = useLocation();

    const isLoggedIn = !!user;
    const isLoginPage = location.pathname === "/login" || location.pathname === "/signup";

    // 🔥 로그인 페이지에서는 헤더 완전 숨김
    if (!isLoggedIn && isLoginPage) {
        return null;
    }

    return (
        <header
            className="
            fixed top- w-full z-50
            flex items-center justify-center
            backdrop-blur-md
            bg-white/70 dark:bg-gray-900/40
            border-b border-gray-200/40 dark:border-gray-700/30
            px-6 py-4
            transition-all
        "
        >
            {/* ------------ 중앙 로고 ------------ */}
            <Link
                to="/"
                className="
                    absolute left-1/2 -translate-x-1/2
                    flex items-center
                "
            >
                <img
                    src={logo}
                    alt="HealthChat+"
                    className="
        h-12        /* 높이 증가 */
        w-auto
        object-contain
        scale-[3]     /* 스케일 업 */
        md:scale-[3]  /* 큰 화면에서 더 크게 */
        drop-shadow-sm
        transition-transform
    "
                />
            </Link>

            {/* ------------ 로그인된 경우에만 오른쪽 컨트롤 표시 ------------ */}
            {isLoggedIn && (
                <div className="ml-auto flex items-center gap-3">

                    {/* 🌙 테마 토글 */}
                    <button
                        onClick={toggleTheme}
                        className="
                            p-2 rounded-full
                            bg-gray-200/60 dark:bg-gray-800/60
                            hover:bg-gray-300 dark:hover:bg-gray-700
                            transition
                        "
                    >
                        {theme === "light" ? (
                            <Moon className="w-5 h-5 text-gray-700" />
                        ) : (
                            <Sun className="w-5 h-5 text-yellow-300" />
                        )}
                    </button>

                    {/* 🚪 로그아웃 */}
                    <button
                        onClick={logout}
                        className="
                            px-3 py-1.5 rounded-lg
                            bg-gray-300 dark:bg-gray-700
                            hover:bg-gray-400 dark:hover:bg-gray-600
                            text-gray-800 dark:text-gray-200
                            transition
                        "
                    >
                        로그아웃
                    </button>
                </div>
            )}
        </header>
    );
}
