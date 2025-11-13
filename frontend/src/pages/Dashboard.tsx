import {useEffect, useState} from "react";
import api from "../api/axios";
import {Link} from "react-router-dom";
import {motion} from "framer-motion";

import ChatContainer from "../components/chat/ChatContainer";
import DashboardMealCard from "../components/meal/DashboardMealCard";
import DashboardActivityCard from "../components/exercise/DashboardActivityCard";

import maleIcon from "../assets/icons/male.svg";
import femaleIcon from "../assets/icons/female.svg";
import otherIcon from "../assets/icons/other.svg";
import {useDashboard} from "../context/DashboardContext.tsx";

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

    const { shouldRefresh, setShouldRefresh } = useDashboard();


    const loadProfile = async () => {
        try {
            const res = await api.get("/user/profile");
            const data = res.data;
            setProfile(data);

            // 목표 파싱
            if (data.goalsDetailJson) {
                try {
                    const parsed = JSON.parse(data.goalsDetailJson);
                    if (Array.isArray(parsed)) {
                        setGoalDetails(parsed);
                    }
                } catch (e) {
                    console.warn("goalsDetailJson 파싱 실패:", e);
                }
            }
        } catch (err) {
            console.warn("⚠️ 프로필 정보를 불러올 수 없습니다.", err);
            setProfile(null);
        } finally {
            setProfileLoading(false);
        }
    };

    // ⭐ 첫 렌더링에서 프로필 불러오기
    useEffect(() => {
        document.title = "HealthChat+ 대시보드";
        loadProfile();
    }, []);

    // ⭐ 자동 갱신 감지 — AI 채팅에서 setShouldRefresh(true) 보내면 실행됨
    useEffect(() => {
        if (shouldRefresh) {
            console.log("🔥 대시보드 자동 갱신 감지! → 프로필 다시 불러오는 중...");
            loadProfile();
            setShouldRefresh(false);
        }
    }, [shouldRefresh]);

    if (profileLoading) {
        return (
            <div className="text-center mt-20 text-gray-600 dark:text-gray-300">
                불러오는 중...
            </div>
        );
    }

    // ✅ 프로필 불완전 여부 판별
    const isIncomplete =
        !profile ||
        !profile.height ||
        !profile.weight ||
        !profile.gender ||
        !profile.age;

    // ✅ BMI 계산
    const bmi =
        profile?.height && profile?.weight
            ? (profile.weight / ((profile.height / 100) ** 2)).toFixed(1)
            : "-";

    // ✅ 성별별 아이콘 선택
    const genderIcon =
        profile?.gender === "M"
            ? maleIcon
            : profile?.gender === "F"
                ? femaleIcon
                : otherIcon;

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.6, ease: "easeOut" }}
            className="max-w-4xl mx-auto px-6 py-12"
        >
            <motion.h2
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2, duration: 0.6, ease: "easeOut" }}
                className="text-3xl font-bold mb-10 text-gray-900 dark:text-gray-100 tracking-tight"
            >
                오늘의 건강 요약
            </motion.h2>

            {/* ✅ 프로필 카드 (목표 통합 + 페이드인 애니메이션) */}
            <motion.div
                initial={{ opacity: 0, y: 30, scale: 0.97 }} // 살짝 아래 + 작게 시작
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{
                    duration: 0.2,
                    ease: [0.16, 1, 0.3, 1],
                }}
                whileHover={{
                    scale: 1.01,
                    boxShadow:
                        "0 10px 25px rgba(59,130,246,0.25), 0 0 20px rgba(147,197,253,0.2)",
                    transition: { duration: 0.3 },
                }}
                className="relative p-10 mb-12 rounded-2xl
        bg-gradient-to-br from-gray-100/80 to-white/90 dark:from-gray-800/70 dark:to-gray-900/80
        backdrop-blur-md border border-gray-300/40 dark:border-gray-700/60
        shadow-[0_8px_20px_rgba(0,0,0,0.12)] hover:shadow-[0_10px_30px_rgba(59,130,246,0.25)]
        transition-all duration-500 text-[17px] leading-relaxed"
            >
                {/* 프로필 헤더 */}
                <div className="flex items-center gap-6 mb-8 border-b border-gray-300/50 dark:border-gray-700/50 pb-6">
                    <div className="w-24 h-24 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center overflow-hidden shadow-md">
                        <img
                            src={genderIcon}
                            alt="프로필 아이콘"
                            className="w-100 h-100 transition-transform hover:scale-110"
                        />
                    </div>
                    <div>
                        <h3 className="text-3xl font-semibold text-gray-900 dark:text-gray-100">
                            {isIncomplete ? "프로필이 아직 완성되지 않았어요" : "내 프로필"}
                        </h3>
                        {profile?.nickname && (
                            <p className="text-gray-600 dark:text-gray-400 text-base mt-1">
                                {profile.nickname} 님
                            </p>
                        )}
                    </div>
                </div>

                {/* ✅ 프로필 정보 */}
                {isIncomplete ? (
                    <>
                        <p className="text-gray-600 dark:text-gray-300 mb-5 text-[17px]">
                            일부 정보가 누락되었습니다. 프로필을 완성해주세요.
                        </p>
                        <div className="bg-yellow-100 dark:bg-yellow-900/40 text-yellow-800 dark:text-yellow-300 rounded-lg p-4 mb-6 border border-yellow-400/20 text-[17px]">
                            ⚠️ 아직 일부 건강 정보를 입력하지 않았습니다.
                            <br />
                            맞춤 피드백을 받으려면 프로필을 완성해주세요.
                        </div>
                        <div className="flex justify-center">
                            <Link
                                to="/profile"
                                className="flex items-center justify-center gap-2 px-6 py-3 w-full bg-green-600 hover:bg-green-700 text-white font-medium rounded-lg transition shadow-md hover:shadow-lg text-[17px]"
                            >
                                설정하기
                            </Link>
                        </div>
                    </>
                ) : (
                    <>
                        {/* ✅ 완성된 프로필 정보 */}
                        <div className="grid sm:grid-cols-2 gap-x-10 gap-y-3 text-[17px] text-gray-700 dark:text-gray-300">
                            <p>👤 {profile.nickname} ({profile.gender === "M" ? "남성" : "여성"} / {profile.age}세)</p>
                            <p>📏 키: {profile.height}cm</p>
                            <p>⚖️ 몸무게: {profile.weight}kg</p>
                            <p>🧮 BMI: <span className="text-blue-400 font-semibold">{bmi}</span></p>
                            {profile.goalWeight && (
                                <p>🎯 목표 체중: <span className="text-green-400 font-semibold">{profile.goalWeight}kg</span></p>
                            )}
                            {(profile.avgSleep || profile.sleepGoal) && (
                                <p>😴 평균 수면: {profile.avgSleep ?? "-"}시간</p>
                            )}
                        </div>

                        {/* ✅ 알레르기 & 복용약 */}
                        <div className="mt-8 border-t border-gray-300/30 dark:border-gray-700/50 pt-6 space-y-3 text-gray-700 dark:text-gray-300">
                            <h4 className="text-lg font-semibold text-orange-500 dark:text-orange-400">🧬 건강 정보</h4>
                            <p>
                                <span className="font-semibold text-gray-900 dark:text-gray-100">• 알레르기:</span>{" "}
                                {profile.allergiesText?.trim()
                                    ? profile.allergiesText
                                    : "등록된 알레르기 정보가 없습니다."}
                            </p>
                            <p>
                                <span className="font-semibold text-gray-900 dark:text-gray-100">• 복용 중인 약:</span>{" "}
                                {profile.medicationsText?.trim()
                                    ? profile.medicationsText
                                    : "등록된 약 정보가 없습니다."}
                            </p>
                        </div>

                        {/* ✅ 나의 목표 */}
                        {goalDetails.length > 0 && (
                            <>
                                <div className="my-8 border-t border-gray-300/30 dark:border-gray-700/50" />
                                <h4 className="text-2xl font-bold mb-5 text-pink-500 dark:text-pink-400 flex items-center gap-2">
                                    🎯 나의 목표
                                </h4>
                                <div className="grid sm:grid-cols-2 gap-x-10 gap-y-6">
                                    {goalDetails.map((g, idx) => (
                                        <div key={idx} className="space-y-2">
                                            <p className="font-semibold text-blue-600 dark:text-blue-400 text-lg">
                                                • {g.goal}
                                            </p>
                                            {g.factors?.length > 0 && (
                                                <ul className="list-disc list-inside text-gray-700 dark:text-gray-300 text-base leading-relaxed space-y-1 ml-1.5">
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

                        <div className="mt-10 flex justify-end">
                            <Link
                                to="/profile"
                                className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition shadow-md hover:shadow-lg"
                            >
                                ⚙️ 수정하기
                            </Link>
                        </div>
                    </>
                )}
            </motion.div>

            {/* ✅ 건강 데이터 카드들 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">

                {/* 🏃 운동 기록 — 이제 실데이터 연동 */}
                <DashboardActivityCard />

                {/* 🥗 식단 요약 — 실데이터 연동 */}
                <DashboardMealCard />

                {/* 💬 감정 일기 (더미 유지) */}
                <div className="p-6 bg-gray-100/70 dark:bg-gray-800/70 rounded-xl border border-gray-300/30 dark:border-gray-700/50 shadow-md hover:shadow-lg hover:scale-[1.02] transition-all duration-300">
                    <h3 className="text-xl font-semibold mb-2 text-purple-400">💬 감정 일기</h3>
                    <p className="text-gray-700 dark:text-gray-300">"오늘은 기분이 안정적이고 활기찼어요!"</p>
                </div>

            </div>
            {/* ✅ AI 피드백 섹션 */}
            <section className="mt-10 bg-gray-100/70 dark:bg-gray-800/70 rounded-2xl border border-gray-300/30 dark:border-gray-700/50 shadow-md hover:shadow-lg p-8 transition-all duration-300">
                <h3 className="text-2xl font-semibold mb-4 text-gray-800 dark:text-gray-100">
                    🤖 AI 건강 코치 피드백
                </h3>
                <p className="text-gray-700 dark:text-gray-300 leading-relaxed">
                    💡 운동량은 충분하지만, 단백질 섭취가 약간 부족해요. 내일은 계란이나 두부를 추가해보세요.
                </p>
            </section>
            <motion.section
                initial={{ opacity: 0, y: 25 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
                className="mt-14 bg-gray-100/70 dark:bg-gray-800/70 rounded-2xl border border-gray-300/30 dark:border-gray-700/50 shadow-lg p-8"
            >
                <h3 className="text-2xl font-bold mb-4 text-gray-800 dark:text-gray-100 flex items-center gap-2">
                    🤖 AI 건강 코치와 대화하기
                </h3>
                <p className="text-gray-600 dark:text-gray-400 mb-6">
                    식단, 운동, 수면 습관에 대해 물어보세요. AI가 맞춤 피드백을 제공합니다 💬
                </p>

                <div className="p-6">
                    <h2 className="text-2xl font-semibold mb-4">AI 건강 코치 💬</h2>
                    <ChatContainer />
                </div>
            </motion.section>
        </motion.div>
    );
}
