import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import ActivityDetailModal from "./ActivityDetailModal";
import { useDashboard } from "../../context/DashboardContext";

export interface ExerciseItem {
    name: string;
    durationMin: number;
    calories: number;
    category?: string;
    part?: string;
    intensity?: string;
}

export interface DailyActivity {
    date: string;
    totalCalories: number;
    totalDuration: number;
    exercises: ExerciseItem[];
}

/** 백엔드 응답 타입 */
interface ActivityResponse {
    activity: DailyActivity | null;
    recommendedBurn: number;
}

export default function DashboardActivityCard() {
    const [activity, setActivity] = useState<DailyActivity | null>(null);
    const [recommendedBurn, setRecommendedBurn] = useState<number>(0);

    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(true);

    // 전역 리프레시 플래그
    const { shouldRefresh, setShouldRefresh } = useDashboard();

    /** 오늘 운동 데이터 로드 */
    const fetchActivity = async () => {
        try {
            const res = await api.get<ActivityResponse>("/ai/activity/today");

            if (res.data) {
                setActivity(res.data.activity);
                setRecommendedBurn(res.data.recommendedBurn);
            } else {
                setActivity(null);
                setRecommendedBurn(0);
            }
        } catch (e) {
            console.warn("❌ 운동 정보를 불러오지 못했습니다.", e);
            setActivity(null);
            setRecommendedBurn(0);
        } finally {
            setLoading(false);
        }
    };

    // 첫 렌더링
    useEffect(() => {
        fetchActivity();
    }, []);

    // AI 입력 후 새로고침
    useEffect(() => {
        if (shouldRefresh) {
            fetchActivity();
            setShouldRefresh(false);
        }
    }, [shouldRefresh]);

    // 모달 오픈 시 body 스크롤 방지
    useEffect(() => {
        document.body.style.overflow = open ? "hidden" : "auto";
        return () => {
            document.body.style.overflow = "auto";
        };
    }, [open]);

    const handleOpen = () => {
        if (!loading && activity) setOpen(true);
    };

    return (
        <>
            <motion.div
                initial={{ opacity: 0, y: 25, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                whileHover={
                    activity
                        ? {
                            scale: 1.03,
                            boxShadow:
                                "0 12px 28px rgba(59,130,246,0.25), 0 0 20px rgba(96,165,250,0.3)",
                            transition: { duration: 0.3 },
                        }
                        : {}
                }
                onClick={handleOpen}
                className={`
        p-7 rounded-2xl border transition-all duration-300 select-none flex flex-col 
        justify-between min-h-[200px]
        ${
                    activity
                        ? "cursor-pointer bg-gradient-to-br from-blue-50/90 to-white/80 dark:from-blue-900/40 dark:to-gray-900/70 border-blue-300/40 dark:border-blue-700/50 shadow-lg hover:shadow-xl"
                        : "cursor-not-allowed bg-gray-200/40 dark:bg-gray-700/60 border-gray-400/30 opacity-70"
                }
    `}
            >
                <div className="text-center flex flex-col items-center">

                    {/* 상단 타이틀 */}
                    <h3 className="text-xl font-bold text-blue-500 dark:text-blue-400 mb-5 tracking-tight">
                        🏃 오늘의 운동 요약
                    </h3>

                    {/* 로딩 */}
                    {loading ? (
                        <p className="text-gray-500 text-sm animate-pulse">
                            불러오는 중...
                        </p>
                    ) : activity ? (
                        <>
                            {/* 운동 칼로리 */}
                            <p className="text-4xl font-extrabold text-gray-900 dark:text-white mb-2 leading-tight">
                                {activity.totalCalories.toFixed(0)} kcal
                            </p>

                            <p className="text-xs text-gray-400 dark:text-gray-500 mb-4">
                                오늘 총 소모 칼로리
                            </p>

                            {/* 운동 시간 */}
                            <div className="flex justify-center gap-3 flex-wrap mb-5">
                                <div className="
                        px-3 py-1.5 rounded-full bg-blue-100 dark:bg-blue-900/30
                        text-blue-600 dark:text-blue-300 text-sm font-medium shadow-sm
                    ">
                                    ⏱ {activity.totalDuration}분 운동
                                </div>
                            </div>

                            {/* 안내 텍스트 */}
                            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                                클릭하여 상세 운동 보기
                            </p>
                        </>
                    ) : (
                        <div className="py-6">
                            <p className="text-gray-600 dark:text-gray-400 text-base">
                                오늘의 운동이 아직 등록되지 않았어요 🏋️
                            </p>
                        </div>
                    )}
                </div>
            </motion.div>


            <AnimatePresence>
                {open && activity && (
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
                    w-full max-w-4xl px-4
                    max-h-[90vh] overflow-y-auto
                "
                        >
                            <ActivityDetailModal
                                activity={activity}
                                recommendedBurn={recommendedBurn}
                                onClose={() => setOpen(false)}
                                onUpdated={(updated) => {
                                    setActivity(updated);
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
