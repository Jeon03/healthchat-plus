import { useEffect, useMemo, useRef, useState } from "react";
import { toast, ToastContainer } from "react-toastify";
import type { Id as ToastId } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";

function formatMMSS(total: number) {
    const m = Math.floor(total / 60)
        .toString()
        .padStart(2, "0");
    const s = Math.floor(total % 60)
        .toString()
        .padStart(2, "0");
    return `${m}:${s}`;
}

export default function SignupPage() {
    const navigate = useNavigate();

    const [step, setStep] = useState<1 | 2>(1);
    const [email, setEmail] = useState("");
    const [code, setCode] = useState("");
    const [password, setPassword] = useState("");
    const [nickname, setNickname] = useState("");
    const [gender, setGender] = useState("");
    const [birthDate, setBirthDate] = useState("");

    const [loading, setLoading] = useState(false);
    const [isCodeVerified, setIsCodeVerified] = useState(false);

    const DEFAULT_TTL = 300; // 5분
    const [secondsLeft, setSecondsLeft] = useState<number>(DEFAULT_TTL);
    const isExpired = secondsLeft <= 0;

    const verifyDebounceRef = useRef<number | null>(null);
    const toastRef = useRef<ToastId | null>(null); // ✅ 공통 토스트 ID (중복 방지)

    /** ✅ 토스트 표시 함수 (중복 방지) */
    const showToast = (msg: string, type: "success" | "error" | "info" | "loading" = "info") => {
        if (type === "loading") {
            if (toastRef.current && toast.isActive(toastRef.current)) {
                toast.update(toastRef.current, {
                    render: msg,
                    isLoading: true,
                    type: "info",
                    autoClose: false,
                });
            } else {
                toastRef.current = toast.loading(msg);
            }
            return;
        }

        if (toastRef.current && toast.isActive(toastRef.current)) {
            toast.update(toastRef.current, {
                render: msg,
                type,
                isLoading: false,
                autoClose: 2500,
            });
        } else {
            toastRef.current = toast(msg, {
                type,
                autoClose: 2500,
            });
        }
    };

    /** ✅ 타이머 시작 */
    const startTimer = (ttl?: number) => {
        setSecondsLeft(ttl && ttl > 0 ? ttl : DEFAULT_TTL);
    };

    /** ⏱️ 1초마다 감소 */
    useEffect(() => {
        if (step !== 2 || isCodeVerified) return;
        if (secondsLeft <= 0) return;
        const id = window.setInterval(() => {
            setSecondsLeft((sec) => Math.max(0, sec - 1));
        }, 1000);
        return () => clearInterval(id);
    }, [step, isCodeVerified, secondsLeft]);

    /** ✅ 1단계: 인증 코드 전송 */
    const handleSendCode = async () => {
        if (!email) return showToast("이메일을 입력해주세요.", "error");
        if (loading) return;

        setLoading(true);
        showToast("📨 이메일 전송 요청 중입니다...", "loading");

        try {
            const res = await api.post("/auth/send-code", { email });
            const expiresIn =
                (typeof res?.data === "object" && res.data?.expiresIn) || undefined;
            startTimer(expiresIn);
            setCode("");
            setIsCodeVerified(false);
            setStep(2);
            showToast("인증 코드 전송이 완료되었습니다!", "success");
        } catch (err: any) {
            showToast(err.response?.data || "이메일 전송 실패", "error");
        } finally {
            setLoading(false);
        }
    };

    /** 🔁 코드 재전송 */
    const handleResend = async () => {
        if (!email) return showToast("이메일을 입력해주세요.", "error");
        if (loading) return;

        setLoading(true);
        showToast("인증 코드 재전송 중...", "loading");

        try {
            const res = await api.post("/auth/send-code", { email });
            const expiresIn =
                (typeof res?.data === "object" && res.data?.expiresIn) || undefined;
            startTimer(expiresIn);
            setCode("");
            setIsCodeVerified(false);
            showToast("인증 코드가 재전송되었습니다!", "success");
        } catch (err: any) {
            showToast(err.response?.data || "재전송 실패", "error");
        } finally {
            setLoading(false);
        }
    };

    /** ✅ 코드 검증 */
    const verifyCodeRequest = async () => {
        if (!email) return showToast("이메일이 없습니다. 처음부터 진행해주세요.", "error");
        if (!code) return;
        if (isExpired) return showToast("인증 코드가 만료되었습니다. 재전송해주세요.", "error");

        try {
            await api.post("/auth/verify-code", { email, code });
            setIsCodeVerified(true);
            showToast("✅ 이메일 인증이 완료되었습니다!", "success");
        } catch {
            setIsCodeVerified(false);
            showToast("❌ 인증 코드가 유효하지 않습니다.", "error");
        }
    };

    /** ⌨️ 자동 검증 (6자리 입력 시) */
    useEffect(() => {
        if (verifyDebounceRef.current) {
            window.clearTimeout(verifyDebounceRef.current);
        }
        if (step === 2 && code && code.trim().length === 6 && !isExpired) {
            verifyDebounceRef.current = window.setTimeout(() => {
                verifyCodeRequest();
            }, 300) as unknown as number;
        }
        return () => {
            if (verifyDebounceRef.current) {
                window.clearTimeout(verifyDebounceRef.current);
                verifyDebounceRef.current = null;
            }
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [code, step, isExpired]);

    /** ✅ 회원가입 */
    const handleSignup = async () => {
        if (!isCodeVerified) return showToast("이메일 인증을 먼저 완료해주세요.", "error");
        if (!password || !nickname || !gender || !birthDate)
            return showToast("모든 필드를 입력해주세요.", "error");

        setLoading(true);
        showToast("⏳ 회원가입 진행 중...", "loading");

        try {
            await api.post("/auth/signup", {
                email,
                password,
                nickname,
                gender,
                birthDate,
                code, // 서버에서 최종 검증을 원치 않으면 제거해도 됨
            });
            showToast("🎉 회원가입이 완료되었습니다!", "success");
            setTimeout(() => navigate("/login"), 1500);
        } catch (err: any) {
            showToast(err.response?.data || "회원가입 실패", "error");
        } finally {
            setLoading(false);
        }
    };

    const timeText = useMemo(() => formatMMSS(secondsLeft), [secondsLeft]);

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
                            disabled={loading || !email}
                            className={`w-full text-white py-2 rounded-lg font-semibold transition ${
                                loading
                                    ? "bg-gray-400 cursor-not-allowed"
                                    : "bg-blue-600 hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600"
                            }`}
                        >
                            {loading ? "전송 중..." : "인증 코드 보내기"}
                        </button>
                    </>
                ) : (
                    <>
                        <div className="flex items-center justify-between mb-2">
                            <p className="text-gray-600 dark:text-gray-300 text-sm">
                                이메일로 받은 인증코드를 입력해주세요
                            </p>
                            <span
                                className={`text-sm font-semibold ${
                                    isExpired ? "text-red-500" : "text-blue-600 dark:text-blue-400"
                                }`}
                            >
                                ⏱ {isExpired ? "만료됨" : timeText}
                            </span>
                        </div>

                        <div className="flex justify-center gap-2 mb-4">
                            <input
                                type="text"
                                placeholder="6자리 코드"
                                value={code}
                                onChange={(e) =>
                                    setCode(
                                        e.target.value
                                            .replace(/[^a-zA-Z0-9]/g, "")
                                            .slice(0, 6)
                                    )
                                }
                                onBlur={verifyCodeRequest}
                                className="flex-1 text-center px-4 py-2 border rounded-lg uppercase
                                 focus:outline-none focus:ring-2 focus:ring-blue-400
                                dark:bg-gray-700 dark:text-white dark:border-gray-600 transition-colors"
                            />
                            <button
                                onClick={verifyCodeRequest}
                                disabled={isExpired || code.length < 4}
                                className={`px-4 py-2 rounded-lg font-semibold text-white transition ${
                                    isExpired || code.length < 4
                                        ? "bg-gray-400 cursor-not-allowed"
                                        : "bg-blue-600 hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600"
                                }`}
                            >
                                확인
                            </button>
                        </div>

                        <div className="flex justify-between items-center mb-4">
                            <button
                                onClick={handleResend}
                                disabled={!isExpired || loading}
                                className={`text-sm underline ${
                                    !isExpired || loading
                                        ? "text-gray-400 cursor-not-allowed"
                                        : "text-blue-600 dark:text-blue-400"
                                }`}
                            >
                                코드 재전송
                            </button>

                            {isCodeVerified ? (
                                <span className="text-green-600 dark:text-green-400 text-sm font-semibold">
                                    인증 완료
                                </span>
                            ) : isExpired ? (
                                <span className="text-red-500 text-sm font-semibold">
                                    인증 만료 — 재전송하세요
                                </span>
                            ) : (
                                <span className="text-gray-500 text-sm">
                                    코드를 입력/확인해주세요
                                </span>
                            )}
                        </div>

                        {isCodeVerified && (
                            <>
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
                                    disabled={loading}
                                    className={`w-full text-white py-2 rounded-lg font-semibold transition ${
                                        loading
                                            ? "bg-gray-400 cursor-not-allowed"
                                            : "bg-green-600 hover:bg-green-700 dark:bg-green-500 dark:hover:bg-green-600"
                                    }`}
                                >
                                    {loading ? "가입 중..." : "회원가입 완료"}
                                </button>
                            </>
                        )}
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

            {/* ✅ 단일 ToastContainer */}
            <ToastContainer
                position="top-center"
                autoClose={2500}
                hideProgressBar={false}
                closeOnClick
                pauseOnHover
                draggable
                theme="light"
                toastClassName="!rounded-xl !shadow-lg dark:!bg-gray-800 dark:!text-white"
            />
        </div>
    );
}
