import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import dayjs from "dayjs";
import { toast } from "react-toastify";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import CategoryDonutWithPartChart from "../charts/CategoryDonutWithPartChart.tsx";

// ✔ lucide-react 아이콘
import { Activity, Flame, Clock, Zap } from "lucide-react";

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

interface ActivityDetailModalProps {
    activity: DailyActivity;
    recommendedBurn: number;
    onClose: () => void;
    onUpdated: (updated: DailyActivity) => void;
}

export default function ActivityDetailModal({
                                                activity,
                                                recommendedBurn,
                                                onClose,
                                                onUpdated
                                            }: ActivityDetailModalProps) {

    const [items, setItems] = useState<ExerciseItem[]>([]);
    const [saving, setSaving] = useState(false);
    const [currentDate, setCurrentDate] = useState(new Date(activity.date));
    const [slideDir, setSlideDir] = useState(0);

    useEffect(() => {
        setItems(activity.exercises || []);
        setCurrentDate(new Date(activity.date));
    }, [activity]);

    const fetchActivityByDate = async (dateStr: string) => {
        try {
            const res = await api.get(`/ai/activity/${dateStr}`);

            if (!res.data || !res.data.exercises) {
                toast.info("해당 날짜의 운동이 없습니다.");
                return;
            }
            setItems(res.data.exercises);
            setCurrentDate(new Date(dateStr));
        } catch {
            toast.error("운동 정보를 불러오지 못했습니다.");
        }
    };

    const moveToDate = async (offset: number) => {
        setSlideDir(offset);
        const next = dayjs(currentDate).add(offset, "day").format("YYYY-MM-DD");
        await fetchActivityByDate(next);
    };

    const handleDateChange = async (date: Date | null) => {
        if (!date) return;
        const formatted = dayjs(date).format("YYYY-MM-DD");
        setSlideDir(0);
        await fetchActivityByDate(formatted);
    };

    const setField = (index: number, field: keyof ExerciseItem, value: string) => {
        setItems(prev =>
            prev.map((item, i) =>
                i === index
                    ? {
                        ...item,
                        [field]:
                            ["durationMin", "calories"].includes(field)
                                ? Number(value) || 0
                                : value,
                    }
                    : item
            )
        );
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            const body: DailyActivity = {
                ...activity,
                date: dayjs(currentDate).format("YYYY-MM-DD"),
                exercises: items,
                totalCalories,
                totalDuration
            };

            const res = await api.post("/ai/activity/save", body);
            onUpdated(res.data);
            toast.success("🏃 운동 데이터가 저장되었습니다!");
        } catch (e) {
            console.error(e);
            toast.error("운동 저장 중 오류가 발생했습니다.");
        } finally {
            setSaving(false);
        }
    };

    const totalCalories = items.reduce((sum, i) => sum + (i.calories || 0), 0);
    const totalDuration = items.reduce((sum, i) => sum + (i.durationMin || 0), 0);

    const burnRatio = recommendedBurn > 0
        ? Math.min(100, (totalCalories / recommendedBurn) * 100)
        : 0;

    return (
        <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl w-full max-w-4xl mx-auto p-8 border border-gray-200 dark:border-gray-700">

            {/* === 날짜 헤더 === */}
            <div className="flex flex-col sm:flex-row justify-between items-center mb-6 gap-3">
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => moveToDate(-1)}
                        className="px-3 py-1 rounded-md bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600"
                    >
                        ⬅︎ 전날
                    </button>

                    <DatePicker
                        selected={currentDate}
                        onChange={handleDateChange}
                        dateFormat="yyyy-MM-dd"
                        className="px-3 py-1 rounded-md bg-gray-100 dark:bg-gray-800 text-center font-semibold text-blue-500 w-36"
                    />

                    <button
                        onClick={() => moveToDate(1)}
                        className="px-3 py-1 rounded-md bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600"
                    >
                        다음날 ➡︎
                    </button>
                </div>

                {/* 총합 (아이콘 적용) */}
                <div className="px-4 py-2 rounded-xl text-center bg-blue-100 dark:bg-blue-800/30 text-blue-700 dark:text-blue-300 flex flex-col items-center">
                    <div className="font-semibold text-sm flex items-center gap-1">
                        <Activity className="w-4 h-4" />
                        총 소모 칼로리 {totalCalories} kcal
                    </div>
                    <div className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                        운동시간 {totalDuration}분
                    </div>
                </div>
            </div>

            {/* === 권장 운동 소모량 게이지 === */}
            <div className="mb-6 bg-blue-50 dark:bg-blue-900/20 rounded-xl p-4 border border-blue-200 dark:border-blue-700 shadow-inner">
                <div className="flex justify-between mb-1">
                    <span className="text-blue-600 dark:text-blue-300 font-semibold flex items-center gap-1">
                        <Zap className="w-4 h-4" />
                        권장 운동 소모량
                    </span>
                    <span className="font-medium text-gray-700 dark:text-gray-300">
                        {totalCalories} / {recommendedBurn} kcal
                    </span>
                </div>

                <div className="w-full bg-blue-200/40 dark:bg-blue-800/30 rounded-full h-3 overflow-hidden">
                    <div
                        style={{ width: `${burnRatio}%` }}
                        className="h-full bg-gradient-to-r from-blue-400 to-blue-600 dark:from-blue-500 dark:to-blue-300 rounded-full shadow-[0_0_8px_rgba(96,165,250,0.7)] transition-all"
                    ></div>
                </div>
            </div>

            {/* === 전체 요약 === */}
            <div className="mt-6 text-center text-sm text-gray-700 dark:text-gray-300 flex justify-center items-center gap-2">
                <Activity className="w-4 h-4 text-blue-500 dark:text-blue-400" />
                <span className="font-semibold text-blue-500 dark:text-blue-400">
                    전체 합계:
                </span>
                {totalCalories} kcal · {totalDuration}분
            </div>

            {/* 차트 */}
            <div className="my-10 flex justify-center">
                <CategoryDonutWithPartChart
                    data={items.map(i => ({
                        category: i.category || "OTHER",
                        part: i.part || "OTHER",
                        total: i.calories || 0
                    }))}
                />
            </div>

            {/* === 운동 리스트 === */}
            <AnimatePresence mode="wait">
                <motion.div
                    key={currentDate.toISOString()}
                    initial={{ opacity: 0, x: slideDir > 0 ? 100 : -100 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: slideDir > 0 ? -100 : 100 }}
                    transition={{ duration: 0.35, ease: "easeInOut" }}
                >
                    <div className="overflow-y-auto px-2" style={{ maxHeight: "calc(100vh - 320px)" }}>
                        <div className="flex justify-center">
                            <div className="w-full max-w-[720px] space-y-6">

                                {items.map((item, i) => (
                                    <div key={i} className="pb-4 border-b border-gray-200/40 dark:border-gray-700/40">

                                        <h3 className="font-semibold text-lg mb-2 text-gray-800 dark:text-gray-200">
                                            {item.name || `운동 ${i + 1}`}
                                        </h3>

                                        <div className="grid grid-cols-[260px,160px,160px,160px] gap-2 mb-2">
                                            <input
                                                className="border rounded px-2 py-1 text-sm"
                                                value={item.name}
                                                onChange={(e) => setField(i, "name", e.target.value)}
                                            />
                                            <input
                                                className="border rounded px-2 py-1 text-right text-sm"
                                                value={String(item.durationMin)}
                                                onChange={(e) => setField(i, "durationMin", e.target.value)}
                                            />
                                            <input
                                                className="border rounded px-2 py-1 text-right text-sm"
                                                value={String(item.calories)}
                                                onChange={(e) => setField(i, "calories", e.target.value)}
                                            />
                                        </div>

                                        {/* 🔥⏱ 아이콘 적용 */}
                                        <div className="text-sm text-gray-600 dark:text-gray-400 flex items-center gap-4">
                                            <span className="flex items-center gap-1">
                                                <Flame className="w-4 h-4 text-red-500/80" />
                                                {item.calories} kcal
                                            </span>

                                            <span className="flex items-center gap-1">
                                                <Clock className="w-4 h-4 text-gray-500" />
                                                {item.durationMin}분
                                            </span>
                                        </div>

                                    </div>
                                ))}

                            </div>
                        </div>
                    </div>
                </motion.div>
            </AnimatePresence>

            {/* === 버튼 === */}
            <div className="mt-6 flex justify-end gap-3 max-w-[720px] mx-auto">
                <button
                    onClick={onClose}
                    className="px-4 py-2 bg-gray-300 dark:bg-gray-700 rounded-lg hover:bg-gray-400 dark:hover:bg-gray-600"
                >
                    닫기
                </button>

                <button
                    onClick={handleSave}
                    disabled={saving}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg shadow-md hover:shadow-lg"
                >
                    {saving ? "저장 중..." : "저장하기"}
                </button>
            </div>

        </div>
    );
}
