import {useEffect, useState} from "react";
import {motion} from "framer-motion";
import api from "../../api/axios";

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

export default function MealDetailModal({ meal, onClose, onUpdated }: MealDetailModalProps) {
    const [parsedMeals, setParsedMeals] = useState<Meal[]>([]);
    const [saving, setSaving] = useState(false);

    /** ✅ 초기 데이터 로드 */
    useEffect(() => {
        try {
            const parsed = JSON.parse(meal.mealsJson || "[]");
            console.log("📋 상세 식단 데이터:", parsed);
            setParsedMeals(parsed);
        } catch (e) {
            console.warn("❌ mealsJson 파싱 실패:", e);
        }
    }, [meal]);

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
                mealsJson: JSON.stringify(parsedMeals),
                ...accumulate(parsedMeals),
            };
            const res = await api.post<DailyMeal>("/ai/meals/save", body);
            onUpdated(res.data);
            onClose();
        } catch (e) {
            console.error("❌ 식단 저장 실패:", e);
            alert("식단 저장 중 오류가 발생했습니다.");
        } finally {
            setSaving(false);
        }
    };

    /** ✅ 끼니 순서 고정 */
    const mealOrder = ["아침", "점심", "저녁", "간식"];
    const sortedMeals = [...parsedMeals].sort(
        (a, b) => mealOrder.indexOf(a.time) - mealOrder.indexOf(b.time)
    );

    return (
        <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
        >
            <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl w-full max-w-4xl mx-auto p-8 border border-gray-200 dark:border-gray-700">
                <h2 className="text-2xl font-bold mb-6 text-green-500 text-center">
                    🥗 오늘의 식단 상세
                </h2>

                {/* ✅ 스크롤 가능한 테이블 영역 */}
                <div className="overflow-y-auto px-2"
                     style={{ maxHeight: "calc(100vh - 220px)" }}>
                <div className="flex justify-center">
                        <div className="w-full max-w-[720px] space-y-6">
                            {sortedMeals.map((m) => {
                                const totals = accumulate([m]);
                                return (
                                    <div
                                        key={m.time}
                                        className="pb-4 border-b border-gray-200/40 dark:border-gray-700/40"
                                    >
                                        <h3 className="font-semibold text-lg mb-3 text-gray-800 dark:text-gray-200">
                                            {m.time}
                                        </h3>

                                        {/* ✅ 테이블 헤더 */}
                                        <div className="grid grid-cols-[150px,80px,80px,80px,80px,80px] gap-2 mb-2 text-xs text-gray-500">
                                            <span>음식명</span>
                                            <span className="text-right">g</span>
                                            <span className="text-right">kcal</span>
                                            <span className="text-right">단백질</span>
                                            <span className="text-right">지방</span>
                                            <span className="text-right">탄수화물</span>
                                        </div>

                                        {/* ✅ 음식 행 */}
                                        {m.foods.map((f, i) => (
                                            <div
                                                key={i}
                                                className="grid grid-cols-[150px,80px,80px,80px,80px,80px] gap-2 mb-2"
                                            >
                                                <input
                                                    className="border rounded px-2 py-1 text-sm"
                                                    value={f.name}
                                                    onChange={(e) =>
                                                        setField(m.time, i, "name", e.target.value)
                                                    }
                                                />
                                                <input
                                                    className="border rounded px-2 py-1 text-right text-sm"
                                                    value={String(f.quantity)}
                                                    onChange={(e) =>
                                                        setField(m.time, i, "quantity", e.target.value)
                                                    }
                                                />
                                                <input
                                                    className="border rounded px-2 py-1 text-right text-sm"
                                                    value={String(f.calories)}
                                                    onChange={(e) =>
                                                        setField(m.time, i, "calories", e.target.value)
                                                    }
                                                />
                                                <input
                                                    className="border rounded px-2 py-1 text-right text-sm"
                                                    value={String(f.protein)}
                                                    onChange={(e) =>
                                                        setField(m.time, i, "protein", e.target.value)
                                                    }
                                                />
                                                <input
                                                    className="border rounded px-2 py-1 text-right text-sm"
                                                    value={String(f.fat)}
                                                    onChange={(e) =>
                                                        setField(m.time, i, "fat", e.target.value)
                                                    }
                                                />
                                                <input
                                                    className="border rounded px-2 py-1 text-right text-sm"
                                                    value={String(f.carbs)}
                                                    onChange={(e) =>
                                                        setField(m.time, i, "carbs", e.target.value)
                                                    }
                                                />
                                            </div>
                                        ))}

                                        {/* ✅ 끼니별 합계 */}
                                        <div className="mt-1 text-sm text-gray-600 dark:text-gray-400">
                                            단백질 {totals.totalProtein.toFixed(1)}g · 지방{" "}
                                            {totals.totalFat.toFixed(1)}g · 탄수화물{" "}
                                            {totals.totalCarbs.toFixed(1)}g
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>

                {/* ✅ 전체 합계 (표와 폭 통일) */}
                <div className="mt-6 flex justify-center">
                    <div className="w-full max-w-[720px] border-t border-gray-300 dark:border-gray-700 pt-3 text-center text-sm text-gray-700 dark:text-gray-300">
                        <span className="font-semibold text-red-500 dark:text-red-400">🍱 전체 합계:</span>{" "}
                        {(() => {
                            const t = accumulate(sortedMeals);
                            return (
                                <>
                                    총{" "}
                                    <span className="font-semibold text-gray-900 dark:text-white">
                                        {t.totalCalories.toFixed(0)} kcal
                                    </span>{" "}
                                    · 단백질{" "}
                                    <span className="font-semibold text-blue-500 dark:text-blue-400">
                                        {t.totalProtein.toFixed(1)} g
                                    </span>{" "}
                                    · 지방{" "}
                                    <span className="font-semibold text-yellow-500 dark:text-yellow-400">
                                        {t.totalFat.toFixed(1)} g
                                    </span>{" "}
                                    · 탄수화물{" "}
                                    <span className="font-semibold text-green-500 dark:text-green-400">
                                        {t.totalCarbs.toFixed(1)} g
                                    </span>
                                </>
                            );
                        })()}
                    </div>
                </div>

                {/* ✅ 버튼 영역 (폭 정렬 유지) */}
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
        </motion.div>
    );
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
