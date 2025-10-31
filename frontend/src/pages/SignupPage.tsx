import { useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";

export default function SignupPage() {
    const navigate = useNavigate();

    // 단계: 1️⃣ 이메일 인증 → 2️⃣ 코드 입력 및 회원정보 작성
    const [step, setStep] = useState<1 | 2>(1);

    // 폼 데이터
    const [email, setEmail] = useState("");
    const [code, setCode] = useState("");
    const [password, setPassword] = useState("");
    const [nickname, setNickname] = useState("");
    const [gender, setGender] = useState("");
    const [birthDate, setBirthDate] = useState("");

    /** ✅ 1단계: 인증 코드 전송 */
    const handleSendCode = async () => {
        if (!email) return toast.error("이메일을 입력해주세요.");
        try {
            await api.post("/api/auth/send-code", { email });
            toast.success("✅ 인증 코드가 이메일로 전송되었습니다!");
            setStep(2);
        } catch (err: any) {
            toast.error("❌ " + (err.response?.data || "이메일 전송 실패"));
        }
    };

    /** ✅ 2단계: 회원가입 완료 */
    const handleSignup = async () => {
        if (!code || !password || !nickname) {
            toast.error("모든 필드를 입력해주세요.");
            return;
        }

        try {
            const res = await api.post("/api/auth/signup", {
                email,
                password,
                nickname,
                gender,
                birthDate,
                code,
            });

            toast.success(res.data || "🎉 회원가입 완료!");
            setTimeout(() => navigate("/login"), 1500);
        } catch (err: any) {
            toast.error("❌ " + (err.response?.data || "회원가입 실패"));
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-b from-blue-50 to-blue-100 dark:from-gray-900 dark:to-gray-950 transition-colors duration-300">
            <div className="bg-white dark:bg-gray-800 p-10 rounded-2xl shadow-xl w-[400px] transition-colors duration-300">
                <h2 className="text-2xl font-bold text-center text-blue-600 dark:text-blue-400 mb-6">
                    🧠 HealthChat+ 회원가입
                </h2>

                {step === 1 ? (
                    <>
                        <p className="text-gray-600 dark:text-gray-300 text-sm mb-4 text-center">
                            가입할 이메일을 입력하면 인증코드를 보내드려요 📧
                        </p>

                        <input
                            type="email"
                            placeholder="이메일 주소"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full mb-3 px-4 py-2 border rounded-lg
                                       focus:outline-none focus:ring-2 focus:ring-blue-400
                                       dark:bg-gray-700 dark:border-gray-600 dark:text-white
                                       dark:placeholder-gray-400 transition-colors"
                        />

                        <button
                            onClick={handleSendCode}
                            className="w-full bg-blue-600 text-white py-2 rounded-lg font-semibold
                                       hover:bg-blue-700 transition dark:bg-blue-500 dark:hover:bg-blue-600"
                        >
                            인증 코드 보내기
                        </button>
                    </>
                ) : (
                    <>
                        <p className="text-gray-600 dark:text-gray-300 text-sm mb-4 text-center">
                            이메일로 받은 인증코드를 입력하고 회원정보를 작성해주세요 ✨
                        </p>

                        <input
                            type="text"
                            placeholder="인증 코드"
                            value={code}
                            onChange={(e) => setCode(e.target.value)}
                            className="w-full mb-3 px-4 py-2 border rounded-lg dark:bg-gray-700 dark:text-white dark:border-gray-600 transition-colors"
                        />

                        <input
                            type="password"
                            placeholder="비밀번호"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full mb-3 px-4 py-2 border rounded-lg dark:bg-gray-700 dark:text-white dark:border-gray-600 transition-colors"
                        />

                        <input
                            type="text"
                            placeholder="닉네임"
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            className="w-full mb-3 px-4 py-2 border rounded-lg dark:bg-gray-700 dark:text-white dark:border-gray-600 transition-colors"
                        />

                        <select
                            value={gender}
                            onChange={(e) => setGender(e.target.value)}
                            className="w-full mb-3 px-4 py-2 border rounded-lg dark:bg-gray-700 dark:text-white dark:border-gray-600 transition-colors"
                        >
                            <option value="">성별 선택</option>
                            <option value="남">남성</option>
                            <option value="여">여성</option>
                        </select>

                        <input
                            type="date"
                            value={birthDate}
                            onChange={(e) => setBirthDate(e.target.value)}
                            className="w-full mb-4 px-4 py-2 border rounded-lg dark:bg-gray-700 dark:text-white dark:border-gray-600 transition-colors"
                        />

                        <button
                            onClick={handleSignup}
                            className="w-full bg-green-600 text-white py-2 rounded-lg font-semibold
                                       hover:bg-green-700 transition dark:bg-green-500 dark:hover:bg-green-600"
                        >
                            회원가입 완료
                        </button>
                    </>
                )}

                <p className="text-center text-gray-500 dark:text-gray-400 mt-4 text-sm">
                    이미 계정이 있으신가요?{" "}
                    <button
                        onClick={() => navigate("/login")}
                        className="text-blue-500 hover:underline dark:text-blue-400"
                    >
                        로그인
                    </button>
                </p>
            </div>

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
        </div>
    );
}
