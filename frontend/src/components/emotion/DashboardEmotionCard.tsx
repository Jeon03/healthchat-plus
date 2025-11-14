import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import EmotionDetailModal from "./EmotionDetailModal";
import { useDashboard } from "../../context/DashboardContext";

/** EmotionSummaryDto 구조 적용 */
export interface EmotionSummaryDto {
    primaryEmotion: string;
    primaryScore: number;
    emotions: string[];
    scores: number[];
    summaries: string[];
    keywords: string[][];
    rawText: string;
    date?: string;
}

export default function DashboardEmotionCard() {
    const [emotion, setEmotion] = useState<EmotionSummaryDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [open, setOpen] = useState(false);

    const { shouldRefresh, setShouldRefresh } = useDashboard();

    const fetchEmotion = async () => {
        try {
            const res = await api.get("/ai/emotion/today");

            console.log("🔥 백엔드 감정 응답:", res.data);   // ← 추가
            if (typeof res.data === "string") {
                setEmotion(null);
            } else {
                setEmotion(res.data);
            }
        } catch (e) {
            console.warn("❌ 감정 정보를 불러오지 못했습니다.", e);
            setEmotion(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchEmotion();
    }, []);

    useEffect(() => {
        if (shouldRefresh) {
            fetchEmotion();
            setShouldRefresh(false);
        }
    }, [shouldRefresh]);

    useEffect(() => {
        document.body.style.overflow = open ? "hidden" : "auto";
        return () => {
            document.body.style.overflow = "auto";
        };
    }, [open]);

    const handleOpen = () => {
        if (!loading && emotion) setOpen(true);
    };

    const primaryIndex = emotion?.emotions?.indexOf(emotion?.primaryEmotion) ?? 0;

    const summaryText =
        emotion && emotion.summaries && emotion.summaries[primaryIndex]
            ? emotion.summaries[primaryIndex]
            : "";
    return (
        <>
            <motion.div
                initial={{ opacity: 0, y: 25, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                whileHover={
                    emotion
                        ? {
                            scale: 1.03,
                            boxShadow:
                                "0 12px 28px rgba(244,114,182,0.25), 0 0 20px rgba(251,182,206,0.3)",
                            transition: { duration: 0.3 },
                        }
                        : {}
                }
                onClick={handleOpen}
                className={`
                    p-7 rounded-2xl border transition-all duration-300 select-none flex flex-col 
                    justify-between min-h-[200px]
                    ${
                    emotion
                        ? "cursor-pointer bg-gradient-to-br from-pink-50/90 to-white/80 dark:from-pink-900/40 dark:to-gray-900/60 border-pink-200/40 dark:border-pink-700/50 shadow-lg"
                        : "cursor-not-allowed bg-gray-200/40 dark:bg-gray-700/60 border-gray-400/30 opacity-70"
                }
                `}
            >
                <div className="text-center flex flex-col items-center">
                    <h3 className="text-xl font-bold text-pink-500 dark:text-pink-300 mb-5 tracking-tight">
                        😊 오늘의 감정 요약
                    </h3>

                    {loading ? (
                        <p className="text-gray-500 text-sm animate-pulse">불러오는 중...</p>
                    ) : emotion ? (
                        <>
                            {/* 대표 감정 */}
                            <p className="text-4xl font-extrabold text-gray-900 dark:text-white mb-2 leading-tight">
                                {emotion.primaryEmotion}
                            </p>

                            <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
                                감정 강도 점수
                            </p>

                            {/* 대표 감정 요약 */}
                            <div
                                className="
                                px-3 py-1.5 rounded-full bg-pink-100 dark:bg-pink-900/30
                                text-pink-600 dark:text-pink-300 text-sm font-medium shadow-sm mb-4
                            "
                            >
                                {summaryText}
                            </div>

                            <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                                클릭하여 감정 상세 보기
                            </p>
                        </>
                    ) : (
                        <div className="py-6">
                            <p className="text-gray-600 dark:text-gray-400 text-base">
                                오늘의 감정이 아직 기록되지 않았어요 ☁️
                            </p>
                        </div>
                    )}
                </div>
            </motion.div>

            {/* 상세 모달 */}
            <AnimatePresence>
                {open && emotion && (
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
                                emotion={emotion}
                                onClose={() => setOpen(false)}
                            />
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
}
