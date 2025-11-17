import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import ActivityDetailModal from "./ActivityDetailModal";
import { useDashboard } from "../../context/DashboardContext";
import dayjs from "dayjs";
import { LuDumbbell } from "react-icons/lu";


interface Props {
    onLoaded?: (v: boolean) => void;
}
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

interface ActivityResponse {
    activity: DailyActivity | null;
    recommendedBurn: number;
}

export default function DashboardActivityCard({ onLoaded }: Props) {
    const [activity, setActivity] = useState<DailyActivity | null>(null);
    const [lastActivity, setLastActivity] = useState<DailyActivity | null>(null);
    const [recommendedBurn, setRecommendedBurn] = useState<number>(0);

    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(true);

    const { shouldRefresh, setShouldRefresh } = useDashboard();

    /** 오늘 운동 기록 불러오기 */
    const fetchActivity = async () => {
        try {
            const res = await api.get<ActivityResponse>("/ai/activity/today");

            console.log("🔥 [운동 조회 응답]", res.data);              // 전체 응답
            console.log("🔥 [오늘 Activity]", res.data.activity);      // DailyActivity
            console.log("🔥 [Exercises]", res.data.activity?.exercises); // 운동 리스트

            if (res.data.activity) {
                setActivity(res.data.activity);
                onLoaded?.(true);    // 🔥 오늘 운동 있음
            } else {
                setActivity(null);
                onLoaded?.(false);   // 🔥 오늘 운동 없음
            }

            setRecommendedBurn(res.data.recommendedBurn || 0);

        } catch (e) {
            console.error("❌ 운동 조회 오류:", e);
            setActivity(null);
            setRecommendedBurn(0);
            onLoaded?.(false);
        } finally {
            setLoading(false);
        }
    };

    const findLastActivity = async () => {
        let offset = 1;

        while (offset <= 10) {   // 10일만 조회 (너무 많으면 비효율적)
            const target = dayjs().subtract(offset, "day").format("YYYY-MM-DD");

            try {
                const res = await api.get(`/ai/activity/${target}`);
                console.log(`📅 [${target}] 조회`, res.data);

                // 1️⃣ 문자열이면 → 운동 없음 → 즉시 중단
                if (typeof res.data === "string") {
                    console.log("문자열 응답 → 운동 없음 → 중단");
                    break;
                }

                // 2️⃣ JSON이지만 activity 자체가 없음
                if (!res.data.activity) {
                    console.log("activity null → 운동 없음 → 중단");
                    break;
                }

                const activity = res.data.activity;

                // 3️⃣ exercises가 비었으면 기록 없음 → 중단
                if (!activity.exercises || activity.exercises.length === 0) {
                    console.log("exercises 없음 → 운동 데이터 없음 → 중단");
                    break;
                }

                // 4️⃣ 진짜 운동 기록 발견 → 저장 후 종료
                console.log("➡️ 최근 운동 발견", activity);
                setLastActivity(activity);
                return;

            } catch (e) {
                console.log(`❌ 조회 실패: ${target}`, e);
                break;  // 예외 발생해도 종료 (무한 루프 방지)
            }

            offset++;
        }

        setLastActivity(null);
    };


    // 최초 로드
    useEffect(() => {
        onLoaded?.(false);
        fetchActivity();
        findLastActivity();
    }, []);

    // refresh 감지
    useEffect(() => {
        if (shouldRefresh) {
            fetchActivity();
            setShouldRefresh(false);
        }
    }, [shouldRefresh]);

    // 모달 스크롤 잠금
    useEffect(() => {
        document.body.style.overflow = open ? "hidden" : "auto";
        return () => {
            document.body.style.overflow = "auto";
        };
    }, [open]);

    /** 클릭 → 항상 모달 열림 */
    const handleOpen = () => {
        if (!loading) setOpen(true);
    };

    /** 모달에 넘길 값 (오늘 데이터 없으면 → 최근 데이터) */
    const modalActivity = activity ?? lastActivity;

    return (
        <>
            {/* 🔵 운동 카드 */}
            <motion.div
                initial={{ opacity: 0, y: 25, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                whileHover={{
                    scale: 1.03,
                    boxShadow:
                        "0 12px 28px rgba(59,130,246,0.25), 0 0 20px rgba(96,165,250,0.3)",
                    transition: { duration: 0.3 },
                }}
                onClick={handleOpen}
                className="
                    p-7 rounded-2xl border transition-all duration-300 select-none
                    flex flex-col justify-between min-h-[200px] cursor-pointer
                    bg-gradient-to-br from-blue-50/90 to-white/80
                    dark:from-blue-900/40 dark:to-gray-900/70
                    border-blue-300/40 dark:border-blue-700/50
                    shadow-lg hover:shadow-xl
                "
            >
                <div className="text-center flex flex-col items-center">
                    <div className="flex justify-center">
                        <h3 className="text-xl font-bold text-blue-500 dark:text-blue-400 mb-4 flex items-center gap-2">
                            <LuDumbbell className="w-6 h-6" />
                            오늘의 운동 요약
                        </h3>
                    </div>

                    {loading ? (
                        <p className="text-gray-500 text-sm animate-pulse">
                            불러오는 중...
                        </p>
                    ) : activity ? (
                        <>
                            <p className="text-4xl font-extrabold text-gray-900 dark:text-white mb-2 leading-tight">
                                {activity.totalCalories.toFixed(0)} kcal
                            </p>

                            <p className="text-xs text-gray-400 dark:text-gray-500 mb-4">
                                오늘 총 소모 칼로리
                            </p>

                            <div className="flex justify-center gap-3 flex-wrap mb-5">
                                <div
                                    className="
                                    px-3 py-1.5 rounded-full bg-blue-100 dark:bg-blue-900/30
                                    text-blue-600 dark:text-blue-300 text-sm font-medium shadow-sm
                                "
                                >
                                    ⏱ {activity.totalDuration}분 운동
                                </div>
                            </div>

                            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                                클릭하여 상세 운동 보기
                            </p>
                        </>
                    ) : (
                        <div className="py-6">
                            <p className="text-gray-600 dark:text-gray-400 text-base leading-relaxed text-center">
                                오늘의 운동이 아직<br />
                                등록되지 않았어요!<br />
                                클릭하면 최근 기록을<br />
                                보여드릴게요!
                            </p>
                        </div>
                    )}
                </div>
            </motion.div>

            {/* 🔵 모달 */}
            <AnimatePresence>
                {open && modalActivity && (
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
                                activity={modalActivity}
                                recommendedBurn={recommendedBurn}
                                onClose={() => setOpen(false)}
                                onUpdated={(updated) => {
                                    // 오늘 날짜 업데이트면 today's activity로 저장
                                    if (updated.date === dayjs().format("YYYY-MM-DD")) {
                                        setActivity(updated);
                                    }
                                    setLastActivity(updated); // 최근 기록 갱신
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
