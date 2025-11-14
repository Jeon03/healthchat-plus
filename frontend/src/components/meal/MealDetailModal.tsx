import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import api from "../../api/axios";
import dayjs from "dayjs";
import { toast } from "react-toastify";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import NutritionDonut from "../charts/NutritionDonut";

export interface FoodItem {
    name: string;
    quantity: number;
    unit: string;
    calories: number;
    protein: number;
    fat: number;
    carbs: number;
}

export interface Meal {
    time: string;
    foods: FoodItem[];
}

export interface DailyMeal {
    date: string;
    mealsJson: string;
    totalCalories: number;
    totalProtein: number;
    totalFat: number;
    totalCarbs: number;
}

interface MealDetailModalProps {
    meal: DailyMeal;
    onClose: () => void;
    onUpdated: (updated: DailyMeal) => void;
}

/** ✅ 식단 점수 계산 (WHO 기준 단4:지3:탄6 비율) */
function calculateNutritionScore(protein: number, fat: number, carbs: number): number {
    const total = protein + fat + carbs;
    if (total === 0) return 0;

    const p = protein / total;
    const f = fat / total;
    const c = carbs / total;

    const ideal = { p: 4 / 13, f: 3 / 13, c: 6 / 13 };
    const deviation = Math.pow(p - ideal.p, 2) + Math.pow(f - ideal.f, 2) + Math.pow(c - ideal.c, 2);

    // ✅ 스케일 조정 (5000 → 500)
    const score = Math.max(0, 100 - deviation * 500);
    return Math.round(score);
}

/** ✅ 합계 계산 유틸 */
function accumulate(meals: Meal[]) {
    let totalCalories = 0;
    let totalProtein = 0;
    let totalFat = 0;
    let totalCarbs = 0;
    for (const m of meals) {
        for (const f of m.foods) {
            totalCalories += f.calories || 0;
            totalProtein += f.protein || 0;
            totalFat += f.fat || 0;
            totalCarbs += f.carbs || 0;
        }
    }
    return { totalCalories, totalProtein, totalFat, totalCarbs };
}

