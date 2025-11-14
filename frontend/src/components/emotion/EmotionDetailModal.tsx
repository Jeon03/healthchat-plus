import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import api from "../../api/axios";
import dayjs from "dayjs";
import { toast } from "react-toastify";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import EmotionPieChart from "../charts/EmotionPieChart.tsx";

/** EmotionSummaryDto 형태 */
interface EmotionSummaryDto {
    primaryEmotion: string;
    primaryScore: number;
    emotions: string[];
    scores: number[];
    summaries: string[];
    keywords: string[][];   // 2차원 배열
    rawText: string;
    date?: string;
}

interface Props {
    emotion: EmotionSummaryDto | null;
    onClose: () => void;
}

/** 감정별 색상 매핑 */
const EMOTION_COLORS: Record<string, string> = {
    "기쁨": "#F472B6",
    "행복": "#F472B6",

    "우울": "#A78BFA",
    "슬픔": "#A78BFA",

    "불안": "#FB923C",
    "걱정": "#FB923C",

    "분노": "#F87171",
    "짜증": "#F87171",

    "피곤": "#60A5FA",
    "지침": "#60A5FA",

    "중립": "#A1A1AA",
    "무감정": "#A1A1AA",
};
export default function EmotionDetailModal({ emotion, onClose }: Props) {

    /** ⭐ 안전한 초기 날짜 처리 */
    const initialDate = emotion?.date ? new Date(emotion.date) : new Date();

    const [current, setCurrent] = useState<EmotionSummaryDto | null>(emotion);
    const [currentDate, setCurrentDate] = useState(initialDate);
    const [slideDir, setSlideDir] = useState(0);

    useEffect(() => {
        setCurrent(emotion);

        /** ⭐ useEffect에서도 안전 처리 */
        if (emotion?.date) {
            setCurrentDate(new Date(emotion.date));
        } else {
            setCurrentDate(new Date());
        }
    }, [emotion]);

    /** 날짜 기반 감정 조회 */
    const fetchEmotionByDate = async (dateStr: string): Promise<boolean> => {
        try {
            const res = await api.get(`/ai/emotion/${dateStr}`);

            if (typeof res.data === "string") {
                toast.info("해당 날짜의 감정 데이터가 없습니다.");
                return false; // ❗ 실패
            }

            setCurrent(res.data);
            setCurrentDate(new Date(dateStr));
            return true; // 성공
        } catch {
            toast.error("감정 정보를 불러오지 못했습니다.");
            return false;
        }
    };

    const moveToDate = async (offset: number) => {
        const next = dayjs(currentDate).add(offset, "day").format("YYYY-MM-DD");

        // 먼저 슬라이드 방향 설정
        setSlideDir(offset);

        const ok = await fetchEmotionByDate(next);

        if (!ok) {
            // ❗ 실패 시 날짜 복원 + 슬라이드 취소
            setSlideDir(0);
        }
    };

    const handleDateChange = async (date: Date | null) => {
        if (!date) return;

        const formatted = dayjs(date).format("YYYY-MM-DD");
        setSlideDir(0);

        const ok = await fetchEmotionByDate(formatted);

        if (!ok) {
            // 실패 시 DatePicker 날짜도 되돌림
            setCurrentDate(current?.date ? new Date(current.date) : new Date());
        }
    };

    return (
        <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl w-full max-w-3xl mx-auto p-8 border border-gray-200 dark:border-gray-700">

            {/* === 날짜 헤더 === */}
            <div className="flex flex-col sm:flex-row justify-between items-center mb-6 gap-3">
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => moveToDate(-1)}
                        className="px-3 py-1 rounded-md bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600"
                    >
                        ⬅︎ 전날
                    </button>

                    <DatePicker
                        selected={currentDate}
                        onChange={handleDateChange}
                        dateFormat="yyyy-MM-dd"
                        className="px-3 py-1 rounded-md bg-gray-100 dark:bg-gray-800 text-center font-semibold text-pink-500 w-36"
                    />

                    <button
                        onClick={() => moveToDate(1)}
                        className="px-3 py-1 rounded-md bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600"
                    >
                        다음날 ➡︎
                    </button>
                </div>

                {current && (
                    <div className="px-4 py-2 rounded-xl text-center bg-pink-100 dark:bg-pink-800/30 text-pink-700 dark:text-pink-300">
                        <div className="font-semibold text-sm">
                            {current.primaryEmotion} ({current.primaryScore})
                        </div>
                        <div className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                            감정 강도 점수
                        </div>
                    </div>
                )}
            </div>
            <EmotionPieChart
                emotions={current?.emotions ?? []}
                scores={current?.scores ?? []}
            />
            {/* === 감정 상세 === */}
            <AnimatePresence mode="wait">
                <motion.div
                    key={
                        isNaN(currentDate.getTime())
                            ? "no-date"
                            : currentDate.toISOString()
                    }
                    initial={{ opacity: 0, x: slideDir > 0 ? 100 : -100 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: slideDir > 0 ? -100 : 100 }}
                    transition={{ duration: 0.35, ease: "easeInOut" }}
                >
                    {current && current.emotions && current.emotions.length > 0 ? (
                        <div className="space-y-6">

                            {current.emotions
                                .map((emo, idx) => ({
                                    emotion: emo,
                                    score: current.scores[idx],
                                    summary: current.summaries[idx],
                                    keywords: current.keywords[idx] || [],
                                }))
                                .sort((a, b) => b.score - a.score)
                                .map((item, idx) => {

                                    const baseColor = EMOTION_COLORS[item.emotion] || "#FBCFE8";
                                    const bgColor = `${baseColor}20`;   // 연한 배경 (투명 20)
                                    const borderColor = `${baseColor}40`; // 테두리 연하게
                                    const textColor = baseColor; // 제목 색상

                                    return (
                                        <div
                                            key={idx}
                                            className="p-4 rounded-xl space-y-3"
                                            style={{
                                                backgroundColor: bgColor,
                                                border: `1px solid ${borderColor}`
                                            }}
                                        >
                                            {/* 감정명 + 점수 */}
                                            <div className="text-lg font-bold" style={{ color: textColor }}>
                                                {item.emotion} ({item.score})
                                            </div>

                                            {/* 감정 요약 */}
                                            <p className="text-sm" style={{ color: textColor }}>
                                                {item.summary}
                                            </p>

                                            {/* 키워드 */}
                                            {item.keywords.length > 0 && (
                                                <div className="flex flex-wrap gap-2 mt-2">
                                                    {item.keywords.map((k, i) => (
                                                        <span
                                                            key={i}
                                                            className="px-3 py-1 rounded-full text-xs"
                                                            style={{
                                                                backgroundColor: borderColor,
                                                                color: textColor
                                                            }}
                                                        >
                                {k}
                            </span>
                                                    ))}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}

                            {/* 전체 원문 */}
                            <div className="mt-6">
                                <h3 className="text-lg font-semibold mb-2 text-gray-300">📝 감정 원문</h3>
                                <p className="text-gray-400 whitespace-pre-line">
                                    {current.rawText}
                                </p>
                            </div>

                        </div>
                    ) : (
                        <p className="text-gray-500 dark:text-gray-400 text-center">
                            해당 날짜에 감정 기록이 없습니다.
                        </p>
                    )}
                </motion.div>
            </AnimatePresence>

            {/* === 버튼 === */}
            <div className="mt-6 flex justify-end">
                <button
                    onClick={onClose}
                    className="px-4 py-2 bg-gray-300 dark:bg-gray-700 rounded-lg hover:bg-gray-400 dark:hover:bg-gray-600"
                >
                    닫기
                </button>
            </div>
        </div>
    );
}
