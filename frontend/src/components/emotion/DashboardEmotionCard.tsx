import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import EmotionDetailModal from "./EmotionDetailModal";
import { useDashboard } from "../../context/DashboardContext";
import dayjs from "dayjs";
import { LuSmile } from "react-icons/lu";

interface Props {
    onLoaded?: (v: boolean) => void;   // 오늘 감정 유무 전달
}

interface EmotionSummaryDto {
    primaryEmotion: string;
    primaryScore: number;
    emotions: string[];
    scores: number[];
    summaries: string[];
    keywords: string[][];
    rawText: string;
    date?: string;
}

export default function DashboardEmotionCard({ onLoaded }: Props) {
    const [emotion, setEmotion] = useState<EmotionSummaryDto | null>(null);
    const [lastEmotion, setLastEmotion] = useState<EmotionSummaryDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [open, setOpen] = useState(false);

    const { shouldRefresh, setShouldRefresh } = useDashboard();

    /** 오늘 감정 조회 */
    const fetchTodayEmotion = async () => {
        try {
            const res = await api.get("/ai/emotion/today");

            if (typeof res.data === "string") {
                setEmotion(null);
            } else {
                if (!res.data.date) {
                    res.data.date = dayjs().format("YYYY-MM-DD");
                }
                setEmotion(res.data);
            }
        } catch {
            setEmotion(null);
        } finally {
            setLoading(false);
        }
    };

    /** 최근 fallback 데이터 조회 */
    const findLastEmotion = async () => {
        let offset = 1;
        while (offset < 30) {
            const date = dayjs().subtract(offset, "day").format("YYYY-MM-DD");
            try {
                const res = await api.get(`/ai/emotion/${date}`);
                if (res.data && typeof res.data !== "string") {
                    setLastEmotion(res.data);
                    return;
                }
            } catch {}
            offset++;
        }
        setLastEmotion(null);
    };

    /** 초기 로드 */
    useEffect(() => {
        onLoaded?.(false);
        fetchTodayEmotion();
        findLastEmotion();
    }, []);

    /** refresh 요청 시 재조회 */
    useEffect(() => {
        if (shouldRefresh) {
            fetchTodayEmotion();
            findLastEmotion();
            setShouldRefresh(false);
        }
    }, [shouldRefresh]);

    /** 오늘 감정 유무 → Dashboard로 전달 */
    useEffect(() => {
        if (!loading) {
            onLoaded?.(!!emotion);   // today 데이터가 있으면 true
        }
    }, [emotion, loading]);

    /** 스크롤 잠금 */
    useEffect(() => {
        document.body.style.overflow = open ? "hidden" : "auto";
        return () => {
            document.body.style.overflow = "auto";
        };
    }, [open]);

    const modalEmotion = emotion ?? lastEmotion;

    const handleOpen = () => {
        if (!loading && modalEmotion) {
            setOpen(true);
        }
    };

    return (
        <>
            {/* 🌸 감정 카드 */}
            <motion.div
                initial={{ opacity: 0, y: 25, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                whileHover={{
                    scale: 1.03,
                    boxShadow:
                        "0 12px 28px rgba(244,114,182,0.25), 0 0 20px rgba(251,182,206,0.3)",
                    transition: { duration: 0.3 },
                }}
                onClick={handleOpen}
                className="
                    p-7 rounded-2xl border transition-all duration-300 select-none
                    flex flex-col justify-between min-h-[200px] cursor-pointer
                    bg-gradient-to-br from-pink-50/90 to-white/80
                    dark:from-pink-900/40 dark:to-gray-900/70
                    border-pink-300/40 dark:border-pink-700/50
                    shadow-lg hover:shadow-xl
                "
            >
                <div className="text-center flex flex-col items-center">

                    <div className="flex justify-center">
                        <h3 className="text-xl font-bold text-pink-500 dark:text-pink-300 mb-4 flex items-center gap-2">
                            <LuSmile className="w-6 h-6" />
                            오늘의 감정 요약
                        </h3>
                    </div>

                    {loading ? (
                        <p className="text-gray-500 text-sm animate-pulse">
                            불러오는 중...
                        </p>
                    ) : emotion ? (
                        <>
                            <p className="text-4xl font-extrabold text-gray-900 dark:text-white mb-2 leading-tight">
                                {emotion.primaryEmotion}
                            </p>

                            <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
                                감정 강도 점수
                            </p>

                            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                                클릭하여 감정 상세 보기
                            </p>
                        </>
                    ) : (
                        <div className="py-6">
                            <p className="text-gray-600 dark:text-gray-400 text-base leading-relaxed text-center">
                                오늘의 감정이 아직<br />
                                등록되지 않았어요!<br />
                                클릭하면 최근 기록을<br />
                                보여드릴게요!
                            </p>
                        </div>
                    )}
                </div>
            </motion.div>

            {/* 🌸 상세 모달 */}
            <AnimatePresence>
                {open && modalEmotion && (
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
                            className="w-full max-w-3xl px-4 max-h-[90vh] overflow-y-auto"
                        >
                            <EmotionDetailModal
                                emotion={modalEmotion}
                                onClose={() => setOpen(false)}
                            />
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
}
