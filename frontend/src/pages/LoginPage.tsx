import { useCallback, useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import api from "../api/axios";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

import googleLogin from "../assets/icons/googleLogin.png";
import naverLogin from "../assets/icons/naverLogin.png";
import kakaoLogin from "../assets/icons/kakaoLogin.png";
import logo from "../assets/logo.png";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const { refreshUser } = useAuth();
    const navigate = useNavigate();

    /** 로그인 */
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

    /** 소셜 로그인 이동 */
    const goOAuth = useCallback((provider: "google" | "naver" | "kakao") => {
        const backend = import.meta.env.VITE_API_URL || "http://localhost:8081";
        window.location.href = `${backend}/oauth2/authorization/${provider}`;
    }, []);

    return (
        <div className="min-h-screen w-full flex flex-col items-center justify-center px-4 -mt-20">

            {/* 💡 히어로 섹션 */}
            <div className="text-center mb-10">
                <div className="flex justify-center mb-6">
                    <img
                        src={logo}
                        alt="HealthChat+ Logo"
                        className="
      w-[220px] h-[70px]
      object-cover
      scale-[1.2]     /* 확대 */
      mx-auto
      overflow-hidden
      select-none
  "
                    />
                </div>

                <p className="text-gray-600 dark:text-gray-300 text-lg leading-relaxed max-w-xl mx-auto">
                    여러분의 <span className="font-semibold text-blue-600 dark:text-blue-400">하루 식단 · 운동 · 감정</span>을
                    자연어로 기록하면,<br/>
                    <span className="font-semibold">AI 건강 코치가 분석·요약·피드백</span>까지 도와드려요.
                </p>

                <p className="mt-3 text-gray-500 dark:text-gray-400 text-sm">
                    더 건강한 하루를 만들기 위한 가장 똑똑한 시작
                </p>
            </div>

            {/* 로그인 카드 */}
            <div className="bg-white dark:bg-gray-800 p-10 rounded-2xl shadow-xl w-[400px] transition-colors duration-300">
                <h2 className="text-2xl font-bold text-center text-blue-600 dark:text-blue-400 mb-6">
                    로그인
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

                <div className="flex flex-col gap-2 mt-5">

                    {/* Google */}
                    <button
                        onClick={() => goOAuth("google")}
                        className="w-full"
                    >
                        <img
                            src={googleLogin}
                            alt="google-login"
                            className="
                w-full h-auto rounded-lg shadow-md cursor-pointer
                hover:brightness-105 hover:shadow-lg active:scale-95
                transition-all duration-200
            "
                        />
                    </button>

                    {/* Naver */}
                    <button
                        onClick={() => goOAuth("naver")}
                        className="w-full"
                    >
                        <img
                            src={naverLogin}
                            alt="naver-login"
                            className="
                w-full h-auto rounded-lg shadow-md cursor-pointer
                hover:brightness-105 hover:shadow-lg active:scale-95
                transition-all duration-200
            "
                        />
                    </button>

                    {/* Kakao */}
                    <button
                        onClick={() => goOAuth("kakao")}
                        className="w-full"
                    >
                        <img
                            src={kakaoLogin}
                            alt="kakao-login"
                            className="
                w-full h-auto rounded-lg shadow-md cursor-pointer
                hover:brightness-105 hover:shadow-lg active:scale-95
                transition-all duration-200
            "
                        />
                    </button>

                </div>


            </div>

            {/* Toast */}
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
