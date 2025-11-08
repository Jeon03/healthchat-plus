import { useEffect, useState } from "react";
import api from "../api/axios";
import { Link } from "react-router-dom";

interface Profile {
    nickname: string;
    gender?: string;
    age?: number;
    height?: number;
    weight?: number;
    bmi?: number;
    bodyFat?: number;
    goalWeight?: number;
    sleepGoal?: number;
    avgSleep?: number;
}

export default function Dashboard() {
    const [profile, setProfile] = useState<Profile | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        document.title = "HealthChat+ 대시보드";

        (async () => {
            try {
                const res = await api.get("/user/profile");
                setProfile(res.data);
            } catch {
                console.warn("프로필 정보가 아직 설정되지 않았습니다.");
                setProfile(null);
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) {
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

    // ✅ BMI 계산 (값이 있을 때만)
    const bmi =
        profile?.height && profile?.weight
            ? (profile.weight / ((profile.height / 100) ** 2)).toFixed(1)
            : "-";

    return (
        <div className="max-w-5xl mx-auto px-6 py-10">
            <h2 className="text-3xl font-semibold mb-8 text-gray-800 dark:text-gray-100">
                오늘의 건강 요약
            </h2>

            {/* ✅ 프로필 카드 */}
            <div className="p-6 mb-10 bg-white dark:bg-gray-800 rounded-xl shadow hover:shadow-lg transition">
                {isIncomplete ? (
                    <>
                        <h3 className="text-xl font-semibold mb-2 text-gray-800 dark:text-gray-200">
                            👤 프로필이 아직 완성되지 않았어요
                        </h3>
                        {profile ? (
                            <>
                                <p className="text-gray-600 dark:text-gray-400 mb-2">
                                    {profile.nickname}님,
                                    입력된 정보:{" "}
                                    {[
                                        profile.height && `키 ${profile.height}cm`,
                                        profile.weight && `몸무게 ${profile.weight}kg`,
                                        profile.gender && `성별 ${profile.gender === "M" ? "남성" : "여성"}`,
                                        profile.age && `${profile.age}세`,
                                    ]
                                        .filter(Boolean)
                                        .join(" / ") || "없음"}
                                </p>
                                <p className="text-yellow-400 text-sm mb-4">
                                    ⚠️ 아직 일부 건강 정보를 입력하지 않았습니다.
                                    <br />
                                    맞춤 피드백을 받으려면 프로필을 완성해주세요.
                                </p>
                            </>
                        ) : (
                            <p className="text-gray-600 dark:text-gray-400 mb-4">
                                건강 데이터를 입력하면 AI가 맞춤 피드백을 제공합니다.
                            </p>
                        )}
                        <Link
                            to="/profile"
                            className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition"
                        >
                            📝 설정하기
                        </Link>
                    </>
                ) : (
                    <>
                        <h3 className="text-xl font-semibold mb-2 text-gray-800 dark:text-gray-200">
                            👤 내 프로필
                        </h3>
                        <p className="text-gray-700 dark:text-gray-300">
                            <b>{profile.nickname}</b>님 (
                            {profile.gender === "M" ? "남성" : "여성"} / {profile.age}세)
                        </p>
                        <p className="text-gray-700 dark:text-gray-300">
                            키 {profile.height}cm / 몸무게 {profile.weight}kg / BMI {bmi}
                        </p>

                        {profile.goalWeight && (
                            <p className="text-gray-700 dark:text-gray-300">
                                🎯 목표 체중: {profile.goalWeight}kg
                            </p>
                        )}
                        {profile.sleepGoal && (
                            <p className="text-gray-700 dark:text-gray-300">
                                😴 수면 목표: {profile.sleepGoal}시간 (현재 평균:{" "}
                                {profile.avgSleep ?? "-"}시간)
                            </p>
                        )}

                        <div className="mt-4 text-right">
                            <Link
                                to="/profile"
                                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition"
                            >
                                ⚙️ 수정하기
                            </Link>
                        </div>
                    </>
                )}
            </div>

            {/* ✅ 건강 데이터 카드 3개 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow hover:shadow-lg transition">
                    <h3 className="text-xl font-semibold mb-2 text-blue-600 dark:text-blue-400">
                        🏃 운동 기록
                    </h3>
                    <p className="text-gray-600 dark:text-gray-300">
                        오늘 총 7,824보 걸음 / 45분 운동
                    </p>
                </div>

                <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow hover:shadow-lg transition">
                    <h3 className="text-xl font-semibold mb-2 text-green-600 dark:text-green-400">
                        🥗 식단 요약
                    </h3>
                    <p className="text-gray-600 dark:text-gray-300">
                        단백질 78g / 탄수화물 190g / 지방 40g
                    </p>
                </div>

                <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow hover:shadow-lg transition">
                    <h3 className="text-xl font-semibold mb-2 text-purple-600 dark:text-purple-400">
                        💬 감정 일기
                    </h3>
                    <p className="text-gray-600 dark:text-gray-300">
                        "오늘은 기분이 안정적이고 활기찼어요!"
                    </p>
                </div>
            </div>

            {/* ✅ AI 피드백 */}
            <section className="mt-10 bg-white dark:bg-gray-800 rounded-xl shadow p-8 transition">
                <h3 className="text-2xl font-semibold mb-4 text-gray-700 dark:text-gray-100">
                    AI 건강 코치 피드백
                </h3>
                <p className="text-gray-700 dark:text-gray-300 leading-relaxed">
                    💡 운동량은 충분하지만, 단백질 섭취가 약간 부족해요. 내일은 계란이나 두부를 추가해보세요.
                </p>
            </section>
        </div>
    );
}
