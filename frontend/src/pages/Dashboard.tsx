import { useEffect, useState } from "react";
import api from "../api/axios";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { LuTriangleAlert } from "react-icons/lu";
import DashboardMealCard from "../components/meal/DashboardMealCard";
import DashboardActivityCard from "../components/exercise/DashboardActivityCard";
import DashboardEmotionCard from "../components/emotion/DashboardEmotionCard";

import maleIcon from "../assets/icons/male.svg";
import femaleIcon from "../assets/icons/female.svg";
import otherIcon from "../assets/icons/other.svg";
import { useDashboard } from "../context/DashboardContext";
import AICoachPanel from "../components/AICoachPanel";
import {LuActivity, LuBedDouble, LuDna, LuRuler, LuSettings2, LuTarget, LuUser, LuWeight} from "react-icons/lu";
import {
    LuClipboardList,
    LuSalad,
    LuDumbbell,
    LuSmilePlus,
    LuBookOpen,
} from "react-icons/lu";
interface Profile {
    nickname: string;
    gender?: string;
    age?: number;
    height?: number;
    weight?: number;
    goalWeight?: number;
    sleepGoal?: number;
    avgSleep?: number;
    goalsDetailJson?: string;
    allergiesText?: string;
    medicationsText?: string;
}

export default function Dashboard() {
    const [profile, setProfile] = useState<Profile | null>(null);
    const [goalDetails, setGoalDetails] = useState<{ goal: string; factors: string[] }[]>([]);
    const [profileLoading, setProfileLoading] = useState(true);
    const [offsetBottom, setOffsetBottom] = useState(24);

    useEffect(() => {
        const handleScroll = () => {
            const footer = document.getElementById("app-footer");
            if (!footer) return;

            const footerRect = footer.getBoundingClientRect();

            // footer가 화면 안에 들어오기 시작한 경우
            if (footerRect.top < window.innerHeight) {
                const overlap = window.innerHeight - footerRect.top;
                setOffsetBottom(overlap + 24); // footer에 닿지 않게 24px 띄움
            } else {
                setOffsetBottom(24); // 기본 bottom 위치
            }
        };

        window.addEventListener("scroll", handleScroll);
        return () => window.removeEventListener("scroll", handleScroll);
    }, []);
    /** ✅ 오늘 로그 유무 (식단/운동/감정) */
    const [hasTodayMeal, setHasTodayMeal] = useState(false);
    const [hasTodayActivity, setHasTodayActivity] = useState(false);
    const [hasTodayEmotion, setHasTodayEmotion] = useState(false);

    /** ✅ 오늘 중 하나라도 있으면 피드백 가능 */
    const canRequestFeedback = hasTodayMeal || hasTodayActivity || hasTodayEmotion;

    /** 🤖 AI 코치 상태 */
    const [coachLoading, setCoachLoading] = useState(false);
    const [coachError, setCoachError] = useState<string | null>(null);
    const [coachFeedback, setCoachFeedback] = useState<any | null>(null);

    /** 💬 AI 코치 채팅 패널 */
    const [openCoach, setOpenCoach] = useState(false);

    const { shouldRefresh, setShouldRefresh } = useDashboard();

    /** 📌 프로필 불러오기 */
    const loadProfile = async () => {
        try {
            const res = await api.get("/user/profile");
            const data = res.data;
            setProfile(data);

            if (data.goalsDetailJson) {
                const parsed = JSON.parse(data.goalsDetailJson);
                if (Array.isArray(parsed)) setGoalDetails(parsed);
            }
        } catch (err) {
            console.warn("⚠ 프로필 로드 실패:", err);
            setProfile(null);
        } finally {
            setProfileLoading(false);
        }
    };

    /** 📌 피드백 가져오기 (DB에 있으면 가져오고, 없으면 생성) */
    const fetchCoachFeedback = async () => {
        // 오늘 기록이 하나도 없으면 바로 막기
        if (!canRequestFeedback) {
            setCoachError("오늘 식단·운동·감정 중 하나 이상 기록 후 피드백을 받을 수 있어요.");
            return;
        }

        setCoachLoading(true);
        setCoachError(null);

        try {
            const res = await api.get("/coach/daily");
            setCoachFeedback(res.data);
        } catch (e) {
            setCoachError("피드백을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
        } finally {
            setCoachLoading(false);
        }
    };

    /** 📌 피드백 재생성 */
    const regenerateFeedback = async () => {
        if (!canRequestFeedback) return;

        setCoachLoading(true);
        setCoachError(null);

        try {
            const res = await api.post("/coach/daily/generate");
            setCoachFeedback(res.data);
        } catch (e) {
            setCoachError("재분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } finally {
            setCoachLoading(false);
        }
    };

    /** 📌 첫 렌더링 */
    useEffect(() => {
        document.title = "HealthChat+ 대시보드";
        loadProfile();
    }, []);

    /** 📌 AI 채팅 등으로 인한 갱신 */
    useEffect(() => {
        if (shouldRefresh) {
            loadProfile();
            setShouldRefresh(false);
        }
    }, [shouldRefresh]);


    useEffect(() => {
        if (canRequestFeedback && !coachFeedback && !coachLoading) {
            fetchCoachFeedback();
        }
    }, [canRequestFeedback, coachFeedback, coachLoading]);

    if (profileLoading) {
        return (
            <div className="text-center mt-20 text-gray-600 dark:text-gray-300">
                불러오는 중...
            </div>
        );
    }

    const isIncomplete =
        !profile || !profile.height || !profile.weight || !profile.gender || !profile.age;

    const bmi =
        profile?.height && profile?.weight
            ? (profile.weight / ((profile.height / 100) ** 2)).toFixed(1)
            : "-";

    const genderIcon =
        profile?.gender === "M"
            ? maleIcon
            : profile?.gender === "F"
                ? femaleIcon
                : otherIcon;

    const getProfileBgClass = () => {
        if (profile?.gender === "M") {
            // 🔵 남성: 운동카드 Blue 테마
            return `
            bg-gradient-to-br from-blue-50/90 to-white/80
            dark:from-blue-900/40 dark:to-gray-900/70
            border-blue-300/40 dark:border-blue-700/50
            shadow-lg
        `;
        }
        if (profile?.gender === "F") {
            // 🌸 여성: 운동카드 스타일의 Pink 버전
            return `
            bg-gradient-to-br from-pink-50/90 to-white/80
            dark:from-pink-900/40 dark:to-gray-900/70
            border-pink-300/40 dark:border-pink-700/50
            shadow-lg
        `;
        }
        // 💜 기타
        return `
        bg-gradient-to-br from-purple-50/90 to-white/80
        dark:from-purple-900/40 dark:to-gray-900/70
        border-purple-300/40 dark:border-purple-700/50
        shadow-lg
    `;
    };

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.6 }}
            className="max-w-4xl mx-auto px-6 py-12"
        >
            <motion.h2
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2, duration: 0.6 }}
                className="text-3xl font-bold mb-10 text-gray-900 dark:text-gray-100"
            >
                오늘의 건강 요약
            </motion.h2>

            <motion.div
                initial={{ opacity: 0, y: 28, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
                whileHover={{
                    scale: 1.015,
                    boxShadow:
                        profile?.gender === "M"
                            ? "0 12px 35px rgba(59,130,246,0.25), 0 0 35px rgba(147,197,253,0.35)"
                            : profile?.gender === "F"
                                ? "0 12px 35px rgba(244,114,182,0.25), 0 0 35px rgba(251,182,206,0.35)"
                                : "0 12px 35px rgba(139,92,246,0.25), 0 0 35px rgba(167,139,250,0.35)",
                    transition: { duration: 0.35 },
                }}
                className={`
        relative p-12 mb-14 rounded-3xl
        backdrop-blur-xl shadow-lg transition-all duration-500
        text-[17px] leading-relaxed border border-white/20 dark:border-gray-700/40

        ${getProfileBgClass()}
    `}
            >
                {/* 🔹 프로필 헤더 */}
                <div className="flex items-center gap-5 mb-6 pb-5 border-b border-gray-300/40 dark:border-gray-700/40">
                    <div className="w-20 h-20 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center overflow-hidden shadow-md">
                        <img
                            src={genderIcon}
                            alt="프로필 아이콘"
                            className="w-100 h-100 transition-transform hover:scale-110"
                        />
                    </div>

                    <div>
                        <h3 className="text-2xl font-bold text-gray-900 dark:text-gray-100 tracking-tight">
                            {isIncomplete ? "프로필이 아직 완성되지 않았어요" : "내 프로필"}
                        </h3>

                        {profile?.nickname && (
                            <p className="text-gray-600 dark:text-gray-400 text-base mt-1 tracking-tight">
                                {profile.nickname} 님
                            </p>
                        )}
                    </div>
                </div>

                {/* 🔹 프로필 정보 */}
                {isIncomplete ? (
                    <>
                        <p className="text-gray-600 dark:text-gray-300 mb-4">
                            일부 정보가 누락되었습니다. 프로필을 완성해주세요.
                        </p>


                        <div
                            className="
    bg-yellow-100/70 dark:bg-yellow-900/40
    text-yellow-800 dark:text-yellow-300
    rounded-xl p-4 mb-6 border border-yellow-400/30
    flex items-start gap-3
  "
                        >
                            <LuTriangleAlert className="w-6 h-6 mt-0.5 flex-shrink-0 text-yellow-600 dark:text-yellow-300" />

                            <p className="leading-relaxed">
                                일부 건강 정보가 입력되지 않았습니다.
                                <br />
                                맞춤 피드백을 위해 프로필을 완성해주세요.
                            </p>
                        </div>

                        <Link
                            to="/profile"
                            className="
                    px-6 py-3 w-full text-center rounded-xl font-medium
                    bg-green-600 text-white shadow-md hover:bg-green-700
                    transition-all hover:shadow-lg
                "
                        >
                            설정하기
                        </Link>
                    </>
                ) : (
                    <>
                        {/* 🔸 기본 정보 */}
                        <div
                            className="
                    grid sm:grid-cols-2 gap-x-10 gap-y-3
                    text-gray-800 dark:text-gray-200 text-[17px]
                "
                        >
                            <p className="flex items-center gap-3">
                                <LuUser className="text-blue-500 w-5 h-5" />
                                {profile.nickname} ({profile.gender === "M" ? "남성" : "여성"} / {profile.age}세)
                            </p>

                            <p className="flex items-center gap-3">
                                <LuRuler className="text-indigo-500 w-5 h-5" />
                                키: {profile.height}cm
                            </p>

                            <p className="flex items-center gap-3">
                                <LuWeight className="text-green-500 w-5 h-5" />
                                몸무게: {profile.weight}kg
                            </p>

                            <p className="flex items-center gap-3">
                                <LuActivity className="text-blue-400 w-5 h-5" />
                                BMI: <span className="text-blue-500 font-semibold ml-1">{bmi}</span>
                            </p>

                            {profile.goalWeight && (
                                <p className="flex items-center gap-3">
                                    <LuTarget className="text-pink-500 w-5 h-5" />
                                    목표 체중:
                                    <span className="text-green-500 font-semibold ml-1">
                            {profile.goalWeight}kg
                        </span>
                                </p>
                            )}

                            {(profile.avgSleep || profile.sleepGoal) && (
                                <p className="flex items-center gap-3">
                                    <LuBedDouble className="text-yellow-500 w-5 h-5" />
                                    평균 수면: {profile.avgSleep ?? "-"}시간
                                </p>
                            )}
                        </div>

                        {/* 🔸 건강 정보 */}
                        <div className="mt-6 border-t border-gray-300/30 dark:border-gray-700/40 pt-5 space-y-3">
                            <h4 className="text-lg font-semibold text-orange-500 flex items-center gap-2">
                                <LuDna className="w-5 h-5 text-orange-500" />
                                건강 정보
                            </h4>

                            <p>
                                <strong>• 알레르기:</strong>{" "}
                                {profile.allergiesText?.trim()
                                    ? profile.allergiesText
                                    : "등록된 알레르기 정보가 없습니다."}
                            </p>

                            <p>
                                <strong>• 복용 중인 약:</strong>{" "}
                                {profile.medicationsText?.trim()
                                    ? profile.medicationsText
                                    : "등록된 약 정보가 없습니다."}
                            </p>
                        </div>

                        {/* 🔸 나의 목표 */}
                        {goalDetails.length > 0 && (
                            <>
                                <div className="my-6 border-t border-gray-300/30 dark:border-gray-700/40" />

                                <h4 className="text-xl font-bold mb-4 text-pink-500 flex items-center gap-2">
                                    <LuTarget className="w-6 h-6 text-pink-500" />
                                    나의 목표
                                </h4>

                                <div className="grid sm:grid-cols-2 gap-x-10 gap-y-6">
                                    {goalDetails.map((g, idx) => (
                                        <div key={idx}>
                                            <p className="font-semibold text-blue-600 dark:text-blue-400 text-lg">
                                                • {g.goal}
                                            </p>
                                            {g.factors?.length > 0 && (
                                                <ul className="list-disc list-inside text-gray-700 dark:text-gray-300 mt-2">
                                                    {g.factors.map((f, i) => (
                                                        <li key={i}>{f}</li>
                                                    ))}
                                                </ul>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </>
                        )}

                        {/* 수정 버튼 */}
                        <div className="mt-8 flex justify-end">
                            <Link
                                to="/profile"
                                className="
                        px-6 py-2.5 rounded-xl bg-blue-600 text-white font-medium
                        shadow-md hover:bg-blue-700 hover:shadow-lg transition-all
                        flex items-center gap-2
                    "
                            >
                                <LuSettings2 className="w-5 h-5" />
                                수정하기
                            </Link>
                        </div>
                    </>
                )}
            </motion.div>



            {/* ================== 카드 3종 (식단/운동/감정) ================== */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* onLoaded로 오늘 데이터 유무 전달 */}
                <DashboardActivityCard onLoaded={setHasTodayActivity} />
                <DashboardMealCard onLoaded={setHasTodayMeal} />
                <DashboardEmotionCard onLoaded={setHasTodayEmotion} />
            </div>

            {/* ================== 🤖 AI 건강 코치 ================== */}
            <section
                className="
        mt-12 p-8 rounded-2xl
        bg-gradient-to-br
        from-blue-50/90 via-white/80 to-blue-100/60
        dark:from-blue-900/40 dark:via-gray-900/60 dark:to-blue-800/40

        backdrop-blur-xl
        border border-blue-300/40 dark:border-blue-700/40
        shadow-[0_8px_20px_rgba(0,0,0,0.12)]
        transition-all duration-500

        hover:scale-[1.02]
        hover:shadow-[0_12px_28px_rgba(59,130,246,0.25),0_0_30px_rgba(147,197,253,0.3)]
    "
            >
                <div className="flex items-center justify-between mb-6">
                    <h3 className="text-2xl font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
                        AI 건강 코치 피드백
                    </h3>

                    <div className="flex items-center gap-3">

                        {/* ✅ 아직 피드백 생성 안됨 → "피드백 받기" 버튼만 */}
                        {!coachFeedback && (
                            <button
                                onClick={fetchCoachFeedback}
                                disabled={coachLoading || !canRequestFeedback}
                                className={`
        px-5 py-3 rounded-lg font-semibold shadow-md transition-all
        active:scale-95

        ${
                                    canRequestFeedback
                                        ? "bg-gradient-to-r from-blue-600 to-indigo-600 text-white hover:from-blue-700 hover:to-indigo-700 shadow-[0_4px_12px_rgba(59,130,246,0.35)]"
                                        : "bg-gray-400 text-white cursor-not-allowed"
                                }
    `}
                            >
                                {coachLoading ? "분석 중..." : "피드백 받기"}
                            </button>
                        )}

                        {/* ✅ 피드백 존재 → "재분석" 버튼만 */}
                        {coachFeedback && (
                            <button
                                onClick={regenerateFeedback}
                                disabled={coachLoading || !canRequestFeedback}
                                className={`px-5 py-3 rounded-lg font-semibold shadow-md transition-all active:scale-95
        ${canRequestFeedback
                                    ? "bg-gradient-to-r from-blue-600 to-indigo-600 text-white hover:from-blue-700 hover:to-indigo-700 shadow-[0_4px_12px_rgba(59,130,246,0.35)]"
                                    : "bg-gray-400 text-white cursor-not-allowed"
                                }
    `}
                            >
                                {coachLoading ? "재분석 중..." : "재분석"}
                            </button>
                        )}
                    </div>
                </div>

                {!canRequestFeedback && !coachFeedback && (
                    <p className="text-gray-600 dark:text-gray-400 mt-3 text-base leading-relaxed">
                        오늘의 <span className="font-semibold text-blue-600 dark:text-blue-400">식단 · 운동 · 감정</span> 중
                        <span className="font-semibold"> 하나라도 기록</span>하면
                        AI 건강 코치가 맞춤 피드백을 드릴게요
                    </p>
                )}

                {coachLoading && (
                    <p className="text-gray-600 dark:text-gray-300">
                        AI가 오늘의 기록을 분석하는 중입니다. 잠시만 기다려주세요...
                    </p>
                )}

                {coachError && (
                    <p className="mt-3 text-red-500 dark:text-red-400">{coachError}</p>
                )}

                {coachFeedback && (
                    <motion.div
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="mt-8 space-y-6 p-6 rounded-xl
            bg-gray-50/80 dark:bg-gray-800/50
            border border-gray-300/30 dark:border-gray-700/40
            shadow-inner"
                    >
                        {/* 📌 하루 요약 */}
                        <div>
                            <h4 className="text-lg font-bold text-blue-600 dark:text-blue-400 mb-2 flex items-center gap-2">
                                <LuClipboardList className="w-5 h-5" />
                                하루 요약
                            </h4>
                            <p className="text-gray-700 dark:text-gray-300 leading-relaxed">
                                {coachFeedback.summary}
                            </p>
                        </div>

                        {/* 🥗 식단 피드백 */}
                        <div>
                            <h4 className="text-lg font-bold text-green-600 dark:text-green-400 mb-2 flex items-center gap-2">
                                <LuSalad className="w-5 h-5" />
                                식단 피드백
                            </h4>
                            <p className="text-gray-700 dark:text-gray-300">
                                {coachFeedback.dietAdvice}
                            </p>
                        </div>

                        {/* 운동 피드백 */}
                        <div>
                            <h4 className="text-lg font-bold text-yellow-500 dark:text-yellow-400 mb-2 flex items-center gap-2">
                                <LuDumbbell className="w-5 h-5" />
                                운동 피드백
                            </h4>
                            <p className="text-gray-700 dark:text-gray-300">
                                {coachFeedback.exerciseAdvice}
                            </p>
                        </div>

                        {/* 감정 코칭 */}
                        <div>
                            <h4 className="text-lg font-bold text-pink-500 dark:text-pink-400 mb-2 flex items-center gap-2">
                                <LuSmilePlus className="w-5 h-5" />
                                감정 코칭
                            </h4>
                            <p className="text-gray-700 dark:text-gray-300">
                                {coachFeedback.emotionAdvice}
                            </p>
                        </div>

                        {/* 목표 정렬 분석 */}
                        <div>
                            <h4 className="text-lg font-bold text-indigo-500 dark:text-indigo-400 mb-2 flex items-center gap-2">
                                <LuTarget className="w-5 h-5" />
                                목표 정렬 분석
                            </h4>
                            <p className="text-gray-700 dark:text-gray-300">
                                {coachFeedback.goalAlignment}
                            </p>
                        </div>

                        {/* 참고 문헌 */}
                        {coachFeedback.references?.length > 0 && (
                            <div>
                                <h4 className="text-lg font-bold text-gray-800 dark:text-gray-200 mb-3 flex items-center gap-2">
                                    <LuBookOpen className="w-5 h-5" />
                                    참고 문헌 근거
                                </h4>

                                <ul className="space-y-3">
                                    {coachFeedback.references.map((ref: any, i: number) => (
                                        <li
                                            key={i}
                                            className="p-4 rounded-lg
                                bg-gray-200/60 dark:bg-gray-700/60
                                shadow border border-gray-300/40 dark:border-gray-700/50"
                                        >
                                            <p className="font-medium">
                                <span className="text-gray-800 dark:text-gray-100">
                                    출처:
                                </span>{" "}
                                                {ref.source}
                                            </p>

                                            <p className="mt-1 text-sm">
                                                {ref.snippet}
                                            </p>

                                            <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                                                {ref.comment}
                                            </p>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </motion.div>
                )}
            </section>

            <button
                onClick={() => setOpenCoach(true)}
                style={{ bottom: `${offsetBottom}px` }}
                className="
                    fixed right-6 z-50
                    px-6 py-3 rounded-xl
                    bg-blue-600 text-white font-semibold
                    shadow-xl hover:bg-blue-700 active:scale-95
                    transition-all duration-300
                "
            >
                🤖 AI 건강 코치
            </button>

            <AICoachPanel open={openCoach} onClose={() => setOpenCoach(false)} />
        </motion.div>
    );
}
