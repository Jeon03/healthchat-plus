import { useState } from "react";
import ChatMessage from "./ChatMessage";
import ChatInput from "./ChatInput";
import api from "../../api/axios";

type ChatRole = "user" | "ai";

/* ---------- 식단 ---------- */
interface FoodItem {
    name: string;
    quantity: number;
    unit: string;
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
}

interface Meal {
    time: string;
    foods: FoodItem[];
}

interface DailyAnalysis {
    action?: "add" | "update" | "delete";
    targetMeal?: string;
    meals: Meal[];
    totalCalories: number;
    totalProtein: number;
    totalFat: number;
    totalCarbs: number;
}

/* ---------- 운동 ---------- */
interface ExerciseItem {
    category: string;
    part: string;
    name: string;
    durationMin: number;
    intensity: string;
    calories: number;
}

interface ExerciseAnalysisResult {
    action: string;
    exercises: ExerciseItem[];
    totalCalories: number;
    totalDuration: number;
    message?: string;
}

/* ---------- 감정 ---------- */
interface EmotionResult {
    mood: string;
    moodScore: string;
    summary: string;
}

/* ---------- 통합 결과 ---------- */
interface UnifiedAnalysisResult {
    mealAnalysis?: DailyAnalysis;
    exerciseAnalysis?: ExerciseAnalysisResult;
    emotionAnalysis?: EmotionResult;
}

/* ---------- 메시지 ---------- */
interface Message {
    role: ChatRole;
    text: string;
}

export default function ChatContainer() {
    const [messages, setMessages] = useState<Message[]>([
        { role: "ai", text: "안녕하세요 👋 오늘의 식단, 운동, 감정을 함께 기록해볼까요?" },
    ]);
    const [loading, setLoading] = useState(false);

    const safe = (val: any, digits: number = 1) => {
        if (val === null || val === undefined || isNaN(val)) return (0).toFixed(digits);
        return Number(val).toFixed(digits);
    };

    /** ✅ 메시지 전송 + AI 분석 요청 */
    const handleSend = async (userText: string) => {
        if (!userText.trim()) return;

        setMessages((prev) => [...prev, { role: "user", text: userText }]);
        setLoading(true);

        try {
            // ✅ 통합 분석 요청
            const res = await api.post<UnifiedAnalysisResult>("/ai/analyze", { text: userText });
            const data = res.data;

            let replyText = "";

            /* 🍱 식단 분석 */
            if (data.mealAnalysis) {
                const meal = data.mealAnalysis;
                replyText += `🍱 [식단 요약]\n총 섭취 칼로리: ${safe(meal.totalCalories, 0)} kcal\n`;
                replyText += `단백질: ${safe(meal.totalProtein)}g, 지방: ${safe(meal.totalFat)}g, 탄수화물: ${safe(
                    meal.totalCarbs
                )}g\n\n`;

                if (meal.meals?.length) {
                    replyText += meal.meals
                        .map((m) => {
                            const foods = m.foods
                                .map(
                                    (f) =>
                                        `- ${f.name} (${safe(f.quantity, 0)}${f.unit}) → ${safe(
                                            f.calories,
                                            0
                                        )} kcal`
                                )
                                .join("\n");
                            return `${m.time}\n${foods}`;
                        })
                        .join("\n\n");
                    replyText += "\n\n";
                }
            }

            /* 💪 운동 분석 */
            if (data.exerciseAnalysis && data.exerciseAnalysis.exercises?.length > 0) {
                const ex = data.exerciseAnalysis;
                replyText += `💪 [운동 요약]\n`;
                replyText += `총 운동 시간: ${safe(ex.totalDuration, 0)}분\n총 소모 칼로리: ${safe(
                    ex.totalCalories,
                    0
                )} kcal\n\n`;

                replyText += ex.exercises
                    .map(
                        (e) =>
                            `- ${e.name} (${e.category}/${e.part}) ${e.durationMin}분 (${e.intensity}) → ${e.calories} kcal`
                    )
                    .join("\n");
                replyText += "\n\n";
            }

            /* 💬 감정 분석 */
            if (data.emotionAnalysis) {
                const emo = data.emotionAnalysis;
                replyText += `💬 [감정 분석]\n기분: ${emo.mood} (${emo.moodScore})\n${emo.summary}`;
            }

            setMessages((prev) => [
                ...prev,
                { role: "ai", text: replyText || "분석 결과가 없어요." },
            ]);
        } catch (err) {
            console.error(err);
            setMessages((prev) => [
                ...prev,
                { role: "ai", text: "❌ 분석 중 오류가 발생했어요." },
            ]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex flex-col w-full max-w-lg mx-auto bg-white dark:bg-gray-800 rounded-2xl shadow-lg p-4 h-[600px]">
            <div className="flex-1 overflow-y-auto space-y-3 mb-3">
                {messages.map((msg, i) => (
                    <ChatMessage key={i} role={msg.role} text={msg.text} />
                ))}
                {loading && <p className="text-sm text-gray-500">AI가 분석 중입니다...</p>}
            </div>
            <ChatInput onSend={handleSend} disabled={loading} />
        </div>
    );
}
