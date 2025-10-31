import { useEffect } from "react";

export default function Dashboard() {
    useEffect(() => {
        document.title = "HealthChat+ 대시보드";
    }, []);

    return (
        <>
            <h2 className="text-3xl font-semibold mb-6 text-gray-800 dark:text-gray-100 mt-10" >
                오늘의 건강 요약
            </h2>

            {/* ✅ 카드 3개 */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* 🏃 운동 기록 */}
                <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow hover:shadow-lg transition">
                    <h3 className="text-xl font-semibold mb-2 text-blue-600 dark:text-blue-400">
                        🏃 운동 기록
                    </h3>
                    <p className="text-gray-600 dark:text-gray-300">
                        오늘 총 7,824보 걸음 / 45분 운동
                    </p>
                </div>

                {/* 🥗 식단 요약 */}
                <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow hover:shadow-lg transition">
                    <h3 className="text-xl font-semibold mb-2 text-green-600 dark:text-green-400">
                        🥗 식단 요약
                    </h3>
                    <p className="text-gray-600 dark:text-gray-300">
                        단백질 78g / 탄수화물 190g / 지방 40g
                    </p>
                </div>

                {/* 💬 감정 일기 */}
                <div className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow hover:shadow-lg transition">
                    <h3 className="text-xl font-semibold mb-2 text-purple-600 dark:text-purple-400">
                        💬 감정 일기
                    </h3>
                    <p className="text-gray-600 dark:text-gray-300">
                        "오늘은 기분이 안정적이고 활기찼어요!"
                    </p>
                </div>
            </div>

            {/* ✅ AI 피드백 섹션 */}
            <section className="mt-10 bg-white dark:bg-gray-800 rounded-xl shadow p-8 transition">
                <h3 className="text-2xl font-semibold mb-4 text-gray-700 dark:text-gray-100">
                    AI 건강 코치 피드백
                </h3>
                <p className="text-gray-700 dark:text-gray-300 leading-relaxed">
                    💡 운동량은 충분하지만, 단백질 섭취가 약간 부족해요. 내일은 계란이나 두부를 추가해보세요.
                </p>
            </section>
        </>
    );
}