export default function MealDetailModal({ meal, onClose, onUpdated }: MealDetailModalProps) {
    const [parsedMeals, setParsedMeals] = useState<Meal[]>([]);
    const [saving, setSaving] = useState(false);
    const [currentDate, setCurrentDate] = useState(new Date(meal.date));
    const [nutritionScore, setNutritionScore] = useState(0);
    const [slideDir, setSlideDir] = useState(0); // ✅ 슬라이드 방향 (-1 전날 / +1 다음날)

    /** ✅ 초기 로드 */
    useEffect(() => {
        try {
            const parsed = JSON.parse(meal.mealsJson || "[]");
            setParsedMeals(parsed);
            setCurrentDate(new Date(meal.date));
        } catch (e) {
            console.warn("❌ mealsJson 파싱 실패:", e);
        }
    }, [meal]);

    /** ✅ parsedMeals 변경 시 식단 점수 재계산 */
    useEffect(() => {
        if (parsedMeals.length === 0) {
            setNutritionScore(0);
            return;
        }
        const totals = accumulate(parsedMeals);
        setNutritionScore(calculateNutritionScore(totals.totalProtein, totals.totalFat, totals.totalCarbs));
    }, [parsedMeals]);

    /** ✅ 날짜 이동 */
    const fetchMealByDate = async (targetDate: string) => {
        try {
            const res = await api.get(`/ai/meals/${targetDate}`);
            if (!res.data || !res.data.mealsJson) {
                toast.info("해당 날짜의 식단이 없습니다.");
                return;
            }
            const parsed = JSON.parse(res.data.mealsJson);
            setParsedMeals(parsed);
            setCurrentDate(new Date(targetDate));
        } catch {
            toast.error("식단 정보를 불러오지 못했습니다.");
        }
    };

    const moveToDate = async (offset: number) => {
        setSlideDir(offset);
        const next = dayjs(currentDate).add(offset, "day").format("YYYY-MM-DD");
        await fetchMealByDate(next);
    };

    const handleDateChange = async (date: Date | null) => {
        if (!date) return;
        const formatted = dayjs(date).format("YYYY-MM-DD");
        setSlideDir(0);
        await fetchMealByDate(formatted);
    };

    /** ✅ 필드 변경 처리 */
    const setField = (mealTime: string, foodIndex: number, field: keyof FoodItem, value: string) => {
        setParsedMeals((prev) =>
            prev.map((m) =>
                m.time === mealTime
                    ? {
                        ...m,
                        foods: m.foods.map((f, i) =>
                            i === foodIndex
                                ? {
                                    ...f,
                                    [field]:
                                        ["quantity", "calories", "protein", "fat", "carbs"].includes(field)
                                            ? Number(value) || 0
                                            : value,
                                }
                                : f
                        ),
                    }
                    : m
            )
        );
    };

    /** ✅ 저장 요청 */
    const handleSave = async () => {
        setSaving(true);
        try {
            const body: DailyMeal = {
                ...meal,
                date: dayjs(currentDate).format("YYYY-MM-DD"),
                mealsJson: JSON.stringify(parsedMeals),
                ...accumulate(parsedMeals),
            };
            const res = await api.post<DailyMeal>("/ai/meals/save", body);
            onUpdated(res.data);
            toast.success("✅ 식단이 저장되었습니다.");
        } catch (e) {
            console.error("❌ 식단 저장 실패:", e);
            toast.error("식단 저장 중 오류가 발생했습니다.");
        } finally {
            setSaving(false);
        }
    };

    const mealOrder = ["아침", "점심", "저녁", "간식"];
    const sortedMeals = [...parsedMeals].sort((a, b) => mealOrder.indexOf(a.time) - mealOrder.indexOf(b.time));
    const totals = accumulate(sortedMeals);

    return (
            <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl w-full max-w-4xl mx-auto p-8 border border-gray-200 dark:border-gray-700">

                {/* ✅ 상단 헤더 */}
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
                            className="px-3 py-1 rounded-md bg-gray-100 dark:bg-gray-800 text-center font-semibold text-green-500 w-36"
                        />

                        <button
                            onClick={() => moveToDate(1)}
                            className="px-3 py-1 rounded-md bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600"
                        >
                            다음날 ➡︎
                        </button>
                    </div>

                    {/* ✅ 식단 점수 + 영양 비율 */}
                    <div
                        className={`px-4 py-2 rounded-xl text-center ${
                            nutritionScore >= 80
                                ? "bg-green-100 text-green-700 dark:bg-green-800/30"
                                : nutritionScore >= 60
                                    ? "bg-yellow-100 text-yellow-700 dark:bg-yellow-800/30"
                                    : "bg-red-100 text-red-700 dark:bg-red-800/30"
                        }`}
                    >
                        <div className="font-semibold text-sm">
                            🥗 식단 점수: {nutritionScore} / 100
                        </div>
                        {totals.totalProtein + totals.totalFat + totals.totalCarbs > 0 && (
                            <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                                단{" "}
                                {((totals.totalProtein / (totals.totalProtein + totals.totalFat + totals.totalCarbs)) * 100).toFixed(1)}%
                                · 지{" "}
                                {((totals.totalFat / (totals.totalProtein + totals.totalFat + totals.totalCarbs)) * 100).toFixed(1)}%
                                · 탄{" "}
                                {((totals.totalCarbs / (totals.totalProtein + totals.totalFat + totals.totalCarbs)) * 100).toFixed(1)}%
                            </div>
                        )}
                    </div>
                </div>

                {/* ✅ 애니메이션으로 감싼 본문 */}
                <AnimatePresence mode="wait">
                    <motion.div
                        key={currentDate.toISOString()}
                        initial={{ opacity: 0, x: slideDir > 0 ? 100 : -100 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: slideDir > 0 ? -100 : 100 }}
                        transition={{ duration: 0.35, ease: "easeInOut" }}
                    >
                        <div className="mt-8 flex justify-center mb-10">
                            <NutritionDonut
                                protein={totals.totalProtein}
                                fat={totals.totalFat}
                                carbs={totals.totalCarbs}
                                size={240}
                            />
                        </div>
                        {/* ✅ 식단 테이블 */}
                        <div className="overflow-y-auto px-2" style={{ maxHeight: "calc(100vh - 260px)" }}>
                            <div className="flex justify-center">
                                <div className="w-full max-w-[720px] space-y-6">
                                    {sortedMeals.map((m) => {
                                        const subtotals = accumulate([m]);
                                        return (
                                            <div key={m.time} className="pb-4 border-b border-gray-200/40 dark:border-gray-700/40">
                                                <h3 className="font-semibold text-lg mb-3 text-gray-800 dark:text-gray-200">{m.time}</h3>
                                                {m.foods.map((f, i) => (
                                                    <div key={i} className="grid grid-cols-[150px,80px,80px,80px,80px,80px] gap-2 mb-2">
                                                        <input className="border rounded px-2 py-1 text-sm" value={f.name}
                                                               onChange={(e) => setField(m.time, i, "name", e.target.value)} />
                                                        <input className="border rounded px-2 py-1 text-right text-sm" value={String(f.quantity)}
                                                               onChange={(e) => setField(m.time, i, "quantity", e.target.value)} />
                                                        <input className="border rounded px-2 py-1 text-right text-sm" value={String(f.calories)}
                                                               onChange={(e) => setField(m.time, i, "calories", e.target.value)} />
                                                        <input className="border rounded px-2 py-1 text-right text-sm" value={String(f.protein)}
                                                               onChange={(e) => setField(m.time, i, "protein", e.target.value)} />
                                                        <input className="border rounded px-2 py-1 text-right text-sm" value={String(f.fat)}
                                                               onChange={(e) => setField(m.time, i, "fat", e.target.value)} />
                                                        <input className="border rounded px-2 py-1 text-right text-sm" value={String(f.carbs)}
                                                               onChange={(e) => setField(m.time, i, "carbs", e.target.value)} />
                                                    </div>
                                                ))}
                                                <div className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                                                    단백질 {subtotals.totalProtein.toFixed(1)}g · 지방{" "}
                                                    {subtotals.totalFat.toFixed(1)}g · 탄수화물{" "}
                                                    {subtotals.totalCarbs.toFixed(1)}g
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>

                        {/* ✅ 전체 합계 + 도넛차트 */}
                        <div className="mt-6 flex justify-center">
                            <div className="w-full max-w-[720px] pt-3 text-center text-sm text-gray-700 dark:text-gray-300">
                                <span className="font-semibold text-red-500 dark:text-red-400">🍱 전체 합계:</span>{" "}
                                총 <span className="font-semibold text-gray-900 dark:text-white">{totals.totalCalories.toFixed(0)} kcal</span>{" "}
                                · 단백질{" "}
                                <span className="font-semibold text-blue-500 dark:text-blue-400">{totals.totalProtein.toFixed(1)} g</span>{" "}
                                · 지방{" "}
                                <span className="font-semibold text-yellow-500 dark:text-yellow-400">{totals.totalFat.toFixed(1)} g</span>{" "}
                                · 탄수화물{" "}
                                <span className="font-semibold text-green-500 dark:text-green-400">{totals.totalCarbs.toFixed(1)} g</span>


                            </div>
                        </div>
                    </motion.div>
                </AnimatePresence>

                {/* ✅ 버튼 */}
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
                        className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg shadow-md hover:shadow-lg"
                    >
                        {saving ? "저장 중..." : "저장하기"}
                    </button>
                </div>
            </div>
    );
}
