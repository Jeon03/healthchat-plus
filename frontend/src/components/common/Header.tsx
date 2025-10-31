import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import { Moon, Sun } from "lucide-react";

export default function Header() {
    const { user, logout } = useAuth();
    const { theme, toggleTheme } = useTheme();

    return (
        <header className="fixed top-0 w-full bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 shadow-sm flex justify-between items-center px-6 py-3 transition-colors duration-300 z-50">
            <h1 className="font-bold text-blue-600 dark:text-blue-400 text-xl">
                🧠 HealthChat+
            </h1>

            <div className="flex items-center gap-4">
                {/* ✅ 테마 토글 */}
                <button
                    onClick={toggleTheme}
                    className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                    aria-label="toggle theme"
                >
                    {theme === "light" ? (
                        <Moon className="w-5 h-5 text-gray-700" />
                    ) : (
                        <Sun className="w-5 h-5 text-yellow-300" />
                    )}
                </button>

                {/* ✅ 로그인 상태별 표시 */}
                {user ? (
                    <div className="flex items-center gap-3">
                        <span className="text-gray-800 dark:text-gray-200 font-medium">
                            {user.nickname}님
                        </span>
                        <button
                            onClick={logout}
                            className="px-3 py-1 bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-100 rounded hover:bg-gray-300 dark:hover:bg-gray-600 transition"
                        >
                            로그아웃
                        </button>
                    </div>
                ) : (
                    <Link
                        to="/login"
                        className="text-blue-500 dark:text-blue-300 hover:underline"
                    >
                        로그인
                    </Link>
                )}
            </div>
        </header>
    );
}
