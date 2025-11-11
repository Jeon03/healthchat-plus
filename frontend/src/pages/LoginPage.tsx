import {useCallback, useState} from "react";
import {toast, ToastContainer} from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import api from "../api/axios";
import {useAuth} from "../context/AuthContext";
import {useNavigate} from "react-router-dom";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const { refreshUser } = useAuth();
    const navigate = useNavigate();

    /** ✅ 로그인 */
    const handleLogin = async () => {
        try {
            const res = await api.post("/auth/login", { email, password });

            toast.success(
                <div className="leading-relaxed text-[15px] font-medium">
                    로그인 성공! <br />
                    <span className="text-blue-600">{res.data.nickname}</span> 님, 환영합니다 🎉
                </div>
            );

            await refreshUser();
            setTimeout(() => navigate("/"), 1500);
        } catch (err: any) {
            const msg = err.response?.data || "로그인 실패";

            toast.error(
                <div
                    dangerouslySetInnerHTML={{ __html: msg }}
                    className="leading-relaxed text-[15px]"
                />
            );
        }
    };

    /** ✅ 소셜 로그인 */
    const goOAuth = useCallback((provider: "google" | "naver" | "kakao") => {
        const backend = import.meta.env.VITE_API_URL || "http://localhost:8081";
        window.location.href = `${backend}/oauth2/authorization/${provider}`;
    }, []);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-b from-blue-50 to-blue-100 dark:from-gray-900 dark:to-gray-950 transition-colors duration-300">
            <div className="bg-white dark:bg-gray-800 p-10 rounded-2xl shadow-xl w-[400px] transition-colors duration-300">
                <h2 className="text-2xl font-bold text-center text-blue-600 dark:text-blue-400 mb-6">
                    🧠 HealthChat+ 로그인
                </h2>

                {/* 이메일 입력 */}
                <input
                    type="email"
                    placeholder="이메일"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full mb-3 px-4 py-2 border rounded-lg
                               focus:outline-none focus:ring-2 focus:ring-blue-400
                               dark:bg-gray-700 dark:border-gray-600 dark:text-white
                               dark:placeholder-gray-400 transition-colors"
                />

                {/* 비밀번호 입력 */}
                <input
                    type="password"
                    placeholder="비밀번호"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full mb-4 px-4 py-2 border rounded-lg
                               focus:outline-none focus:ring-2 focus:ring-blue-400
                               dark:bg-gray-700 dark:border-gray-600 dark:text-white
                               dark:placeholder-gray-400 transition-colors"
                />

                {/* 로그인 버튼 */}
                <button
                    onClick={handleLogin}
                    className="w-full bg-blue-600 text-white font-semibold py-2 rounded-lg
                               hover:bg-blue-700 transition dark:bg-blue-500 dark:hover:bg-blue-600"
                >
                    로그인
                </button>

                {/* 회원가입 이동 링크 */}
                <p className="text-center text-gray-500 dark:text-gray-400 mt-4 text-sm">
                    계정이 없으신가요?{" "}
                    <button
                        onClick={() => navigate("/signup")}
                        className="text-blue-500 hover:underline dark:text-blue-400"
                    >
                        회원가입
                    </button>
                </p>

                <hr className="my-5 border-gray-300 dark:border-gray-600" />

                {/* ✅ 소셜 로그인 */}
                <div className="flex flex-col gap-2">
                    <button
                        onClick={() => goOAuth("google")}
                        className="bg-[#DB4437] text-white py-2 rounded-lg hover:bg-[#c23321] transition"
                    >
                        Google 계정으로 로그인
                    </button>

                    <button
                        onClick={() => goOAuth("naver")}
                        className="bg-[#03C75A] text-white py-2 rounded-lg hover:bg-[#02b152] transition"
                    >
                        Naver 계정으로 로그인
                    </button>

                    <button
                        onClick={() => goOAuth("kakao")}
                        className="bg-[#FEE500] text-[#3C1E1E] py-2 rounded-lg hover:bg-[#fddb00] transition"
                    >
                        Kakao 계정으로 로그인
                    </button>
                </div>
            </div>

            {/* Toast 메시지 */}
            <ToastContainer
                position="top-center"
                autoClose={2500}
                hideProgressBar={false}
                closeOnClick
                pauseOnHover
                draggable
                theme="light"
                toastClassName="!w-[440px] !max-w-[90vw] dark:!bg-gray-800 dark:!text-white !rounded-xl !shadow-md"
            />
        </div>
    );
}
