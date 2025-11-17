import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import MealDetailModal from "./MealDetailModal";
import { useDashboard } from "../../context/DashboardContext.tsx";
import dayjs from "dayjs";
import { LuSalad } from "react-icons/lu";

interface Props {
    onLoaded?: (v: boolean) => void;
}
export interface DailyMeal {
    date: string;
    totalCalories: number;
    totalProtein: number;
    totalFat: number;
    totalCarbs: number;
    mealsJson: string;
}

export default function DashboardMealCard({ onLoaded }: Props) {
    const [meal, setMeal] = useState<DailyMeal | null>(null);
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(true);

    const [lastMeal, setLastMeal] = useState<DailyMeal | null>(null);

    // 글로벌 refresh
    const { shouldRefresh, setShouldRefresh } = useDashboard();

    const fetchMeal = async () => {
        try {
            const res = await api.get<DailyMeal>("/ai/meals/today");

            if (res.data && res.data.mealsJson && res.data.mealsJson !== "[]") {
                setMeal(res.data);
                onLoaded?.(true);
            } else {
                setMeal(null);
                onLoaded?.(false);
            }
        } catch {
            setMeal(null);
            onLoaded?.(false);
        } finally {
            setLoading(false);
        }
    };

    /** 🔥 가장 최근 기록 탐색 */
    const findLastAvailableMeal = async () => {
        let offset = 1;
        while (offset < 30) {
            const date = dayjs().subtract(offset, "day").format("YYYY-MM-DD");
            try {
                const res = await api.get(`/ai/meals/${date}`);
                if (res.data && res.data.mealsJson) {
                    setLastMeal(res.data);
                    return;
                }
            } catch {}
            offset++;
        }
        setLastMeal(null);
    };

    /** 최초 렌더 → 오늘 식단 확인 */
    useEffect(() => {
        onLoaded?.(false);   // ✅ 로딩 시작 시 "오늘 기록 없음" 먼저 알림
        fetchMeal();
        findLastAvailableMeal();
    }, []);

    /** AI 입력 후 자동 새로고침 */
    useEffect(() => {
        if (shouldRefresh) {
            fetchMeal();
            setShouldRefresh(false);
        }
    }, [shouldRefresh]);

    useEffect(() => {
        document.body.style.overflow = open ? "hidden" : "auto";
        return () => {
            document.body.style.overflow = "auto";
        };
    }, [open]);

    /** 클릭 시 항상 모달 오픈 */
    const handleOpen = () => {
        if (!loading) setOpen(true);
    };

    /** 모달에 넘길 데이터 = 오늘 데이터 있으면 today, 없으면 최근 기록 */
    const modalMeal = meal ?? lastMeal;

    return (
        <>
            {/* 🔥 카드 (meal 없어도 동일 스타일) */}
            <motion.div
                initial={{ opacity: 0, y: 25, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                whileHover={{
                    scale: 1.03,
                    transition: { duration: 0.3 },
                    boxShadow:
                        "0 12px 28px rgba(34,197,94,0.25), 0 0 20px rgba(74,222,128,0.3)",
                }}
                onClick={handleOpen}
                className="
                    p-7 rounded-2xl border transition-all duration-300 select-none
                    flex flex-col justify-between min-h-[180px] cursor-pointer
                    bg-gradient-to-br from-green-50/90 to-white/80
                    dark:from-green-900/40 dark:to-gray-900/70
                    border-green-300/40 dark:border-green-700/50
                    shadow-lg hover:shadow-xl
                "
            >
                <div className="text-center">
                    <div className="flex justify-center">
                        <h3 className="text-xl font-bold text-green-500 dark:text-green-400 mb-4 flex items-center gap-2">
                            <LuSalad className="w-6 h-6" />
                            오늘의 식단 요약
                        </h3>
                    </div>

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
                        <div className="py-6">
                            <p className="text-gray-600 dark:text-gray-400 text-base leading-relaxed text-center mt-1">
                                오늘의 식단이 아직<br />
                                등록되지 않았어요!<br />
                                클릭하면 최근 기록을<br />
                                보여드릴게요!
                            </p>
                        </div>
                    )}
                </div>
            </motion.div>

            {/* 🔥 모달 (today 없으면 lastMeal로 표시) */}
            <AnimatePresence>
                {open && modalMeal && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.25 }}
                        className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex justify-center items-center"
                    >
                        <motion.div
                            initial={{ y: 40, opacity: 0 }}
                            animate={{ y: 0, opacity: 1 }}
                            exit={{ y: 40, opacity: 0 }}
                            transition={{ duration: 0.3, ease: "easeOut" }}
                            className="w-full max-w-4xl mx-auto px-4 max-h-[90vh] overflow-y-auto"
                        >
                            <MealDetailModal
                                meal={modalMeal}
                                onClose={() => setOpen(false)}
                                onUpdated={(updated) => {
                                    setMeal(updated.date === dayjs().format("YYYY-MM-DD") ? updated : meal);
                                    setLastMeal(updated);
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
