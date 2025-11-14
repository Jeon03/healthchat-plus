import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import MealDetailModal from "./MealDetailModal";
import {useDashboard} from "../../context/DashboardContext.tsx";

export interface DailyMeal {
    date: string;
    totalCalories: number;
    totalProtein: number;
    totalFat: number;
    totalCarbs: number;
    mealsJson: string;
}

export default function DashboardMealCard() {
    const [meal, setMeal] = useState<DailyMeal | null>(null);
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(true);

    // ⭐ 전역 상태로부터 자동갱신 플래그 가져오기
    const { shouldRefresh, setShouldRefresh } = useDashboard();

    /** ✅ 오늘 식단 데이터 로드 */
    const fetchMeal = async () => {
        try {
            const res = await api.get<DailyMeal>("/ai/meals/today");

            if (res.data && typeof res.data === "object" && Object.keys(res.data).length > 0) {
                setMeal(res.data);
            } else {
                setMeal(null);
            }
        } catch (err) {
            console.warn("❌ 식단 정보를 불러오지 못했습니다.", err);
            setMeal(null);
        } finally {
            setLoading(false);
        }
    };

    // ✅ 첫 렌더링 시 오늘 식단 불러오기
    useEffect(() => {
        fetchMeal();
    }, []);

    // ⭐⭐ AI 입력 → setShouldRefresh(true) → 이 부분이 자동 실행됨
    useEffect(() => {
        if (shouldRefresh) {
            console.log("🔥 DashboardMealCard 갱신 감지 → 식단 다시 불러오기");
            fetchMeal();
            setShouldRefresh(false); // 플래그 리셋
        }
    }, [shouldRefresh]);

    // 모달 열기/닫기 제어
    useEffect(() => {
        if (open) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = "auto";
        }
        return () => {
            document.body.style.overflow = "auto";
        };
    }, [open]);

    const handleOpen = () => {
        if (!loading && meal) setOpen(true);
    };

    return (
        <>
            <motion.div
                initial={{ opacity: 0, y: 25, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                whileHover={
                    meal
                        ? {
                            scale: 1.03,
                            boxShadow:
                                "0 12px 28px rgba(34,197,94,0.25), 0 0 20px rgba(74,222,128,0.3)",
                            transition: { duration: 0.3 },
                        }
                        : {}
                }
                onClick={handleOpen}
                className={`
        p-7 rounded-2xl border transition-all duration-300 select-none flex flex-col 
        justify-between min-h-[180px]
        ${
                    meal
                        ? "cursor-pointer bg-gradient-to-br from-green-50/90 to-white/80 dark:from-green-900/40 dark:to-gray-900/70 border-green-300/40 dark:border-green-700/50 shadow-lg hover:shadow-xl"
                        : "cursor-not-allowed bg-gray-200/40 dark:bg-gray-700/60 border-gray-400/30 opacity-70"
                }
    `}
            >
                <div className="text-center">
                    <h3 className="text-xl font-bold text-green-500 dark:text-green-400 mb-4">
                        🥗 오늘의 식단 요약
                    </h3>

                    {loading ? (
                        <p className="text-gray-500">불러오는 중...</p>
                    ) : meal ? (
                        <>
                            <p className="text-4xl font-extrabold text-gray-900 dark:text-white mb-3">
                                {meal.totalCalories.toFixed(0)} kcal
                            </p>

                            <div className="flex justify-center gap-3 flex-wrap">
                                <div className="px-3 py-1 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-300 text-sm font-medium">
                                    단백질 {meal.totalProtein.toFixed(1)}g
                                </div>
                                <div className="px-3 py-1 rounded-full bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300 text-sm font-medium">
                                    지방 {meal.totalFat.toFixed(1)}g
                                </div>
                                <div className="px-3 py-1 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-sm font-medium">
                                    탄수화물 {meal.totalCarbs.toFixed(1)}g
                                </div>
                            </div>

                            <p className="mt-3 text-sm text-gray-500 dark:text-gray-400">
                                클릭하면 상세 식단 보기
                            </p>
                        </>
                    ) : (
                        <p className="text-gray-600 dark:text-gray-400 text-base">
                            오늘의 식단이 아직 등록되지 않았어요 🍱
                        </p>
                    )}
                </div>
            </motion.div>

            <AnimatePresence>
                {open && meal && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.25 }}
                        className="
                fixed inset-0 z-50 bg-black/50 backdrop-blur-sm
                flex justify-center items-center
            "
                    >
                        <motion.div
                            initial={{ y: 40, opacity: 0 }}
                            animate={{ y: 0, opacity: 1 }}
                            exit={{ y: 40, opacity: 0 }}
                            transition={{ duration: 0.3, ease: 'easeOut' }}
                            className="
                    w-full max-w-4xl mx-auto px-4
                    max-h-[90vh] overflow-y-auto
                "
                        >
                            <MealDetailModal
                                meal={meal}
                                onClose={() => setOpen(false)}
                                onUpdated={(updated) => {
                                    setMeal(updated);
                                    setOpen(false);
                                }}
                            />
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
}
